package cn.bugstack.infrastructure.gateway.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/27 20:18
 */
@Data
public class ProductDTO {

    /** 商品ID */
    private String productId;
    /** 商品名称 */
    private String productName;
    /** 商品描述 */
    private String productDesc;
    /** 商品价格 */
    private BigDecimal price;
}
