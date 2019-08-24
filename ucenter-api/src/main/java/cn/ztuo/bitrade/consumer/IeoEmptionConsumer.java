package cn.ztuo.bitrade.consumer;

import com.alibaba.fastjson.JSONObject;
import cn.ztuo.bitrade.entity.EmptionRecord;
import cn.ztuo.bitrade.entity.IeoEmption;
import cn.ztuo.bitrade.entity.Member;
import cn.ztuo.bitrade.service.EmptionRecordService;
import cn.ztuo.bitrade.service.IeoEmptionService;
import cn.ztuo.bitrade.service.MemberService;
import cn.ztuo.bitrade.util.GeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Description:
 * @Author: GuoShuai
 * @Date: 2019/4/27 10:52 AM
 */
@Component
@Slf4j
public class IeoEmptionConsumer {

    @Autowired
    private IeoEmptionService ieoEmptionService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private EmptionRecordService emptionRecordService;


    /**
     * 处理IEO订购单
     * @param content
     */
    @KafkaListener(topics = {"ieo-order"})
    public void handlerIeoOrder(String content){
        log.info("-----处理用户IEO订单---"+content);
        Date date = new Date();
        JSONObject receiveContent = JSONObject.parseObject(content);
        IeoEmption ieoEmption = ieoEmptionService.findById(receiveContent.getLong("ieoId"));
        BigDecimal amount = receiveContent.getBigDecimal("amount");

        BigDecimal successRatio = ieoEmption.getSuccessRatio().multiply(BigDecimal.valueOf(100L));
        EmptionRecord emptionRecord = new EmptionRecord();
        Member member = memberService.findOne(receiveContent.getLong("uid"));
        emptionRecord.setCreateTime(date);
        emptionRecord.setEndTime(ieoEmption.getEndTime());
        emptionRecord.setExpectTime(ieoEmption.getExpectTime());
        emptionRecord.setIeoName(ieoEmption.getIeoName());
        emptionRecord.setRaiseCoin(ieoEmption.getRaiseCoin());
        emptionRecord.setRatio(ieoEmption.getRatio());
        emptionRecord.setSaleAmount(ieoEmption.getSaleAmount());
        emptionRecord.setSaleCoin(ieoEmption.getSaleCoin());
        emptionRecord.setStartTime(ieoEmption.getStartTime());
        emptionRecord.setStatus("0");
        emptionRecord.setUserId(member.getId());
        emptionRecord.setUserMobile(member.getMobilePhone());
        emptionRecord.setUserName(member.getUsername());
        emptionRecord.setIeoId(ieoEmption.getId());
        emptionRecord.setPicView(ieoEmption.getPicView());

        emptionRecord.setPayAmount(amount);
        BigDecimal receAmount = amount.multiply(ieoEmption.getRatio()).setScale(4, BigDecimal.ROUND_DOWN);
        emptionRecord.setReceiveAmount(receAmount);

        Integer successInteger = successRatio.intValue();
        //设置随机数
        Integer random = GeneratorUtil.getRandomNumber(10,99);
        //0.判断用户成功率
        if (successInteger > random){
            synchronized (this){
                //减少库存
                int subResult = 0;
                try {
                    subResult = ieoEmptionService.subAmount(amount,ieoEmption,member.getId());
                } catch (Exception e) {
                    log.info("-----更新用户余额异常={}",e);
                    emptionRecordService.save(emptionRecord);
                    return;
                }
                if (subResult == 1){
                    emptionRecord.setStatus("1");
                    emptionRecord.setPayAmount(amount);
                    emptionRecord.setReceiveAmount(receAmount);
                    //3.保存订单
                    emptionRecordService.save(emptionRecord);

                }else {
                    emptionRecordService.save(emptionRecord);
                    return;
                }
            }
        }else {
            emptionRecordService.save(emptionRecord);
            return;
        }

    }
}
