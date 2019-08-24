package cn.ztuo.bitrade.controller;

import com.alibaba.fastjson.JSONObject;
import cn.ztuo.bitrade.constant.BooleanEnum;
import cn.ztuo.bitrade.constant.MemberLevelEnum;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.service.*;
import cn.ztuo.bitrade.util.MessageResult;
import cn.ztuo.bitrade.vo.OtcWalletVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.math.BigDecimal;
import java.util.List;

import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;
import static cn.ztuo.bitrade.util.BigDecimalUtils.*;
import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.isTrue;

/**
 * @Description:
 * @Author: GuoShuai
 * @Date: 2019/5/5 3:18 PM
 */
@RestController
@RequestMapping("otc/wallet")
@Slf4j
public class OtcWalletController extends BaseController {

    /**
     * 1.币币账户到法币账户互转
     * 2.查询法币账户
     */


    @Autowired
    private OtcWalletService otcWalletService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberWalletService memberWalletService;

    @Autowired
    private LocaleMessageSourceService sourceService;

    @Autowired
    private CoinService coinService;

    @Autowired
    private OtcCoinService otcCoinService;


    /**
     * 查询用户法币账户
     *
     * @param user
     * @return
     */
    @RequestMapping(value = "get", method = RequestMethod.POST)
    public MessageResult getUserOtcWallet(@SessionAttribute(SESSION_MEMBER) AuthMember user) {
        log.info("---------查询用户法币账户:" + user.getId());
        List<OtcWallet> result = otcWalletService.findByMemberId(user.getId());
        return success(result);
    }



    /**
     * 币币账户到法币账户互转
     *
     * @param user
     * @param otcWalletVO
     * @return
     */
    @RequestMapping(value = "transfer", method = RequestMethod.POST)
    public MessageResult transferOtcWallet(@SessionAttribute(SESSION_MEMBER) AuthMember user, OtcWalletVO
            otcWalletVO) throws Exception {
        log.info("---------币币账户到法币账户互转:userId=" + user.getId() + "," + JSONObject.toJSONString(otcWalletVO));
//        String jyPassword = otcWalletVO.getJyPassword();
//        hasText(jyPassword, sourceService.getMessage("MISSING_JYPASSWORD"));
        OtcCoin coin = otcCoinService.findByUnit(otcWalletVO.getCoinName());
        if (coin == null){
            return error("不支持的法币币种");
        }
        BigDecimal amount = otcWalletVO.getAmount().setScale(coin.getCoinScale(), BigDecimal.ROUND_DOWN);
        Member member = memberService.findOne(user.getId());
        isTrue(member.getMemberLevel() != MemberLevelEnum.GENERAL, "请先进行实名认证!");
//        Assert.isTrue(Md5.md5Digest(jyPassword + member.getSalt()).toLowerCase().equals(member.getJyPassword()),
//                sourceService.getMessage("ERROR_JYPASSWORD"));
        isTrue(compare(amount, BigDecimal.ZERO), sourceService.getMessage("参数异常"));


        Coin memberCoin = coinService.findByUnit(coin.getUnit());

        //查询用户币币账户
        MemberWallet memberWallet = memberWalletService.findByCoinAndMemberId(memberCoin, user.getId());
        isTrue(memberWallet.getIsLock() == BooleanEnum.IS_FALSE, "钱包已锁定");


        //查询用户法币账户
        OtcWallet otcWallet = otcWalletService.findByOtcCoinAndMemberId(member.getId(), coin);
        if (otcWallet == null) {
            //如果法币账户不存在新建
            OtcWallet otcWalletNew = new OtcWallet();
            otcWalletNew.setCoin(memberCoin);
            otcWalletNew.setIsLock(0);
            otcWalletNew.setMemberId(member.getId());
            otcWalletNew.setBalance(BigDecimal.ZERO);
            otcWalletNew.setFrozenBalance(BigDecimal.ZERO);
            otcWalletNew.setReleaseBalance(BigDecimal.ZERO);
            otcWalletNew.setVersion(0);
            otcWallet = otcWalletService.save(otcWalletNew);
            if (otcWallet == null) {
                return error("法币账户创建失败，请联系客服");
            }
        }
        isTrue(otcWallet.getIsLock() == 0, "钱包已锁定");
        if ("0".equals(otcWalletVO.getDirection())) {
            //币币转法币
            isTrue(compare(memberWallet.getBalance(), amount), sourceService.getMessage("INSUFFICIENT_BALANCE"));
            int subResult = otcWalletService.coin2Otc(memberWallet,otcWallet,amount);
            if (subResult == 1){
                return success("划转成功");
            }
            return error("划转失败");
        } else if ("1".equals(otcWalletVO.getDirection())) {
            //法币转币币
            isTrue(compare(otcWallet.getBalance(), amount), sourceService.getMessage("INSUFFICIENT_BALANCE"));
            int addResult = otcWalletService.otc2Coin(memberWallet,otcWallet,amount);
            if (addResult == 1){
                return success("划转成功");
            }
            return error("划转失败");

        } else {
            return error("参数异常");
        }

    }


}
