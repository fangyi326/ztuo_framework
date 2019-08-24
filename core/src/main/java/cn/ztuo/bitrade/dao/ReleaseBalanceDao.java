package cn.ztuo.bitrade.dao;


import cn.ztuo.bitrade.dao.base.BaseDao;
import cn.ztuo.bitrade.entity.ReleaseBalance;
import org.springframework.data.jpa.repository.Query;


import java.util.Date;
import java.util.List;

public interface ReleaseBalanceDao extends BaseDao<ReleaseBalance> {


    /**
     * 审核送币
     */
    ReleaseBalance findOne(Long memberId);
}
