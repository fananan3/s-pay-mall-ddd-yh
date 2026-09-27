package cn.bugstack.infrastructure.adapter.port;

import cn.bugstack.domain.order.adapter.port.IProductPort;
import cn.bugstack.domain.order.model.entity.ProductEntity;
import cn.bugstack.infrastructure.gateway.ProductRPC;
import cn.bugstack.infrastructure.gateway.dto.ProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/27 20:16
 */

@Slf4j
@Service
public class ProductPort implements IProductPort {

    @Resource
    private ProductRPC productRPC;
    @Override
    public ProductEntity queryProductByProductId(String productId) {
        ProductDTO productDTO = productRPC.queryProductByProductId(productId);
        return ProductEntity.builder()
                .productName(productDTO.getProductName())
                .productId(productDTO.getProductId())
                .productDesc(productDTO.getProductDesc())
                .price(productDTO.getPrice())
                .build();
    }
}
