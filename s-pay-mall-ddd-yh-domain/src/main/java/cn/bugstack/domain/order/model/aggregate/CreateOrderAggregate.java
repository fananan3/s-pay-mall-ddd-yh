package cn.bugstack.domain.order.model.aggregate;

import cn.bugstack.domain.order.model.entity.OrderEntity;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.entity.ProductEntity;
import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.core.annotation.Order;

import java.util.Date;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/27 17:56
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderAggregate {
    private String userId;

    private ProductEntity productEntity;
    private OrderEntity orderEntity;
    public static OrderEntity buildOrderEntity(String productId,String productName,Integer marketType){
        return OrderEntity.builder()
                .productId(productId)
                .productName(productName)
                .orderId(RandomStringUtils.randomNumeric(14))
                .orderTime(new Date())
                .orderStatusVO(OrderStatusVO.CREATE)
                .marketType(marketType)
                .build();
    }
}
