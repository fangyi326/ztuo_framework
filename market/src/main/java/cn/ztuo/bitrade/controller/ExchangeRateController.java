package cn.ztuo.bitrade.controller;

import cn.ztuo.bitrade.component.CoinExchangeRate;
import cn.ztuo.bitrade.util.MessageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/exchange-rate")
public class ExchangeRateController {
    @Autowired
    private CoinExchangeRate coinExchangeRate;


    /**
     * 获取 交易币对法币的价格
     * @param legalCoin 法币
     * @param coin 交易币
     * @return
     */
    @RequestMapping("{legalCoin}/{coin}")
    public MessageResult getUsdExchangeRate(@PathVariable String legalCoin,@PathVariable String coin){
        MessageResult mr = new MessageResult(0,"success");
        BigDecimal latestPrice = coinExchangeRate.getCoinLegalRate(legalCoin.toUpperCase(),coin.toUpperCase());
        mr.setData(latestPrice.toString());
        return mr;
    }

    /**
     * 获取法币之间的汇率
     * @param fromUnit
     * @param toUnit
     * @return
     */
    @RequestMapping("{fromUnit}-{toUnit}")
    public MessageResult getUsdCnyRate(@PathVariable String fromUnit,@PathVariable String toUnit){
        MessageResult mr = new MessageResult(0,"success");
        mr.setData(coinExchangeRate.getLegalRate(fromUnit.toUpperCase(),toUnit.toUpperCase()));
        return mr;
    }
}
