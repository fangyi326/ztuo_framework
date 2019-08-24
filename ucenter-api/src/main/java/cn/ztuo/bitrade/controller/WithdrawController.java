package cn.ztuo.bitrade.controller;

import cn.ztuo.bitrade.util.*;
import com.alibaba.fastjson.JSONObject;
import cn.ztuo.bitrade.system.CoinExchangeFactory;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import cn.ztuo.bitrade.constant.*;
import cn.ztuo.bitrade.entity.ScanMemberAddress;
import cn.ztuo.bitrade.entity.WithdrawWalletInfo;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.exception.InformationExpiredException;
import cn.ztuo.bitrade.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;
import static cn.ztuo.bitrade.util.BigDecimalUtils.*;
import static cn.ztuo.bitrade.util.MessageResult.error;
import static org.springframework.util.Assert.*;

/**
 * @author GuoShuai
 * @date 2018年01月26日
 */
@RestController
@Slf4j
@RequestMapping(value = "/withdraw", method = RequestMethod.POST)
public class WithdrawController {
    @Autowired
    private MemberAddressService memberAddressService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private MemberService memberService;
    @Autowired
    private CoinService coinService;
    @Autowired
    private MemberWalletService memberWalletService;
    @Autowired
    private WithdrawRecordService withdrawApplyService;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private LocaleMessageSourceService sourceService;
    @Autowired
    private MemberGradeService gradeService;
    @Autowired
    private CoinExchangeFactory coinExchangeFactory ;
    /**
     * 增加提现地址
     * @param address
     * @param unit
     * @param remark
     * @param code
     * @param aims
     * @param user
     * @return
     */
    @RequestMapping("address/add")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult addAddress(String address, String unit, String remark, String code, String aims, @SessionAttribute(SESSION_MEMBER) AuthMember user) {
        hasText(address, sourceService.getMessage("MISSING_COIN_ADDRESS"));
        hasText(unit, sourceService.getMessage("MISSING_COIN_TYPE"));
        hasText(code, sourceService.getMessage("MISSING_VERIFICATION_CODE"));
        hasText(aims, sourceService.getMessage("MISSING_PHONE_OR_EMAIL"));
        Coin coin = coinService.findByUnit(unit);
        List<MemberAddress> memberAddress = memberAddressService.findByMemberIdAndCoinAndAddress(user.getId(),coin,address,CommonStatus.NORMAL);
        if(memberAddress!=null && memberAddress.size()>0) {
            return error("该地址已经存在，请确认地址");
        }
        Member member = memberService.findOne(user.getId());
        if (member.getMobilePhone() != null && aims.equals(member.getMobilePhone())) {
            Object info = redisUtil.get(SysConstant.PHONE_ADD_ADDRESS_PREFIX + member.getMobilePhone());
            if(info==null){
                return error(sourceService.getMessage("VERIFICATION_CODE_NOT_EXISTS"));
            }
            if (!info.toString().equals(code)) {
                return error(sourceService.getMessage("VERIFICATION_CODE_INCORRECT"));
            } else {
                redisUtil.delete(SysConstant.PHONE_ADD_ADDRESS_PREFIX + member.getMobilePhone());
            }
        } else if (member.getEmail() != null && aims.equals(member.getEmail())) {
            Object info = redisUtil.get(SysConstant.ADD_ADDRESS_CODE_PREFIX + member.getEmail());
            if(info==null){
                return error(sourceService.getMessage("VERIFICATION_CODE_NOT_EXISTS"));
            }
            if (!info.toString().equals(code)) {
                return error(sourceService.getMessage("VERIFICATION_CODE_INCORRECT"));
            } else {
                redisUtil.delete(SysConstant.ADD_ADDRESS_CODE_PREFIX + member.getEmail());
            }
        } else {
            return error(sourceService.getMessage("ADD_ADDRESS_FAILED"));
        }
        MessageResult result = memberAddressService.addMemberAddress(user.getId(), address, unit, remark);
        if (result.getCode() == 0) {
            result.setMessage(sourceService.getMessage("ADD_ADDRESS_SUCCESS"));
        } else if (result.getCode() == 500) {
            result.setMessage(sourceService.getMessage("ADD_ADDRESS_FAILED"));
        } else if (result.getCode() == 600) {
            result.setMessage(sourceService.getMessage("COIN_NOT_SUPPORT"));
        }
        return result;
    }

    /**
     * 删除提现地址
     * @param id
     * @param user
     * @return
     */
    @RequestMapping("address/delete")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult deleteAddress(long id, @SessionAttribute(SESSION_MEMBER) AuthMember user) {
        MessageResult result = memberAddressService.deleteMemberAddress(user.getId(), id);
        if (result.getCode() == 0) {
            result.setMessage(sourceService.getMessage("DELETE_ADDRESS_SUCCESS"));
        } else {
            result.setMessage(sourceService.getMessage("DELETE_ADDRESS_FAILED"));
        }
        return result;
    }

