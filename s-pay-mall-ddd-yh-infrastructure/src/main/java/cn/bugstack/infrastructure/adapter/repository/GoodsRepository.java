package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.goods.adapter.repository.IGoodsRepository;
import cn.bugstack.infrastructure.dao.IOrderDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/28 22:03
 */

@Slf4j
@Service
public class GoodsRepository implements IGoodsRepository {

    @Resource
    private IOrderDao orderDao;
    @Override
    public void changeOrderDealDone(String tradeNo) {
        orderDao.changeOrderDealDone(tradeNo);
    }
}
