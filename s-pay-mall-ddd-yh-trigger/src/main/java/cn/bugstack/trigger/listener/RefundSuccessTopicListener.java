package cn.bugstack.trigger.listener;

import cn.bugstack.api.dto.TeamRefundSuccessRequestDTO;
import cn.bugstack.domain.order.service.IOrderService;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @Description 营销退单成功消息（退款，资金场景）
 * 手动 ACK：业务成功 basicAck，失败 basicNack(requeue=false) 进死信队列，
 * 避免毒消息 requeue 死循环；死信消费者报警并留痕，等待人工处理资金。
 * @Author fanyiheng
 * @Date 2026/10/8 00:01
 */
@Slf4j
@Component
public class RefundSuccessTopicListener {
    @Resource
    private IOrderService orderService;


    /**
     * 指定消费队列
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "${spring.rabbitmq.config.consumer.topic_team_refund.queue}",
                            arguments = {
                                    @Argument(name = "x-dead-letter-exchange", value = ""),
                                    @Argument(name = "x-dead-letter-routing-key",
                                            value = "${spring.rabbitmq.config.consumer.topic_team_refund.dlq_queue}")
                            }),
                    exchange = @Exchange(value = "${spring.rabbitmq.config.consumer.topic_team_refund.exchange}", type = ExchangeTypes.TOPIC),
                    key = "${spring.rabbitmq.config.consumer.topic_team_refund.routing_key}"
            )
    )
    public void listener(String message, Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        try {
            log.info("退单回调，发起退款 {}", message);
            TeamRefundSuccessRequestDTO requestDTO = JSON.parseObject(message, TeamRefundSuccessRequestDTO.class);
            String type = requestDTO.getType();
            if ("unpaid_unlock".equals(type)) {
                // 未支付：没收到钱，不需要调支付宝，直接把商城订单关掉（要幂等）
                orderService.closeRefundOrder(requestDTO.getUserId(), requestDTO.getOutTradeNo());
            } else if ("paid_unformed".equals(type) || "paid_formed".equals(type)) {
                orderService.refundPayOrder(requestDTO.getUserId(), requestDTO.getOutTradeNo());
            } else {
                log.warn("未知退单类型，忽略 type:{} message:{}", type, message);
            }
            channel.basicAck(tag, false);
        } catch (AlipayApiException exception) {
            log.error("[资金] 支付宝退款异常，消息进死信待人工:{}", message, exception);
            channel.basicNack(tag, false, false);
        } catch (Exception e) {
            log.error("[资金] 拼团回调，退单完成，退款失败，消息进死信待人工:{}", message, e);
            channel.basicNack(tag, false, false);
        }
    }

    /**
     * 死信兜底：资金场景，必须报警 + 人工介入（TODO 接入钉钉/邮件报警）
     */
    @RabbitListener(queuesToDeclare = @Queue("${spring.rabbitmq.config.consumer.topic_team_refund.dlq_queue}"))
    public void dlqListener(String message) {
        log.error("[资金安全][死信] 退款消息处理失败，需人工介入处理退款:{}", message);
    }
}
