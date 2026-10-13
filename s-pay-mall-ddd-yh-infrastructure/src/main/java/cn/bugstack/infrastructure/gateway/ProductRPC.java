package cn.bugstack.infrastructure.gateway;


import cn.bugstack.infrastructure.gateway.dto.ProductDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/26 21:09
 */
@Service
public class ProductRPC {
    public ProductDTO queryProductByProductId(String productId){
        ProductDTO productDTO = new ProductDTO();
        productDTO.setProductId(productId);
        productDTO.setProductName("MyBatisBook");
        productDTO.setProductDesc("MyBatisBook");
        productDTO.setPrice(new BigDecimal("100.00"));
        return productDTO;
    }
}
