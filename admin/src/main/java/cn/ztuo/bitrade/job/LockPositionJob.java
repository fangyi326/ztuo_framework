package cn.ztuo.bitrade.job;

import cn.ztuo.bitrade.service.LockPositionRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class LockPositionJob {
    @Autowired
    private LockPositionRecordService lockPositionRecordService;

    @Scheduled(fixedRate = 60 * 1000)
    public void unlockByTime() {
        Date now=new Date();
        lockPositionRecordService.unlockByTime(now);
    }
}
