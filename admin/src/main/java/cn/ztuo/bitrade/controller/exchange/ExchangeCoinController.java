package cn.ztuo.bitrade.controller.exchange;

import cn.ztuo.bitrade.controller.common.BaseAdminController;
import cn.ztuo.bitrade.core.Encrypt;
import com.alibaba.fastjson.JSONObject;
import cn.ztuo.bitrade.annotation.AccessLog;
import cn.ztuo.bitrade.constant.AdminModule;
import cn.ztuo.bitrade.constant.BooleanEnum;
import cn.ztuo.bitrade.constant.PageModel;
import cn.ztuo.bitrade.constant.SysConstant;
import cn.ztuo.bitrade.entity.Admin;
import cn.ztuo.bitrade.entity.ExchangeCoin;
import cn.ztuo.bitrade.service.ExchangeCoinService;
import cn.ztuo.bitrade.service.LocaleMessageSourceService;
import cn.ztuo.bitrade.util.FileUtil;
import cn.ztuo.bitrade.util.MessageResult;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.Assert.notNull;

/**
 * @author MrGao
 * @description 币币交易手续费
 * @date 2018/1/19 15:16
 */
@RestController
@RequestMapping("exchange/exchange-coin")
public class ExchangeCoinController extends BaseAdminController {

    @Value("${bdtop.system.md5.key}")
    private String md5Key;
    @Autowired
    private LocaleMessageSourceService messageSource;

    @Autowired
    private ExchangeCoinService exchangeCoinService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;



    @RequiresPermissions("exchange:exchange-coin:merge")
    @PostMapping("merge")
    @AccessLog(module = AdminModule.EXCHANGE, operation = "分页查找币币交易手续费exchangeCoin")
    public MessageResult ExchangeCoinList(
            @Valid ExchangeCoin exchangeCoin) {
        if(exchangeCoin.getBaseSymbol().equalsIgnoreCase(exchangeCoin.getCoinSymbol())){
            return MessageResult.error(messageSource.getMessage("Incorrect_Parameters"));
        }
        ExchangeCoin oldCoin=exchangeCoinService.findBySymbol(exchangeCoin.getSymbol());
        if(oldCoin!=null){
            return MessageResult.error(messageSource.getMessage("EXCHANGE_COIN_EXIST"));
        }
        //设置默认交易对儿
        if (exchangeCoin.getDefaultSymbol().equals("1")){
            exchangeCoinService.updateDefault();
        }
        exchangeCoin = exchangeCoinService.save(exchangeCoin);
        if (exchangeCoin != null && exchangeCoin.getEnable() == 1){
            List<ExchangeCoin> result = new ArrayList<>();
            result.add(exchangeCoin);
            //发送Kafka消息，上架行情
            kafkaTemplate.send("exchange-symbol", JSONObject.toJSONString(result));
            kafkaTemplate.send("exchange-market-symbol",JSONObject.toJSONString(result));
        }
        return MessageResult.getSuccessInstance(messageSource.getMessage("SUCCESS"), exchangeCoin);
    }

    @RequiresPermissions("exchange:exchange-coin:page-query")
    @PostMapping("page-query")
    @AccessLog(module = AdminModule.EXCHANGE, operation = "分页查找币币交易手续费exchangeCoin")
    public MessageResult ExchangeCoinList(PageModel pageModel) {
        if (pageModel.getProperty() == null) {
            List<String> list = new ArrayList<>();
            list.add("symbol");
            List<Sort.Direction> directions = new ArrayList<>();
            directions.add(Sort.Direction.DESC);
            pageModel.setProperty(list);
            pageModel.setDirection(directions);
        }
        Page<ExchangeCoin> all = exchangeCoinService.findAll(null, pageModel.getPageable());
        return success(all);
    }

    @RequiresPermissions("exchange:exchange-coin:detail")
    @PostMapping("detail")
    @AccessLog(module = AdminModule.EXCHANGE, operation = "币币交易手续费exchangeCoin 详情")
    public MessageResult detail(
            @RequestParam(value = "symbol") String symbol) {
        ExchangeCoin exchangeCoin = exchangeCoinService.findOne(symbol);
        notNull(exchangeCoin, "validate symbol!");
        return success(exchangeCoin);
    }

    @RequiresPermissions("exchange:exchange-coin:deletes")
    @PostMapping("deletes")
    @AccessLog(module = AdminModule.EXCHANGE, operation = "币币交易手续费exchangeCoin 删除")
    public MessageResult deletes(
            @RequestParam(value = "ids") String[] ids) {
        exchangeCoinService.deletes(ids);
        return success(messageSource.getMessage("SUCCESS"));
    }

