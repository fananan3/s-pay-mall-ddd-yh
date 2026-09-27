package cn.bugstack.domain.order.service.impl;

import cn.bugstack.domain.order.adapter.port.IProductPort;
import cn.bugstack.domain.order.adapter.repository.IOrderRepository;
import cn.bugstack.domain.order.model.aggregate.CreateOrderAggregate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/27 18:06
 */

@Service
@Slf4j
public class OrderService extends AbstractOrderService{

    public OrderService(IOrderRepository repository, IProductPort port){
        super(repository,port);
    }

    @Override
    protected void doSaveOrder(CreateOrderAggregate createOrderAggregate) {
        orderRepository.doSaveOrder(createOrderAggregate);
    }
}
