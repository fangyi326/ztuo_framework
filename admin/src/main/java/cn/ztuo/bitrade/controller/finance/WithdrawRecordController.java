package cn.ztuo.bitrade.controller.finance;

import cn.ztuo.bitrade.controller.common.BaseAdminController;
import cn.ztuo.bitrade.core.Encrypt;
import cn.ztuo.bitrade.model.screen.WithdrawRecordScreen;
import com.querydsl.core.types.Predicate;
import cn.ztuo.bitrade.annotation.AccessLog;
import cn.ztuo.bitrade.constant.*;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.service.LocaleMessageSourceService;
import cn.ztuo.bitrade.service.MemberTransactionService;
import cn.ztuo.bitrade.service.MemberWalletService;
import cn.ztuo.bitrade.service.WithdrawRecordService;
import cn.ztuo.bitrade.util.MessageResult;
import cn.ztuo.bitrade.vo.WithdrawRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.ztuo.bitrade.constant.BooleanEnum.IS_FALSE;
import static cn.ztuo.bitrade.constant.WithdrawStatus.*;
import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.notNull;

/**
 * @author MrGao
 * @description 提现
 * @date 2018/2/25 11:22
 */
@Slf4j
@RestController
@RequestMapping("/finance/withdraw-record")
public class WithdrawRecordController extends BaseAdminController {
    @Autowired
    private WithdrawRecordService withdrawRecordService;

    @Autowired
    private MemberWalletService memberWalletService;

    @Autowired
    private MemberTransactionService memberTransactionService;

    @Autowired
    private LocaleMessageSourceService messageSource;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${coin.not.sync}")
    private String notSyncCoin;


    @Value("${bdtop.system.md5.key}")
    private String md5Key;

    @RequiresPermissions("finance:withdraw-record:all")
    @GetMapping("/all")
    @AccessLog(module = AdminModule.FINANCE, operation = "所有提现记录WithdrawRecord")
    public MessageResult all() {
        List<WithdrawRecord> withdrawRecordList = withdrawRecordService.findAll();
        if (withdrawRecordList == null || withdrawRecordList.size() < 1) {
            return error(messageSource.getMessage("NO_DATA"));
        }
        return success(withdrawRecordList);
    }

    @RequiresPermissions(value = {"finance:withdraw-record:page-query", "finance:withdraw-record:page-query:success"}, logical = Logical.OR)
    @RequestMapping("/page-query")
    @AccessLog(module = AdminModule.FINANCE, operation = "分页查询提现记录WithdrawRecord")
    public MessageResult pageQuery(
            PageModel pageModel,
            WithdrawRecordScreen screen) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(QWithdrawRecord.withdrawRecord.memberId.eq(QMember.member.id));

        if (screen.getMemberId() != null) {
            predicates.add(QWithdrawRecord.withdrawRecord.memberId.eq(screen.getMemberId()));
        }
        if (screen.getStatus() != null) {
            predicates.add(QWithdrawRecord.withdrawRecord.status.eq(screen.getStatus()));
        }
        if (screen.getIsAuto() != null) {
            predicates.add(QWithdrawRecord.withdrawRecord.isAuto.eq(screen.getIsAuto()));
        }
        if (!StringUtils.isEmpty(screen.getAddress())) {
            predicates.add(QWithdrawRecord.withdrawRecord.address.eq(screen.getAddress()));
        }
        if (!StringUtils.isEmpty(screen.getUnit())) {
            predicates.add(QWithdrawRecord.withdrawRecord.coin.unit.equalsIgnoreCase(screen.getUnit()));
        }
        if(!StringUtils.isEmpty(screen.getOrderSn())){
            predicates.add(QWithdrawRecord.withdrawRecord.transactionNumber.like(screen.getOrderSn()));
        }
        if (!StringUtils.isEmpty(screen.getAccount())) {
            predicates.add(QMember.member.username.like("%" + screen.getAccount() + "%")
                    .or(QMember.member.realName.like("%" + screen.getAccount() + "%")));
        }
        if(screen.getWithdrawRecordId()!=null){
            predicates.add(QWithdrawRecord.withdrawRecord.id.eq(screen.getWithdrawRecordId()));
        }
        Page<WithdrawRecordVO> pageListMapResult = withdrawRecordService.joinFind(predicates, pageModel);
        return success(pageListMapResult);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("finance:withdraw-record:detail")
    @AccessLog(module = AdminModule.FINANCE, operation = "提现记录WithdrawRecord 详情")
    public MessageResult detail(@PathVariable("id") Long id) {
        WithdrawRecord withdrawRecord = withdrawRecordService.findOne(id);
        notNull(withdrawRecord, messageSource.getMessage("NO_DATA"));
        return success(withdrawRecord);
    }

