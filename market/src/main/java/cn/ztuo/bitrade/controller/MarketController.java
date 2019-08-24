package cn.ztuo.bitrade.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cn.ztuo.bitrade.constant.BooleanEnum;
import cn.ztuo.bitrade.constant.SysConstant;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.processor.CoinProcessor;
import cn.ztuo.bitrade.processor.CoinProcessorFactory;
import cn.ztuo.bitrade.service.ExchangeTradeService;
import cn.ztuo.bitrade.service.LeverCoinService;
import cn.ztuo.bitrade.service.MarketService;
import cn.ztuo.bitrade.service.ExchangeCoinService;
import cn.ztuo.bitrade.util.MessageResult;
import cn.ztuo.bitrade.util.RedisUtil;
import cn.ztuo.bitrade.waiting.WaitingOrder;
import cn.ztuo.bitrade.waiting.WaitingOrderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@RestController
public class MarketController extends BaseController{
    @Autowired
    private MarketService marketService;
    @Autowired
    private ExchangeCoinService coinService;
    @Autowired
    private CoinProcessorFactory coinProcessorFactory;
    @Autowired
    private ExchangeTradeService exchangeTradeService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private LeverCoinService leverCoinService;

    @Autowired
    private WaitingOrderFactory factory ;

    @Autowired
    private RedisUtil redisUtil;


    /**
     * 查询默认交易对儿
     * @return
     */
    @RequestMapping(value = "default/symbol",method = RequestMethod.GET)
    public MessageResult findDefaultSymbol(){
        Object redisResult = redisUtil.get(SysConstant.DEFAULT_SYMBOL);
        JSONObject jsonObject = new JSONObject();
        if (redisResult == null){
            ExchangeCoin exchangeCoin = coinService.findByDefault("1");
            if (exchangeCoin != null){
                String result = exchangeCoin.getCoinSymbol()+"_"+exchangeCoin.getBaseSymbol();
                jsonObject.put("app",exchangeCoin.getSymbol().toUpperCase());
                jsonObject.put("web",result.toUpperCase());
                redisUtil.set(SysConstant.DEFAULT_SYMBOL,jsonObject);
                return success(jsonObject);
            }else {
                jsonObject.put("app","BTC/USDT");
                jsonObject.put("web","BTC_USDT");
                return success(jsonObject);
            }
        }else {
            return success(redisResult);
        }




    }




    /**
     * 查询待触发队列中信息
     * @param symbol
     * @param orderId
     * @param direction
     * @return
     */
    @RequestMapping("find/waiting")
    public ExchangeOrder findWaitingOrder(String symbol,String orderId, ExchangeOrderDirection direction){
        WaitingOrder waitingOrder = factory.getWaitingOrder(symbol);
        ExchangeOrder order = waitingOrder.findWaitingOrder(orderId,direction);
        return order;
    }
    /**
     * 获取支持的交易币种
     * @return
     */
    @RequestMapping("symbol")
    public List<ExchangeCoin> findAllSymbol(){
        List<ExchangeCoin> coins = coinService.findAllEnabled();
        return coins;
    }

    @RequestMapping("overview")
    public Map<String,List<CoinThumb>> overview(){
        log.info("/market/overview");
        Calendar calendar = Calendar.getInstance();
        //将秒、微秒字段置为0
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        calendar.set(Calendar.MINUTE,0);
        long nowTime = calendar.getTimeInMillis();
        calendar.add(Calendar.HOUR_OF_DAY,-24);
        long firstTimeOfToday = calendar.getTimeInMillis();
        Map<String,List<CoinThumb>> result = new HashMap<>();
        List<ExchangeCoin> recommendCoin = coinService.findAllByFlag(1);
        List<CoinThumb> recommendThumbs = new ArrayList<>();
        for(ExchangeCoin coin:recommendCoin){
            CoinProcessor processor = coinProcessorFactory.getProcessor(coin.getSymbol());
            if(processor!= null) {
                CoinThumb thumb = processor.getThumb();
                List<KLine> lines = marketService.findAllKLine(thumb.getSymbol(),firstTimeOfToday,nowTime,"1hour");
                List<BigDecimal> trend = new ArrayList();
                for(KLine line:lines){
                    trend.add(line.getClosePrice());
                }
                thumb.setTrend(trend);
                recommendThumbs.add(thumb);
            }
        }
        result.put("recommend",recommendThumbs);
        List<CoinThumb> allThumbs = findSymbolThumb(false);
        Collections.sort(allThumbs, (o1, o2) -> o2.getChg().compareTo(o1.getChg()));
        int limit = allThumbs.size() > 5 ? 5 : allThumbs.size();
        result.put("changeRank",allThumbs.subList(0,limit));
        return result;
    }


    /**
     * 获取某交易对详情
     * @param symbol
     * @return
     */
    @RequestMapping("symbol-info")
    public ExchangeCoin findSymbol(String symbol){
        ExchangeCoin coin = coinService.findBySymbol(symbol);
        return coin;
    }

