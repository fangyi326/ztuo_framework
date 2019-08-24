package cn.ztuo.bitrade.service;

import com.querydsl.core.types.Predicate;
import cn.ztuo.bitrade.constant.BooleanEnum;
import cn.ztuo.bitrade.dao.VoteDao;
import cn.ztuo.bitrade.entity.Vote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * @author GuoShuai
 * @date 2018年03月26日
 */
@Service
public class VoteService {

    @Autowired
    private VoteDao voteDao;

    public Vote findById(Long id){
        return voteDao.findOne(id);
    }

    public Vote findVote(){
       return voteDao.findFirstByOrderByIdDesc();
    }

    public Vote save(Vote vote){
        return voteDao.save(vote);
    }

    public Page<Vote> findAll(Predicate predicate, Pageable pageable) {
        return voteDao.findAll(predicate, pageable);
    }

    public void turnOffAllVote(){
        voteDao.turnOffAllVote(BooleanEnum.IS_FALSE,BooleanEnum.IS_TRUE) ;
    }

}
