package cn.ztuo.bitrade.controller;

import com.alibaba.fastjson.JSONObject;
import cn.ztuo.bitrade.constant.MemberLevelEnum;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.service.*;
import cn.ztuo.bitrade.util.DateUtil;
import cn.ztuo.bitrade.util.Md5;
import cn.ztuo.bitrade.util.MessageResult;
import cn.ztuo.bitrade.vo.EmptionRecrodVO;
import cn.ztuo.bitrade.vo.IeoEmptionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;

import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;
import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.isTrue;

/**
 * @Description:
 * @Author: GuoShuai
 * @Date: 2019/4/27 9:29 AM
 */
@RestController
@RequestMapping("ieo")
@Slf4j
public class IeoEmptionController extends BaseController {


    /**
     * 1.接收用户认购订单，并判断是否满足认购条件
     * *持有某种币种多少
     * *认购时间判断
     * 2.满足认购条件发送Kafka消息，统一处理
     * <p>
     * 3.Kafka处理是否认购成功
     */

    @Autowired
    private IeoEmptionService ieoEmptionService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private LocaleMessageSourceService sourceService;

    @Autowired
    private MemberWalletService memberWalletService;

    @Autowired
    private CoinService coinService;

    @Autowired
    private EmptionRecordService emptionRecordService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;


    /**
     * 查询所有ieo
     *
     * @param emptionVO
     * @return
     */
    @RequestMapping(value = "all", method = RequestMethod.POST)
    public MessageResult getAllIeo(IeoEmptionVO emptionVO) {
        log.info("-----查询所有ieo-----" + JSONObject.toJSONString(emptionVO));
        try {
            Page<IeoEmption> result = ieoEmptionService.getByPage(emptionVO);
            return successDataAndTotal(result.getContent(), result.getTotalElements());
        } catch (Exception e) {
            log.info("---------查询所有ieo异常={}", e);
            return error("查询失败，稍后重试");
        }
    }

    /**
     * 查询个人ieo记录
     * @param emptionRecrodVO
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "record", method = RequestMethod.POST)
    public MessageResult getAllRecord(@SessionAttribute(SESSION_MEMBER) AuthMember user,EmptionRecrodVO
            emptionRecrodVO) throws Exception {
        log.info("-----查询个人ieo记录-----" + JSONObject.toJSONString(emptionRecrodVO));
        emptionRecrodVO.setUserId(user.getId());
        Page<EmptionRecord> result = emptionRecordService.getByPage(emptionRecrodVO);
        return successDataAndTotal(result.getContent(), result.getTotalElements());
    }


    /**
     * 用户认购IEO
     *
     * @param user
     * @param id
     * @param amount
     * @param jyPassword
     * @return
     */
    @RequestMapping(value = "order", method = RequestMethod.POST)
    public MessageResult orderIeo(@SessionAttribute(SESSION_MEMBER) AuthMember user,@RequestParam("id") Long id,
                                  @RequestParam("amount") BigDecimal amount, @RequestParam("jyPassword") String
                                          jyPassword) throws Exception {

            log.info("-----用户认购，id={},amount={},userId={}",id,amount,user.getId());
        hasText(jyPassword, sourceService.getMessage("MISSING_JYPASSWORD"));
        Assert.isTrue(amount.compareTo(BigDecimal.ZERO) > 0, "数量异常");
        amount.setScale(4, BigDecimal.ROUND_DOWN);
        Member member = memberService.findOne(user.getId());
        String mbPassword = member.getJyPassword();
        isTrue(member.getMemberLevel() != MemberLevelEnum.GENERAL, "请先进行实名认证!");
        Assert.hasText(mbPassword, sourceService.getMessage("NO_SET_JYPASSWORD"));
        Assert.isTrue(Md5.md5Digest(jyPassword + member.getSalt()).toLowerCase().equals(mbPassword), sourceService
                .getMessage("ERROR_JYPASSWORD"));
        Date date = new Date();
        //查询正在进行的活动
        IeoEmption ieoEmption = ieoEmptionService.findbyCondition(id, DateUtil.dateToString(date));
        if (ieoEmption != null) {
            Assert.isTrue(ieoEmption.getLimitAmount().compareTo(amount) > 0, "超过抢购限额");
            BigDecimal receAmount = amount.multiply(ieoEmption.getRatio()).setScale(4, BigDecimal.ROUND_DOWN);
            Assert.isTrue(ieoEmption.getSurplusAmount().compareTo(receAmount) >= 0, "库存不足");
            //查询用户是否满足条件
            Coin coin = coinService.findByUnit(ieoEmption.getHaveCoin());
            MemberWallet memberWallet = memberWalletService.findByCoinAndMemberId(coin, member.getId());
            if (memberWallet != null) {
                int result = memberWallet.getBalance().compareTo(ieoEmption.getHaveAmount());
                if (result >= 0) {
                    JSONObject json = new JSONObject();
                    json.put("uid", member.getId());
                    json.put("ieoId", ieoEmption.getId());
                    json.put("amount", amount);
                    kafkaTemplate.send("ieo-order", json.toJSONString());
                    return success("认购完成，结果请到个人中心查看");
                } else {
                    return error("用户持有币种不符合规则");
                }
            } else {
                return error("用户持有币种不符合规则");
            }
        } else {
            return error("该活动暂未开始或已结束");
        }
    }

}
