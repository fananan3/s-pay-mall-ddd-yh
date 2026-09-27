package cn.bugstack.domain.order.model.entity;


import cn.bugstack.domain.order.model.valobj.OrderStatusVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/26 21:06
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayOrderEntity {

    private String userId;
    private String orderId;
    private String payUrl;
    private OrderStatusVO orderStatusVO;

}