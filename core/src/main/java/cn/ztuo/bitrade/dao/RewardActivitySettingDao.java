package cn.ztuo.bitrade.dao;

import cn.ztuo.bitrade.constant.ActivityRewardType;
import cn.ztuo.bitrade.constant.BooleanEnum;
import cn.ztuo.bitrade.dao.base.BaseDao;
import cn.ztuo.bitrade.entity.RewardActivitySetting;

/**
 * @author GuoShuai
 * @date 2018年03月08日
 */
public interface RewardActivitySettingDao extends BaseDao<RewardActivitySetting> {
    RewardActivitySetting findByStatusAndType(BooleanEnum booleanEnum, ActivityRewardType type);
}