    //一键审核通过
    @RequiresPermissions("finance:withdraw-record:audit-pass")
    @PatchMapping("/audit-pass")
    @AccessLog(module = AdminModule.FINANCE, operation = "提现记录WithdrawRecord一键审核通过")
    public MessageResult auditPass(@RequestParam("ids") Long[] ids) {
        withdrawRecordService.audit(ids, WAITING);
        return success(messageSource.getMessage("PASS_THE_AUDIT"));
    }

    //一键审核不通过
    @RequiresPermissions("finance:withdraw-record:audit-no-pass")
    @PatchMapping("/audit-no-pass")
    @AccessLog(module = AdminModule.FINANCE, operation = "提现记录WithdrawRecord一键审核不通过")
    public MessageResult auditNoPass(@RequestParam("ids") Long[] ids) {
        withdrawRecordService.audit(ids, FAIL);
        return success(messageSource.getMessage("AUDIT_DOES_NOT_PASS"));
    }

    /**
     * 单个打款 转账成功添加流水号
     *
     * @param id
     * @param transactionNumber
     * @return
     */
    @RequiresPermissions("finance:withdraw-record:add-transaction-number")
    @PatchMapping("/add-transaction-number")
    @AccessLog(module = AdminModule.FINANCE, operation = "添加交易流水号")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult addNumber(
            @RequestParam("id") Long id,
            @RequestParam("transactionNumber") String transactionNumber) {
        WithdrawRecord record = withdrawRecordService.findOne(id);
        Assert.notNull(record, "该记录不存在");
        Assert.isTrue(record.getIsAuto() == BooleanEnum.IS_FALSE, "该提现单为自动审核");
        Assert.isTrue(record.getStatus().equals(WithdrawStatus.WAITING),"提现状态不是等待放币,不能打款!");
        record.setTransactionNumber(transactionNumber);
        record.setStatus(WithdrawStatus.SUCCESS);
        MemberWallet memberWallet = memberWalletService.findByCoinAndMemberId(record.getCoin(), record.getMemberId());
        Assert.notNull(memberWallet, "member id " + record.getMemberId() + " 的 wallet 为 null");
        memberWallet.setFrozenBalance(memberWallet.getFrozenBalance().subtract(record.getTotalAmount()));
        memberWalletService.save(memberWallet);
        record = withdrawRecordService.save(record);

        MemberTransaction memberTransaction = new MemberTransaction();
        memberTransaction.setMemberId(record.getMemberId());
        memberTransaction.setAddress(record.getAddress());
        memberTransaction.setAmount(record.getTotalAmount().negate());
        memberTransaction.setSymbol(record.getCoin().getUnit());
        memberTransaction.setCreateTime(record.getCreateTime());
        memberTransaction.setType(TransactionType.WITHDRAW);
        memberTransaction.setFee(record.getFee().negate());
        memberTransactionService.save(memberTransaction);

        return MessageResult.success(messageSource.getMessage("SUCCESS"), record);
    }

    //批量打款
    @RequiresPermissions("finance:withdraw-record:remittance")
    @PatchMapping("/remittance")
    @AccessLog(module = AdminModule.FINANCE, operation = "提现记录/批量打款")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult remittance(
            @SessionAttribute(SysConstant.SESSION_ADMIN) Admin admin,
            @RequestParam("ids") Long[] ids,
            @RequestParam("transactionNumber") String transactionNumber,
            @RequestParam("password") String password) {
        Assert.notNull(admin, messageSource.getMessage("DATA_EXPIRED_LOGIN_AGAIN"));
        password = Encrypt.MD5(password + md5Key);
        if (!password.equals(admin.getPassword())) {
            return error(messageSource.getMessage("WRONG_PASSWORD"));
        }
        WithdrawRecord withdrawRecord;
        for (Long id : ids) {
            withdrawRecord = withdrawRecordService.findOne(id);
            notNull(withdrawRecord, "id :" + id + messageSource.getMessage("NO_DATA"));
            isTrue(withdrawRecord.getStatus() == WAITING, "提现状态不是等待放币,不能打款!");
            isTrue(withdrawRecord.getIsAuto() == IS_FALSE, "不是人工审核提现!");
            //标记提现完成
            withdrawRecord.setStatus(SUCCESS);
            //交易编码
            withdrawRecord.setTransactionNumber(transactionNumber);
            MemberWallet memberWallet = memberWalletService.findByCoinAndMemberId(withdrawRecord.getCoin(), withdrawRecord.getMemberId());
            Assert.notNull(memberWallet, "member id " + withdrawRecord.getMemberId() + " 的 wallet 为 null");
            memberWallet.setFrozenBalance(memberWallet.getFrozenBalance().subtract(withdrawRecord.getTotalAmount()));
            memberWalletService.save(memberWallet);
            withdrawRecordService.save(withdrawRecord);

            MemberTransaction memberTransaction = new MemberTransaction();
            memberTransaction.setMemberId(withdrawRecord.getMemberId());
            memberTransaction.setAddress(withdrawRecord.getAddress());
            memberTransaction.setAmount(withdrawRecord.getTotalAmount().negate());
            memberTransaction.setSymbol(withdrawRecord.getCoin().getUnit());
            memberTransaction.setCreateTime(withdrawRecord.getCreateTime());
            memberTransaction.setType(TransactionType.WITHDRAW);
            memberTransaction.setFee(withdrawRecord.getFee().negate());
            memberTransactionService.save(memberTransaction);

        }
        return success();
    }

