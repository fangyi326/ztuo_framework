package cn.ztuo.bitrade.dao;

import cn.ztuo.bitrade.entity.Vote;
import cn.ztuo.bitrade.entity.VoteDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * @author GuoShuai
 * @date 2018年03月30日
 */
public interface VoteDetailDao extends JpaRepository<VoteDetail,Long>,JpaSpecificationExecutor<VoteDetail> {
    List<VoteDetail> findAllByUserIdAndVote(long var1, Vote vote);
}