    /**
     * 提现地址分页信息
     * @param user
     * @param pageNo
     * @param pageSize
     * @param unit
     * @return
     */
    @RequestMapping("address/page")
    public MessageResult addressPage(@SessionAttribute(SESSION_MEMBER) AuthMember user, int pageNo, int pageSize, String unit) {
        Page<MemberAddress> page = memberAddressService.pageQuery(pageNo, pageSize, user.getId(), unit);
        Page<ScanMemberAddress> scanMemberAddresses = page.map(x -> ScanMemberAddress.toScanMemberAddress(x));
        MessageResult result = MessageResult.success();
        result.setData(scanMemberAddresses);
        return result;
    }

    /**
     * 支持提现的地址
     * @return
     */
    @RequestMapping("support/coin")
    public MessageResult queryWithdraw() {
        List<Coin> list = coinService.findAllCanWithDraw();
        List<String> list1 = new ArrayList<>();
        list.stream().forEach(x -> list1.add(x.getUnit()));
        MessageResult result = MessageResult.success();
        result.setData(list1);
        return result;
    }

    /**
     * 提现币种详细信息
     * @param user
     * @return
     */
    @RequestMapping("support/coin/info")
    public MessageResult queryWithdrawCoin(@SessionAttribute(SESSION_MEMBER) AuthMember user) {
        List<Coin> list = coinService.findAllCanWithDraw();
        List<MemberWallet> list1 = memberWalletService.findAllByMemberId(user.getId());
        long id = user.getId();
        List<WithdrawWalletInfo> list2 = list1.stream().filter(x -> list.contains(x.getCoin())).map(x ->
                WithdrawWalletInfo.builder()
                        .balance(x.getBalance())
                        .withdrawScale(x.getCoin().getWithdrawScale())
                        .maxTxFee(x.getCoin().getMaxTxFee())
                        .minTxFee(x.getCoin().getMinTxFee())
                        .minAmount(x.getCoin().getMinWithdrawAmount())
                        .maxAmount(x.getCoin().getMaxWithdrawAmount())
                        .name(x.getCoin().getName())
                        .nameCn(x.getCoin().getNameCn())
                        .threshold(x.getCoin().getWithdrawThreshold())
                        .unit(x.getCoin().getUnit())
                        .canAutoWithdraw(x.getCoin().getCanAutoWithdraw())
                        .addresses(memberAddressService.queryAddress(id, x.getCoin().getName())).build()
        ).collect(Collectors.toList());
        MessageResult result = MessageResult.success();
        result.setData(list2);
        return result;
    }

