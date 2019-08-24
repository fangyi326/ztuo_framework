package cn.ztuo.bitrade.component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cn.ztuo.bitrade.entity.CoinThumb;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import cn.ztuo.bitrade.entity.Coin;
import cn.ztuo.bitrade.processor.CoinProcessor;
import cn.ztuo.bitrade.processor.CoinProcessorFactory;
import cn.ztuo.bitrade.service.CoinService;
import cn.ztuo.bitrade.service.ExchangeCoinService;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 币种汇率管理
 */
@Component
@Slf4j
@ToString
public class CoinExchangeRate {
    private Map<String,BigDecimal> legalRateMap = new HashMap<>();
    @Value("${forex.api-key:y4lmqQRykolHFp3VkzjYp2XZfgCdo8Tv}")
    private String forexApiKey;
    @Value("${forex.pairs:USDCNH,CNHUSD}")
    private String forexPairs;
    @Setter
    private CoinProcessorFactory coinProcessorFactory;

    @Autowired
    private CoinService coinService;
    @Autowired
    private ExchangeCoinService exCoinService;
    public Map<String,String> legalAnchoredCoins = new HashMap<>();



    public BigDecimal getCoinLegalRate(String legalCoin, String coin){
        //检查coin是否为legalCoin
        if(legalCoin.equalsIgnoreCase(coin)){
            return BigDecimal.ONE;
        }
        //检查coin是否为锚定币
        if(legalAnchoredCoins.containsKey(coin)){
            String anchoredCoin = legalAnchoredCoins.get(coin);
            //如果与legalCoin为锚定币要则返回1
            if(anchoredCoin.equalsIgnoreCase(legalCoin)) {
                return BigDecimal.ONE;
            }
            else {
                //与其他法币为锚定币的要进行汇率折算
                return getLegalRate(anchoredCoin,legalCoin);
            }
        }
        //取交易价格
        CoinProcessor processor = coinProcessorFactory.getProcessorByCoin(coin);
        if(processor != null){
            BigDecimal baseCoinRate = getCoinLegalRate(legalCoin,processor.getBaseCoin());
            BigDecimal close ;
            if(processor.getThumb()!=null){
                close =  processor.getThumb().getClose();
            }else {
                close = BigDecimal.ZERO ;
            }
            return close.multiply(baseCoinRate).setScale(8,BigDecimal.ROUND_DOWN);
        }

        return getDefaultRate(legalCoin,coin);
    }

    /**
     * 获取法币之间的汇率，
     * @param coin
     * @param base
     * @return
     */
    public BigDecimal getLegalRate(String coin,String base){
        if(coin.equalsIgnoreCase("CNY")){
            coin = "CNH";
        }
        if(base.equalsIgnoreCase("CNY")){
            base = "CNH";
        }
        String pair = coin+base;
        if(legalRateMap.containsKey(pair))return legalRateMap.get(pair);
        else return BigDecimal.ZERO;
    }

    /**
     * 获取币种设置里的默认价格
     *
     * @param symbol
     * @return
     */
    public BigDecimal getDefaultRate(String legal, String symbol) {
        Coin coin = coinService.findByUnit(symbol);
        if (coin != null) {
            if(legal.equalsIgnoreCase("USD")) {
                return coin.getUsdRate();
            }
            else if(legal.equalsIgnoreCase("CNY")){
                return coin.getCnyRate();
            }
            else{
                return getLegalRate("USD",legal).multiply(coin.getUsdRate()).setScale(8,RoundingMode.DOWN);
            }
        } else return BigDecimal.ZERO;
    }

    public void syncLegalRate() throws UnirestException {
//        String url = "https://forex.1forge.com/1.0.3/quotes";
//        HttpResponse<JsonNode> resp = Unirest.get(url)
//                .queryString("pairs", forexPairs)
//                .queryString("api_key", forexApiKey)
//                .asJson();
//        log.info("forex result:{}", resp.getBody());
//        JSONArray result = JSON.parseArray(resp.getBody().toString());
//        result.forEach(json -> {
//            JSONObject obj = (JSONObject) json;
//            legalRateMap.put(obj.getString("symbol"),obj.getBigDecimal("price"));
//        });
        legalRateMap.put("USDCNH",BigDecimal.valueOf(6.7));
        legalRateMap.put("CNHUSD",BigDecimal.valueOf(0.14925));
        log.info("rate map:{}",legalRateMap);
    }
}
