package cn.ztuo.bitrade.dao;

import cn.ztuo.bitrade.constant.BooleanEnum;
import cn.ztuo.bitrade.dao.base.BaseDao;
import cn.ztuo.bitrade.entity.Vote;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author GuoShuai
 * @date 2018年03月26日
 */
public interface VoteDao extends BaseDao<Vote> {

    Vote findVoteByStatus(BooleanEnum var);

    Vote findFirstByOrderByIdDesc();

    @Modifying
    @Transactional(rollbackFor = Exception.class)
    @Query("update Vote vote set vote.status = :status where vote.status = :lastStatus")
    int turnOffAllVote(@Param("status") BooleanEnum status, @Param("lastStatus") BooleanEnum lastStatus);
}
