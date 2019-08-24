package cn.ztuo.bitrade.controller;


import cn.ztuo.bitrade.entity.CoinThumb;
import cn.ztuo.bitrade.entity.ExchangeTrade;
import cn.ztuo.bitrade.entity.KLine;
import cn.ztuo.bitrade.entity.TradePlateItem;
import cn.ztuo.bitrade.exception.GeneralException;
import cn.ztuo.bitrade.service.ApiMarketService;
import cn.ztuo.bitrade.service.ExchangeTradeService;
import cn.ztuo.bitrade.util.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 *
 */
@RestController
@RequestMapping("open")
@Slf4j
public class OpenApiController extends BaseController{


    @Autowired
    private ApiMarketService apiMarketService;
    @Autowired
    private ExchangeTradeService exchangeTradeService;
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 获取支持的交易对
     * @return
     * @throws GeneralException
     */
    @RequestMapping(value = "symbol_thumb",method = RequestMethod.GET)
    public MessageResult getCoinSymbol()throws  GeneralException{
        List<CoinThumb> thumbs = new ArrayList<>();
        try {
            //远程RPC服务URL,后缀为币种单位
            String serviceName = "BITRADE-MARKET";
            String url = "http://" + serviceName + "/market/symbol-thumb";
            ResponseEntity<List> result = restTemplate.getForEntity(url,List.class);
            log.info("remote call:service={},result={}", serviceName, result);
            if (result.getStatusCode().value() == 200) {
                thumbs.addAll(result.getBody());
            }
        } catch (Exception e) {
            log.info(">>>>>>获取交易对异常>>>>>>"+e);
            throw new GeneralException("GET_COIN_SYMBOL_ERROR",e.getMessage());
        }
        return success(thumbs);
    }

    /**
     * 获取历史K线图 根据时间
     * @return
     * @throws GeneralException
     */
    @RequestMapping(value = "/history/kline",method = RequestMethod.POST)
    public MessageResult getHistoryKline(@RequestParam("symbol")String symbol,@RequestParam("period")String period ,@RequestParam("size") int size)throws GeneralException{
        List<KLine> list;
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH,-1);
            list = apiMarketService.findAllKLine(symbol,calendar.getTimeInMillis(),System.currentTimeMillis(),period);
            if(list.size()>size){
                list=list.subList(0,size);
            }
        } catch (Exception e) {
            log.info(">>>>>>>获取K线图异常>>>>>",e);
            throw new GeneralException("GET_HISTORY_ERROR",e.getMessage());
        }
        return success(list);
    }

    /**
     *  获取盘口信息
     * @return
     * @throws GeneralException
     */
    @RequestMapping(value = "/trade_plate",method = RequestMethod.POST)
    public MessageResult getTradePlateInfo(@RequestParam("symbol")String symbol,@RequestParam("size")int size)throws  GeneralException{
        Map<String,List<TradePlateItem>> result = new HashMap<>();
        try {
            String serviceName = "SERVICE-EXCHANGE-TRADE";
            String url = "http://" + serviceName + "/monitor/plate?symbol="+symbol;
            ResponseEntity<HashMap> resultMap = restTemplate.getForEntity(url, HashMap.class);
            Map<String,List<TradePlateItem>> map= (Map<String, List<TradePlateItem>>) resultMap.getBody();
            List<TradePlateItem> askList = map.get("ask");
            List<TradePlateItem> bidList = map.get("bid");
            if(bidList.size()<size) {
                size = bidList.size();
                result.put("bid", bidList.subList(0, size));
            }
            if(askList.size()<size) {
                size = askList.size();
                result.put("ask", askList.subList(0, size));
            }
        }catch (Exception e){
            log.info(">>>>>>>获取盘口信息异常>>>>>>>",e);
            throw new GeneralException("GET_TRADE_PLATE_ERROR",e);
        }
        return  success(result);
    }

    /**
     * 获取成交历史信息
     * @param symbol
     * @param size
     * @return
     * @throws GeneralException
     */
    @RequestMapping(value = "/trade_history",method = RequestMethod.POST)
    public MessageResult getExchangeTradeHistory(@RequestParam("symbol")String symbol,@RequestParam("size")int size)throws GeneralException{
        List<ExchangeTrade> tradeList ;
        try {
            tradeList=  exchangeTradeService.findLatest(symbol,size);
        }catch (Exception e){
            log.info(">>>>>>查询成交历史出错>>>>>>",e);
            throw  new GeneralException("GET_EXCHNAGE_TRADE_ERROR",e.getMessage());
        }
        return success(tradeList);
    }


}
