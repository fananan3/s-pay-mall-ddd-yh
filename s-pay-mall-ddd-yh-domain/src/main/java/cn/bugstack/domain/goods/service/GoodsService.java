package cn.bugstack.domain.goods.service;

import cn.bugstack.domain.goods.adapter.repository.IGoodsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/28 22:01
 */

@Slf4j
@Service
public class GoodsService implements IGoodsService{

    @Resource
    private IGoodsRepository goodsRepository;
    @Override
    public void changeOrderDealDone(String tradeNo) {
        goodsRepository.changeOrderDealDone(tradeNo);
    }
}
