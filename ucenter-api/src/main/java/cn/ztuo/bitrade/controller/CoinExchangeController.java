package cn.ztuo.bitrade.controller;


import cn.ztuo.bitrade.entity.AssetExchangeCoin;
import cn.ztuo.bitrade.entity.transform.AuthMember;
import cn.ztuo.bitrade.service.AssetExchangeService;
import cn.ztuo.bitrade.util.MessageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.math.BigDecimal;
import java.util.List;

import static cn.ztuo.bitrade.constant.SysConstant.SESSION_MEMBER;

/**
 * 币种兑换服务，按预先设计好的汇率相互对换
 */
@RestController
@RequestMapping("/exchange")
public class CoinExchangeController {
    @Autowired
    private AssetExchangeService assetExchangeService;

    /**
     * 获取支持兑换的币种
     * @param unit
     * @return
     */
    @RequestMapping("supported-coin")
    public List<AssetExchangeCoin> findSupportedCoin(String unit){
        return assetExchangeService.findAllByFromCoin(unit);
    }

    /**
     * 币种兑换
     * @param member
     * @param from
     * @param to
     * @param amount
     */
    @RequestMapping("transfer")
    public MessageResult exchange(@SessionAttribute(SESSION_MEMBER) AuthMember member, String from, String to, BigDecimal amount){
        AssetExchangeCoin coin =  assetExchangeService.findOne(from,to);
        if(coin == null){
            return new MessageResult(500,"不支持该币种兑换");
        }
        if(amount.compareTo(BigDecimal.ZERO) <= 0){
            return new MessageResult(500,"数量需大于0");
        }
        return assetExchangeService.exchange(member.getId(),coin,amount);
    }
}
