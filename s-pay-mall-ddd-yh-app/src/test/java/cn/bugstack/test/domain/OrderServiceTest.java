package cn.bugstack.test.domain;

import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.entity.ShopCartEntity;
import cn.bugstack.domain.order.service.IOrderService;
import cn.bugstack.domain.order.service.impl.OrderService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/27 20:58
 */

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class OrderServiceTest {

    @Resource
    private IOrderService orderService;
    @Test
    public void createPayOrderEntity_test() throws Exception {

        ShopCartEntity shopCartEntity = new ShopCartEntity();
        shopCartEntity.setUserId("123");
        shopCartEntity.setProductId("123");
        PayOrderEntity order = orderService.createOrder(shopCartEntity);
        log.info("请求参数：{}",shopCartEntity);
        log.info("测试结果：{}", JSON.toJSONString(order));
    }
}
