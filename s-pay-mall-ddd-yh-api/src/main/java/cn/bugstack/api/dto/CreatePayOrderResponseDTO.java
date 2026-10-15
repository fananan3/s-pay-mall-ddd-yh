package cn.bugstack.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @Description 下单应答对象：除了支付宝收银台表单，还要把营销结果告诉前端
 *              前端据此判断"拼团优惠是否生效"，不生效（锁单失败降级）时要提示用户按原价支付
 * @Author fanyiheng
 * @Date 2026/9/29 16:20
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePayOrderResponseDTO {

    /** 订单号 */
    private String orderId;

    /** 支付宝收银台表单 HTML，前端插入页面后 submit 即可跳转 */
    private String payUrl;

    /** 营销类型：0 无营销（按原价支付）、1 拼团营销 */
    private Integer marketType;

    /** 本单实付金额（降级时就是商品原价） */
    private BigDecimal payAmount;

    /** 营销优惠金额（降级时为 0） */
    private BigDecimal marketDeductionAmount;

    /**
     * 是否发生了"营销兜底"：请求的是拼团单（marketType=1），但订单最终没有营销优惠（marketType 回落为 0）。
     * 常见原因：拼团名额已用完、活动已失效、拼团服务不可用等。
     * true 时前端必须提示用户：拼团名额已用完，本单将按原价支付。
     */
    private Boolean marketFallback;

}
