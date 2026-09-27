package cn.bugstack.domain.order.model.entity;

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
public class ShopCartEntity {

    private String userId;

    private String productId;

}
