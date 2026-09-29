package cn.bugstack.domain.goods.adapter.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/28 22:02
 */


public interface IGoodsRepository {
    void changeOrderDealDone(String tradeNo);
}