    /**
     * 获取币种缩略行情
     * @return
     */
    @RequestMapping("symbol-thumb")
    public List<CoinThumb> findSymbolThumb(Boolean isLever){
        List<CoinThumb> thumbs = new ArrayList<>();
        if(isLever!=null&&isLever){
            List<LeverCoin> leverCoinList=leverCoinService.findByEnable(BooleanEnum.IS_TRUE);
            for(LeverCoin leverCoin:leverCoinList){
                CoinProcessor processor=coinProcessorFactory.getProcessor(leverCoin.getSymbol());
                if(processor != null) {
                    CoinThumb thumb = processor.getThumb();
                    thumb.setProportion(leverCoin.getProportion());
                    thumbs.add(thumb);
                }
            }
        }else{
            List<ExchangeCoin> coins = coinService.findAllEnabled();
            for(ExchangeCoin coin:coins){
                CoinProcessor processor = coinProcessorFactory.getProcessor(coin.getSymbol());
                if(processor != null) {
                    CoinThumb thumb = processor.getThumb();
                    thumbs.add(thumb);
                }
            }
        }
        return thumbs;
    }

    @RequestMapping("symbol-thumb-trend")
    public JSONArray findSymbolThumbWithTrend(){
        List<ExchangeCoin> coins = coinService.findAllEnabled();
        //List<CoinThumb> thumbs = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        //将秒、微秒字段置为0
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        calendar.set(Calendar.MINUTE,0);
        long nowTime = calendar.getTimeInMillis();
        calendar.add(Calendar.HOUR_OF_DAY,-24);
        JSONArray array = new JSONArray();
        long firstTimeOfToday = calendar.getTimeInMillis();
        for(ExchangeCoin coin:coins){
            CoinProcessor processor = coinProcessorFactory.getProcessor(coin.getSymbol());
            CoinThumb thumb = processor.getThumb();
            JSONObject json = (JSONObject) JSON.toJSON(thumb);
            json.put("zone",coin.getZone());
            List<KLine> lines = marketService.findAllKLine(thumb.getSymbol(),firstTimeOfToday,nowTime,"1hour");
            JSONArray trend = new JSONArray();
            for(KLine line:lines){
                trend.add(line.getClosePrice());
            }
            json.put("trend",trend);
            array.add(json);
        }
        return array;
    }

    /**
     * 获取币种历史K线
     * @param symbol
     * @param from
     * @param to
     * @param resolution
     * @return
     */
    @RequestMapping("history")
    public JSONArray findKHistory(String symbol,long from,long to,String resolution){
        String period = "";
        if(resolution.endsWith("H") || resolution.endsWith("h")){
            period = resolution.substring(0,resolution.length()-1) + "hour";
        }
        else if(resolution.endsWith("D") || resolution.endsWith("d")){
            period = resolution.substring(0,resolution.length()-1) + "day";
        }
        else if(resolution.endsWith("W") || resolution.endsWith("w")){
            period = resolution.substring(0,resolution.length()-1) + "week";
        }
        else if(resolution.endsWith("M") || resolution.endsWith("m")){
            period = resolution.substring(0,resolution.length()-1) + "month";
        }
        else{
            Integer val = Integer.parseInt(resolution);
            if(val < 60) {
                period = resolution + "min";
            }
            else {
                period = (val/60) + "hour";
            }
        }
        List<KLine> list = marketService.findAllKLine(symbol,from,to,period);

        JSONArray array = new JSONArray();
        for(KLine item:list){
            JSONArray group = new JSONArray();
            group.add(0,item.getTime());
            group.add(1,item.getOpenPrice());
            group.add(2,item.getHighestPrice());
            group.add(3,item.getLowestPrice());
            group.add(4,item.getClosePrice());
            group.add(5,item.getVolume());
            array.add(group);
        }
        return array;
    }

    /**
     * 查询最近成交记录
     * @param symbol 交易对符号
     * @param size 返回记录最大数量
     * @return
     */
    @RequestMapping("latest-trade")
    public List<ExchangeTrade> latestTrade(String symbol, int size){
        ExchangeCoin exchangeCoin = coinService.findBySymbol(symbol);
        List<ExchangeTrade> exchangeTrades = exchangeTradeService.findLatest(symbol,size);
        return exchangeTrades;
    }

    @RequestMapping("exchange-plate")
    public Map<String,List<TradePlateItem>> findTradePlate(String symbol){
        //远程RPC服务URL,后缀为币种单位
        String serviceName = "SERVICE-EXCHANGE-TRADE";
        String url = "http://" + serviceName + "/monitor/plate?symbol="+symbol;
        ResponseEntity<HashMap> result = restTemplate.getForEntity(url, HashMap.class);
        return (Map<String, List<TradePlateItem>>) result.getBody();
    }


    @RequestMapping("exchange-plate-mini")
    public Map<String,JSONObject> findTradePlateMini(String symbol){
        //远程RPC服务URL,后缀为币种单位
        String serviceName = "SERVICE-EXCHANGE-TRADE";
        String url = "http://" + serviceName + "/monitor/plate-mini?symbol="+symbol;
        ResponseEntity<HashMap> result = restTemplate.getForEntity(url, HashMap.class);
        return (Map<String, JSONObject>) result.getBody();
    }


    @RequestMapping("exchange-plate-full")
    public Map<String,JSONObject> findTradePlateFull(String symbol){
        //远程RPC服务URL,后缀为币种单位
        String serviceName = "SERVICE-EXCHANGE-TRADE";
        String url = "http://" + serviceName + "/monitor/plate-full?symbol="+symbol;
        ResponseEntity<HashMap> result = restTemplate.getForEntity(url, HashMap.class);
        return (Map<String,JSONObject>) result.getBody();
    }
}