    @RequiresPermissions("exchange:exchange-coin:alter-rate")
    @PostMapping("alter-rate")
    @AccessLog(module = AdminModule.EXCHANGE, operation = "修改币币交易手续费exchangeCoin")
    public MessageResult alterExchangeCoinRate(
            @RequestParam("symbol") String symbol,
            @RequestParam(value = "fee", required = false) BigDecimal fee,//交易币手续费
            @RequestParam(value = "baseFee",required =false)BigDecimal baseFee,//结算币种手续费
            @RequestParam(value = "enable", required = false) Integer enable,
            @RequestParam(value = "sort", required = false) Integer sort,
            @RequestParam(value = "password") String password,
            @RequestParam(value = "enableMarketSell",defaultValue = "1")BooleanEnum enableMarketSell,//允许市价卖
            @RequestParam(value = "enableMarketBuy",defaultValue = "1")BooleanEnum  enableMarketBuy,//允许市价买
            @RequestParam(value = "baseCoinScale", required = false) Integer baseCoinScale,//结算币种精度
            @RequestParam(value = "coinScale",required = false) Integer coinScale,//交易币种精度
            @RequestParam(value = "minTurnover",required = false)BigDecimal minTurnover,//最小成交额
            @RequestParam(value = "minSellPrice",required = false)BigDecimal minSellPrice,//最小卖单价
            @RequestParam(value = "maxVolume",required = false)BigDecimal maxOrderQuantity,//最大下单量
            @RequestParam(value = "minVolume",required = false)BigDecimal minOrderQuantity,//最小下单量
            @RequestParam(value = "maxTradingTime",required = false)Integer maxTradingTime,//最大交易时间
            @RequestParam(value = "flag",required = false)Integer flag,//是否推荐
            @RequestParam(value = "defaultSymbol",required = false)String defaultSymbol,//是否默认
            @SessionAttribute(SysConstant.SESSION_ADMIN) Admin admin) {
        password = Encrypt.MD5(password + md5Key);
        Assert.isTrue(password.equals(admin.getPassword()), messageSource.getMessage("WRONG_PASSWORD"));
        ExchangeCoin exchangeCoin = exchangeCoinService.findOne(symbol);
        notNull(exchangeCoin, "validate symbol!");
        if (fee != null&&fee.compareTo(BigDecimal.ZERO)>=0)
            exchangeCoin.setFee(fee);//修改手续费
        if (sort != null)
            exchangeCoin.setSort(sort);//设置排序
        if (enable != null && enable > 0 && enable < 3)
            exchangeCoin.setEnable(enable);//设置启用 禁用
        if(baseCoinScale!=null) {
            exchangeCoin.setBaseCoinScale(baseCoinScale);
        }
        if(coinScale!=null) {
            exchangeCoin.setCoinScale(coinScale);
        }
        if(exchangeCoin.getBaseCoinScale()+exchangeCoin.getCoinScale()>8){
            return MessageResult.error(messageSource.getMessage("ACCURACY_ERROR"));
        }
        if(maxOrderQuantity!=null){
            exchangeCoin.setMaxVolume(maxOrderQuantity);
        }
        if(minOrderQuantity!=null){
            exchangeCoin.setMinVolume(minOrderQuantity);
        }
        if(baseFee!=null&&baseFee.compareTo(BigDecimal.ZERO)>=0){
            exchangeCoin.setBaseFee(baseFee);//修改结算币种手续费
        }
        if(enableMarketSell!=null){
            exchangeCoin.setEnableMarketSell(enableMarketSell);
        }
        if(enableMarketBuy!=null){
            exchangeCoin.setEnableMarketBuy(enableMarketBuy);
        }
        if(minTurnover!=null){
            exchangeCoin.setMinTurnover(minTurnover);
        }
        if(minSellPrice!=null){
            exchangeCoin.setMinSellPrice(minSellPrice);
        }
        if(maxTradingTime!=null){
            exchangeCoin.setMaxTradingTime(maxTradingTime);
        }
        if(flag!=null){
            exchangeCoin.setFlag(flag);
        }
        //设置默认交易对儿
        if (defaultSymbol != null && defaultSymbol.equals("1")){
            exchangeCoin.setDefaultSymbol("1");
            exchangeCoinService.updateDefault();
        }
        exchangeCoinService.save(exchangeCoin);
        return success(messageSource.getMessage("SUCCESS"));
    }

    @RequiresPermissions("exchange:exchange-coin:out-excel")
    @GetMapping("out-excel")
    @AccessLog(module = AdminModule.EXCHANGE, operation = "导出币币交易手续费exchangeCoin Excel")
    public MessageResult outExcel(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List all = exchangeCoinService.findAll();
        return new FileUtil().exportExcel(request, response, all, "exchangeCoin");
    }

    /**
     * 获取所有交易区币种的单位
     *
     * @return
     */
    @PostMapping("all-base-symbol-units")
    public MessageResult getAllBaseSymbolUnits() {
        List<String> list = exchangeCoinService.getBaseSymbol();
        return success(messageSource.getMessage("SUCCESS"), list);
    }

    /**
     * 获取交易区币种 所支持的交易 币种
     *
     * @return
     */
    @PostMapping("all-coin-symbol-units")
    public MessageResult getAllCoinSymbolUnits(@RequestParam("baseSymbol") String baseSymbol) {
        List<String> list = exchangeCoinService.getCoinSymbol(baseSymbol);
        return success(messageSource.getMessage("SUCCESS"), list);
    }

}
