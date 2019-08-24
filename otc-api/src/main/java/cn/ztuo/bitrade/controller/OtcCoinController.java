package cn.ztuo.bitrade.controller;

import cn.ztuo.bitrade.coin.CoinExchangeFactory;
import cn.ztuo.bitrade.service.OtcCoinService;
import cn.ztuo.bitrade.util.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static cn.ztuo.bitrade.util.MessageResult.success;

/**
 * @author GuoShuai
 * @date 2018年01月06日
 */
@RestController
@Slf4j
@RequestMapping(value = "/coin")
public class OtcCoinController {

    @Autowired
    private OtcCoinService coinService;
    @Autowired
    private CoinExchangeFactory coins;

    /**
     * 取得正常的币种
     *
     * @return
     */
    @RequestMapping(value = "all")
    public MessageResult allCoin() throws Exception {
        List<Map<String, String>> list = coinService.getAllNormalCoin();
        list.stream().forEachOrdered(x ->{
            if(coins.getCny(x.get("unit")) != null) {
                x.put("marketPrice", coins.getCny(x.get("unit")).toPlainString());
            }
            if(coins.getJpy(x.get("unit")) != null) {
                x.put("jpyMarketPrice", coins.getJpy(x.get("unit")).toPlainString());
            }
        });
        MessageResult result = success();
        result.setData(list);
        return result;
    }
}
