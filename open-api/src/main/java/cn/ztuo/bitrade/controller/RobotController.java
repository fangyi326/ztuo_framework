package cn.ztuo.bitrade.controller;

import com.alibaba.fastjson.JSONObject;
import cn.ztuo.bitrade.constant.SysConstant;
import cn.ztuo.bitrade.entity.ExchangeOrder;
import cn.ztuo.bitrade.entity.InitPlate;
import cn.ztuo.bitrade.service.ExchangeOrderService;
import cn.ztuo.bitrade.service.InitPlateService;
import cn.ztuo.bitrade.util.MessageResult;
import cn.ztuo.bitrade.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RequestMapping("robot")
@RestController
@Slf4j
public class RobotController {

    @Autowired
    private InitPlateService initPlateService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private ExchangeOrderService orderService ;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @PostMapping("init_plate")
    @ResponseBody
    public InitPlate getInitPlateForRobot(){
        //从缓存中取
        String key = SysConstant.EXCHANGE_INIT_PLATE_SYMBOL_KEY+"HTL/ETH";
        Object object = redisUtil.get(key);//获取initPlate String
        InitPlate initPlate ;
        log.info(">>缓存中>>>object "+object);
        if(object==null){
            //从库中读取
            initPlate = initPlateService.findInitPlateBySymbol("HTL/ETH");
            redisUtil.set(key,JSONObject.toJSONString(initPlate),SysConstant.EXCHANGE_INIT_PLATE_SYMBOL_EXPIRE_TIME,TimeUnit.SECONDS);
        }else {
            initPlate = JSONObject.parseObject(object.toString(),InitPlate.class);
        }

        return initPlate;
    }

    @PostMapping("canceld_order")
    public MessageResult canceldOderByMemberId(@RequestParam long sellMemberId ,@RequestParam long canceldTime,
                                               @RequestParam long buyMemberId)throws Exception{
        MessageResult messageResult = new MessageResult();
        //只取消HTL机器人账号
        List<Long> memberIdList = new ArrayList<>();
        memberIdList.add(119284L);
        memberIdList.add(76895L);
        if(memberIdList.contains(sellMemberId) && memberIdList.contains(buyMemberId)){
            List<ExchangeOrder> orders = orderService.queryExchangeOrderByTimeById(canceldTime);
            if (orders.size() != 0) {
                for (ExchangeOrder order : orders) {
                    log.info(">>>>>>此次取消订单的会员id" + order.getMemberId() + ">>>取消订单的id为>>>" + order.getOrderId() +
                            ">>订单创建时间为>>>" + DateFormatUtils.format(order.getTime(), "yyyy-MM-dd HH:mm:ss:SSS"));
                    // 发送消息至Exchange系统
                    kafkaTemplate.send("exchange-order-cancel", JSONObject.toJSONString(order));
                    Thread.sleep(10);
                }
            }
            messageResult.setCode(0);
            messageResult.setMessage("success");
        }else {
            messageResult.setCode(500);
            messageResult.setMessage("会员不合法");
        }
        return messageResult;
    }


}