    /**
     * 申请提币
     * @param user
     * @param unit
     * @param address
     * @param amount
     * @param fee
     * @param remark
     * @param jyPassword
     * @return
     * @throws Exception
     */
    @RequestMapping("apply")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult withdraw(@SessionAttribute(SESSION_MEMBER) AuthMember user, String unit, String address,
                                  BigDecimal amount, BigDecimal fee,String remark,String jyPassword,
                                  @RequestParam("code") String code,@RequestParam(value = "googleCode",required = false)
                                              String googleCode) throws Exception {
        hasText(jyPassword, sourceService.getMessage("MISSING_JYPASSWORD"));
        hasText(unit, sourceService.getMessage("MISSING_COIN_TYPE"));
        Coin coin = coinService.findByUnit(unit);
        amount.setScale(coin.getWithdrawScale(),BigDecimal.ROUND_DOWN);
        notNull(coin, sourceService.getMessage("COIN_ILLEGAL"));
        isTrue(coin.getStatus().equals(CommonStatus.NORMAL) && coin.getCanWithdraw().equals(BooleanEnum.IS_TRUE), sourceService.getMessage("COIN_NOT_SUPPORT"));
        isTrue(compare(fee, new BigDecimal(String.valueOf(coin.getMinTxFee()))), sourceService.getMessage("CHARGE_MIN") + coin.getMinTxFee());
        isTrue(compare(new BigDecimal(String.valueOf(coin.getMaxTxFee())), fee), sourceService.getMessage("CHARGE_MAX") + coin.getMaxTxFee());
        isTrue(compare(coin.getMaxWithdrawAmount(), amount), sourceService.getMessage("WITHDRAW_MAX") + coin.getMaxWithdrawAmount());
        isTrue(compare(amount, coin.getMinWithdrawAmount()), sourceService.getMessage("WITHDRAW_MIN") + coin.getMinWithdrawAmount());
        MemberWallet memberWallet = memberWalletService.findByCoinAndMemberId(coin, user.getId());
        isTrue(compare(memberWallet.getBalance(), amount), sourceService.getMessage("INSUFFICIENT_BALANCE"));
//        isTrue(memberAddressService.findByMemberIdAndAddress(user.getId(), address).size() > 0, sourceService.getMessage("WRONG_ADDRESS"));
        isTrue(memberWallet.getIsLock()==BooleanEnum.IS_FALSE,"钱包已锁定");
        Member member = memberService.findOne(user.getId());
        //是否完成kyc二级认证
        isTrue(member.getKycStatus()==4,"请先完成视频认证");
        String mbPassword = member.getJyPassword();
        isTrue(member.getMemberLevel()!=MemberLevelEnum.GENERAL,"请先进行实名认证!");
        Assert.hasText(mbPassword, sourceService.getMessage("NO_SET_JYPASSWORD"));
        if (member.getGoogleState() == 1) {
            //谷歌验证
            if (org.apache.commons.lang.StringUtils.isNotEmpty(googleCode)) {
                long googleCodes = Long.parseLong(googleCode);
                long t = System.currentTimeMillis();
                GoogleAuthenticatorUtil ga = new GoogleAuthenticatorUtil();
                boolean r = ga.check_code(member.getGoogleKey(), googleCodes, t);
                if (!r) {
                    return MessageResult.error("谷歌验证失败");
                }

            }else {
                return MessageResult.error("请输入谷歌验证码");
            }
        }
        if (!code.equals(redisUtil.get(SysConstant.PHONE_WITHDRAW_MONEY_CODE_PREFIX + member.getMobilePhone()))) {
            return error(sourceService.getMessage("VERIFICATION_CODE_INCORRECT"));
        } else {
            redisUtil.delete(SysConstant.PHONE_WITHDRAW_MONEY_CODE_PREFIX + member.getMobilePhone());
        }
        Assert.isTrue(Md5.md5Digest(jyPassword + member.getSalt()).toLowerCase().equals(mbPassword), sourceService.getMessage("ERROR_JYPASSWORD"));
        MessageResult result = memberWalletService.freezeBalance(memberWallet, amount);
        if (result.getCode() != 0) {
            throw new InformationExpiredException("Information Expired");
        }
        //判断该用户当日提币笔数与当日提币数量
        MemberGrade grade = gradeService.findOne(member.getMemberGradeId());
        Object count= redisUtil.get(SysConstant.CUSTOMER_DAY_WITHDRAW_TOTAL_COUNT+user.getId());
        Long countLong = count==null ?0:(Long)count;
        if(countLong>grade.getDayWithdrawCount() ){
            return error("超过当前等级最大提币次数");
        }
        Object coverUsdAmount = redisUtil.get(SysConstant.CUSTOMER_DAY_WITHDRAW_COVER_USD_AMOUNT);
        BigDecimal coverUsdAmountBigDecimal = coverUsdAmount==null ? BigDecimal.ZERO:(BigDecimal)coverUsdAmount;
        if( coverUsdAmountBigDecimal.compareTo(grade.getWithdrawCoinAmount())==1){
            return error("超过当前等级最大提币数量");
        }
        Long expireTime = DateUtil.calculateCurrentTime2SecondDaySec();
        //设置提币笔数 与折合数量
        CoinExchangeFactory.ExchangeRate rate = coinExchangeFactory.get(unit);
        coverUsdAmountBigDecimal = coverUsdAmountBigDecimal.add(rate.getUsdRate().multiply(amount));
        log.info("该用户提币次数={},提币折合USD数量={}",coverUsdAmountBigDecimal);
        //判断用户等级最大提币数
        isTrue(compare(grade.getWithdrawCoinAmount(),coverUsdAmountBigDecimal),"超过等级最大提币数");
        countLong++;
        redisUtil.set(SysConstant.CUSTOMER_DAY_WITHDRAW_TOTAL_COUNT+user.getId(),countLong,expireTime);
        redisUtil.set(SysConstant.CUSTOMER_DAY_WITHDRAW_COVER_USD_AMOUNT+user.getId(),coverUsdAmountBigDecimal,expireTime);



        WithdrawRecord withdrawApply = new WithdrawRecord();
        withdrawApply.setCoin(coin);
        withdrawApply.setFee(fee);
        withdrawApply.setArrivedAmount(sub(amount, fee));
        withdrawApply.setMemberId(user.getId());
        withdrawApply.setTotalAmount(amount);
        withdrawApply.setAddress(address);
        withdrawApply.setRemark(remark);
        withdrawApply.setCanAutoWithdraw(coin.getCanAutoWithdraw());

        //提币数量低于或等于阈值并且该币种支持自动提币
        if (amount.compareTo(coin.getWithdrawThreshold()) <= 0 && coin.getCanAutoWithdraw().equals(BooleanEnum.IS_TRUE)) {
            Double withAmountSum=sumDailyWithdraw(coin);
            //如果币种设置了单日最大提币量，并且当天已申请的数量（包括待审核、待放币、成功、转账中状态的所有记录）加上当前提币量大于每日最大提币量
            // 进入人工审核
            if(coin.getMaxDailyWithdrawRate()!=null&&coin.getMaxDailyWithdrawRate().compareTo(BigDecimal.ZERO)>0
                    &&coin.getMaxDailyWithdrawRate().compareTo(new BigDecimal(withAmountSum).add(amount))<0){
                withdrawApply.setStatus(WithdrawStatus.PROCESSING);
                withdrawApply.setIsAuto(BooleanEnum.IS_FALSE);
                if (withdrawApplyService.save(withdrawApply) != null) {
                    return MessageResult.success(sourceService.getMessage("APPLY_AUDIT"));
                } else {
                    throw new InformationExpiredException("Information Expired");
                }
            }else{
                withdrawApply.setStatus(WithdrawStatus.WAITING);
                withdrawApply.setIsAuto(BooleanEnum.IS_TRUE);
                withdrawApply.setDealTime(withdrawApply.getCreateTime());
                WithdrawRecord withdrawRecord = withdrawApplyService.save(withdrawApply);
                JSONObject json = new JSONObject();
                json.put("uid", user.getId());
                //提币总数量
                json.put("totalAmount", amount);
                //手续费
                json.put("fee", fee);
                //预计到账数量
                json.put("arriveAmount", sub(amount, fee));
                //币种
                json.put("coin", coin);
                //提币地址
                json.put("address", address);
                //提币记录id
                json.put("withdrawId", withdrawRecord.getId());
                json.put("remark",remark);
                kafkaTemplate.send("withdraw", coin.getUnit(), json.toJSONString());
                return MessageResult.success(sourceService.getMessage("APPLY_SUCCESS"));
            }
        } else {
            withdrawApply.setStatus(WithdrawStatus.PROCESSING);
            withdrawApply.setIsAuto(BooleanEnum.IS_FALSE);
            if (withdrawApplyService.save(withdrawApply) != null) {
                return MessageResult.success(sourceService.getMessage("APPLY_AUDIT"));
            } else {
                throw new InformationExpiredException("Information Expired");
            }
        }
    }



