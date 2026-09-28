package cn.bugstack.domain.order.service.impl;

import cn.bugstack.domain.order.adapter.port.IProductPort;
import cn.bugstack.domain.order.adapter.repository.IOrderRepository;
import cn.bugstack.domain.order.model.aggregate.CreateOrderAggregate;
import cn.bugstack.domain.order.model.entity.*;
import cn.bugstack.domain.order.model.valobj.MarketTypeVO;
import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import cn.bugstack.domain.order.service.IOrderService;

import com.alipay.api.AlipayApiException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;


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
            //  掉单，需要重新调用支付沙箱创建支付订单
            Integer marketType = unpaidOrderEntity.getMarketType();
            BigDecimal marketDeductionAmount = unpaidOrderEntity.getMarketDeductionAmount();
            PayOrderEntity payOrderEntity = null;

            if (MarketTypeVO.GROUP_BUY_MARKET.getCode().equals(marketType) && marketDeductionAmount == null){
                MarketPayDiscountEntity marketPayDiscountEntity =  this.lockMarketPayOrder(shopCartEntity.getUserId(),
                        shopCartEntity.getTeamId(),
                        shopCartEntity.getActivityId(),
                        shopCartEntity.getProductId(),
                        unpaidOrderEntity.getOrderId());
                payOrderEntity = doPrepayOrder(shopCartEntity.getUserId(), shopCartEntity.getProductId(),
                        unpaidOrderEntity.getProductName(), unpaidOrderEntity.getOrderId(), unpaidOrderEntity.getTotalAmount(), marketPayDiscountEntity);
            } else if (MarketTypeVO.GROUP_BUY_MARKET.getCode().equals(marketType)) {
                payOrderEntity = doPrepayOrder(shopCartEntity.getUserId(), shopCartEntity.getProductId(),
                        unpaidOrderEntity.getProductName(), unpaidOrderEntity.getOrderId(), unpaidOrderEntity.getPayAmount());
            }else {
                payOrderEntity = doPrepayOrder(shopCartEntity.getUserId(), shopCartEntity.getProductId(),
                        unpaidOrderEntity.getProductName(), unpaidOrderEntity.getOrderId(), unpaidOrderEntity.getTotalAmount());
            }

            return PayOrderEntity.builder()
                    .orderId(payOrderEntity.getOrderId())
                    .payUrl(payOrderEntity.getPayUrl())
                    .build();
        }
        // 查询商品
        ProductEntity productEntity = productPort.queryProductByProductId(shopCartEntity.getProductId());

        // 订单实体信息
        OrderEntity orderEntity = CreateOrderAggregate.buildOrderEntity(shopCartEntity.getProductId(), productEntity.getProductName(),shopCartEntity.getMarketTypeVO().getCode());
        // 订单聚合对象
        CreateOrderAggregate orderAggregate = CreateOrderAggregate.builder()
                .userId(shopCartEntity.getUserId())
                .productEntity(productEntity)
                .orderEntity(orderEntity)
                .build();
        // 创建本地订单
        this.doSaveOrder(orderAggregate);

        // 发起营销锁单
        MarketPayDiscountEntity marketPayDiscountEntity = null;
        if (MarketTypeVO.GROUP_BUY_MARKET.equals(shopCartEntity.getMarketTypeVO())){
            marketPayDiscountEntity = this.lockMarketPayOrder(shopCartEntity.getUserId(),
                    shopCartEntity.getTeamId(),
                    shopCartEntity.getActivityId(),
                    shopCartEntity.getProductId(),
                    orderEntity.getOrderId());
        }
        // 创建支付订单
        PayOrderEntity payOrderEntity = doPrepayOrder(shopCartEntity.getUserId(),
                productEntity.getProductId(),
                productEntity.getProductName(),
                orderEntity.getOrderId(),
                productEntity.getPrice(),
                marketPayDiscountEntity);

        return PayOrderEntity.builder()
                .orderId(orderEntity.getOrderId())
                .payUrl(payOrderEntity.getPayUrl())
                .build();
    }

    protected abstract PayOrderEntity doPrepayOrder(String userId, String productId, String productName, String orderId, BigDecimal totalAmount) throws AlipayApiException;

    protected abstract PayOrderEntity doPrepayOrder(String userId, String productId, String productName, String orderId, BigDecimal totalAmount, MarketPayDiscountEntity marketPayDiscountEntity) throws AlipayApiException;

    protected abstract MarketPayDiscountEntity lockMarketPayOrder(String userId, String teamId, Long activityId, String productId, String orderId);
    protected abstract void doSaveOrder(CreateOrderAggregate createOrderAggregate);
}