    @RequiresPermissions("finance:withdraw-record:audit-pass")
    @PostMapping("autuWithdraw")
    @AccessLog(module = AdminModule.FINANCE, operation = "审核通过自动打款")
    public MessageResult authWithdraw(@RequestParam("ids") Long[] ids){
        List<WithdrawRecord> withdrawRecordList=withdrawRecordService.findByIds(ids);
        if(withdrawRecordList==null||withdrawRecordList.size()==0){
            return MessageResult.error(messageSource.getMessage("ERROR_WITHDRAW_IDS"));
        }
        List<Coin> coinList=new ArrayList<>();
        madeCoinList(withdrawRecordList,coinList);
        checkBalance(coinList,withdrawRecordList);
        for(WithdrawRecord withdrawRecord:withdrawRecordList){
            isTrue(withdrawRecord.getStatus() == WAITING, "提现状态不是等待放币,不能打款!");
            isTrue(withdrawRecord.getIsAuto() == IS_FALSE, "不是人工审核提现!");
            try {
                Coin coin=withdrawRecord.getCoin();
                boolean flag=notSyncCoin.indexOf(coin.getUnit().toUpperCase())>=0;
                String serviceName = "SERVICE-RPC-" + coin.getUnit().toUpperCase();
                String url = "http://" + serviceName + "/rpc/withdraw?address={1}&amount={2}&fee={3}&withdrawId="+withdrawRecord.getId();
                //异步打钱的币种
                if(flag){
                    url+="&sync=false";
                }
                log.info("coin = {}",coin.toString());
                if (coin != null && coin.getCanAutoWithdraw() == BooleanEnum.IS_TRUE) {
                    BigDecimal minerFee = coin.getMinerFee();
                    MessageResult result = restTemplate.getForObject(url,
                            MessageResult.class, withdrawRecord.getAddress(), withdrawRecord.getArrivedAmount(), minerFee);
                    log.info("result = {}", result);
                    if (result.getCode() == 200) {
                        //异步打钱，更新状态为等待汇款
                        withdrawRecord.setStatus(WithdrawStatus.TRANSFER);
                        withdrawRecordService.save(withdrawRecord);
                    }else if(result.getCode()==0&&result.getData() != null){
                        //非异步打钱，处理成功,data为txid，更新业务订单
                        String txid = (String) result.getData();
                        withdrawRecord.setStatus(WithdrawStatus.SUCCESS);
                        withdrawRecordService.withdrawSuccess(withdrawRecord.getId(), txid);
                    }else{
                        return MessageResult.error(result.getMessage());
                    }
                }else{
                    return MessageResult.error("不允许自动提币或币种不存在");
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("auto withdraw failed,error={}", e.getMessage());
            }
        }
        return MessageResult.success();
    }

    private void madeCoinList(List<WithdrawRecord> withdrawRecordList,List<Coin> coinList){
        for(WithdrawRecord withdrawRecord:withdrawRecordList){
            if(!coinList.contains(withdrawRecord.getCoin())){
                coinList.add(withdrawRecord.getCoin());
            }
        }
    }

    private MessageResult checkBalance(List<Coin> coinList,List<WithdrawRecord> withdrawRecordList){
        Map<Coin,BigDecimal> map=new HashMap<>();
        for(Coin coin:coinList){
            BigDecimal amount=BigDecimal.ZERO;
            for(WithdrawRecord withdrawRecord:withdrawRecordList){
                if(coin.equals(withdrawRecord.getCoin())){
                    amount=amount.add(withdrawRecord.getTotalAmount());
                }
            }
            map.put(coin,amount);
        }
        for(Coin coin:map.keySet()){
            BigDecimal amount=map.get(coin);
            String url = "http://SERVICE-RPC-" + coin.getUnit() + "/rpc/balance";
            ResponseEntity<MessageResult> result = restTemplate.getForEntity(url, MessageResult.class);
            log.info("result={}", result);
            if (result.getStatusCode().value() == 200) {
                MessageResult mr = result.getBody();
                if (mr.getCode() == 0) {
                    String balance = mr.getData().toString();
                    BigDecimal bigDecimal = new BigDecimal(balance);
                    if(bigDecimal.compareTo(amount)<0){
                        return MessageResult.error(coin.getUnit()+" "+messageSource.getMessage("HOT_BALANCE_FAILD"));
                    }
                }else{
                    return MessageResult.error(coin.getUnit()+" "+messageSource.getMessage("HOT_BALANCE_FAILD"));
                }
            }
        }
        return MessageResult.success();
    }

}