    /**
     * 提币记录
     * @param user
     * @return
     */
    @GetMapping("record")
    public MessageResult pageWithdraw(@SessionAttribute(SESSION_MEMBER) AuthMember user, PageModel pageModel,
                                      String unit) {
        MessageResult mr = new MessageResult(0, "success");
        ArrayList<BooleanExpression> booleanExpressions = new ArrayList<>();
        if (!StringUtils.isEmpty(unit)) {
            booleanExpressions.add(QWithdrawRecord.withdrawRecord.coin.unit.eq(unit));
        }
        booleanExpressions.add(QWithdrawRecord.withdrawRecord.memberId.eq(user.getId()));
        Predicate predicate=PredicateUtils.getPredicate(booleanExpressions);
        Page<WithdrawRecord> records = withdrawApplyService.findAll(predicate,pageModel);
        records.map(x -> ScanWithdrawRecord.toScanWithdrawRecord(x));
        mr.setData(records);
        return mr;
    }

    /**
     * 当日已申请数量
     * @return
     */
    @GetMapping("todayWithdrawSum")
    public MessageResult todayWithdrawSum(@SessionAttribute(SESSION_MEMBER) AuthMember user,String symbol){
        if(StringUtils.isEmpty(symbol)){
            return error("symbol is not null");
        }
        Coin coin=coinService.findByUnit(symbol);
        if(coin==null){
            return error("coin has not found");
        }
        Double withAmountSum=sumDailyWithdraw(coin);
        MessageResult result=MessageResult.success();
        result.setData(withAmountSum);
        return result;
    }

    private Double sumDailyWithdraw(Coin coin){
        Date endTime=new Date();
        Calendar calendar=Calendar.getInstance();
        calendar.setTime(endTime);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DATE,-1);
        Date startTime=calendar.getTime();
        Double withAmountSum=withdrawApplyService.countWithdrawAmountByTimeAndMemberIdAndCoin(startTime,endTime,coin);
        if(withAmountSum==null){
            withAmountSum=0.0;
        }
        return withAmountSum;
    }



}
