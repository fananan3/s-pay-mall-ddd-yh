package cn.bugstack.domain.order.service.impl;

import cn.bugstack.domain.order.adapter.port.IProductPort;
import cn.bugstack.domain.order.adapter.repository.IOrderRepository;
import cn.bugstack.domain.order.model.aggregate.CreateOrderAggregate;
import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.entity.ProductEntity;
import cn.bugstack.domain.order.model.entity.ShopCartEntity;
import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import cn.bugstack.domain.order.service.IOrderService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;


/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/26 21:04
 */

@Slf4j
@Service
public abstract class AbstractOrderService implements IOrderService {




    protected final IProductPort productPort;


    protected final IOrderRepository orderRepository;

    public AbstractOrderService(IOrderRepository repository, IProductPort port){
        this.productPort = port;
        this.orderRepository = repository;
    }

    @Override
    public PayOrderEntity createOrder(ShopCartEntity shopCartEntity) throws Exception {



        // 1. 查询当前用户是否存在未支付订单或掉单订单
        OrderEntity unpaidOrderEntity = orderRepository.queryUnPayOrder(shopCartEntity);

        if (unpaidOrderEntity != null && unpaidOrderEntity.getOrderStatusVO().equals(OrderStatusVO.PAY_WAIT)){
            log.info("创建订单-存在，已存在未支付订单。userId:{} productId:{} orderId:{}", shopCartEntity.getUserId(), shopCartEntity.getProductId(), unpaidOrderEntity.getOrderId());

            return PayOrderEntity.builder()
                    .orderId(unpaidOrderEntity.getOrderId())
                    .payUrl(unpaidOrderEntity.getPayUrl())
                    .build();

        } else if (unpaidOrderEntity != null && unpaidOrderEntity.getOrderStatusVO().equals(OrderStatusVO.CREATE)) {
            // TODO 掉单，需要重新调用支付沙箱创建支付订单
        }
        // 2. 查询商品 & 创建订单
        ProductEntity productEntity = productPort.queryProductByProductId(shopCartEntity.getProductId());

        OrderEntity orderEntity = CreateOrderAggregate.buildOrderEntity(shopCartEntity.getProductId(), productEntity.getProductName());

        CreateOrderAggregate orderAggregate = CreateOrderAggregate.builder()
                .userId(shopCartEntity.getUserId())
                .productEntity(productEntity)
                .orderEntity(orderEntity)
                .build();

        this.doSaveOrder(orderAggregate);


        // 3. 创建支付单 todo

        return PayOrderEntity.builder()
                .orderId(orderEntity.getOrderId())
                .payUrl("暂无")
                .build();
    }

    protected abstract void doSaveOrder(CreateOrderAggregate createOrderAggregate);
}
