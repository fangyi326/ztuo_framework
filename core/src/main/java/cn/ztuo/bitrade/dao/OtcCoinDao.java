package cn.ztuo.bitrade.dao;

import cn.ztuo.bitrade.constant.CommonStatus;
import cn.ztuo.bitrade.dao.base.BaseDao;
import cn.ztuo.bitrade.entity.OtcCoin;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * @author GuoShuai
 * @date 2018年01月12日
 */
public interface OtcCoinDao extends BaseDao<OtcCoin> {

    OtcCoin findOtcCoinByUnitAndStatus(String unit, CommonStatus status);

    List<OtcCoin> findAllByStatus(CommonStatus status);

    OtcCoin findOtcCoinByUnit(String unit);

    @Query("select distinct a.unit from OtcCoin a where a.status = 0")
    List<String> findAllUnits();



}
