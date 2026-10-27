package cn.bugstack.trigger.listener;

import cn.bugstack.api.dto.NotifyRequestDTO;
import cn.bugstack.domain.order.service.IOrderService;
import com.alibaba.fastjson.JSON;
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
 * @author
 * @description 结算完成消息监听（成团结算）
 * 手动 ACK：业务成功 basicAck，失败 basicNack(requeue=false) 进死信队列，避免 requeue 死循环。
 * @create 2025-03-08 13:49
 */
@Slf4j
@Component
public class TeamSuccessTopicListener {


    @Resource
    private IOrderService orderService;
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "${spring.rabbitmq.config.consumer.topic_team_success.queue}",
                            arguments = {
                                    @Argument(name = "x-dead-letter-exchange", value = ""),
                                    @Argument(name = "x-dead-letter-routing-key",
                                            value = "${spring.rabbitmq.config.consumer.topic_team_success.dlq_queue}")
                            }),
                    exchange = @Exchange(value = "${spring.rabbitmq.config.consumer.topic_team_success.exchange}", type = ExchangeTypes.TOPIC),
                    key = "${spring.rabbitmq.config.consumer.topic_team_success.routing_key}"
            )
    )
    public void listener(String message, Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        try {
            NotifyRequestDTO requestDTO = JSON.parseObject(message, NotifyRequestDTO.class);
            log.info("拼团回调，组队完成，结算开始 {}", JSON.toJSONString(requestDTO));
            // 营销结算
            orderService.changeOrderMarketSettlement(requestDTO.getOutTradeNoList());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("拼团回调，组队完成，结算失败，消息进死信:{}", message, e);
            channel.basicNack(tag, false, false);
        }
    }

    /**
     * 死信兜底：结算失败影响订单状态，死信记录留痕 + 报警（TODO 接入钉钉/邮件报警）
     */
    @RabbitListener(queuesToDeclare = @Queue("${spring.rabbitmq.config.consumer.topic_team_success.dlq_queue}"))
    public void dlqListener(String message) {
        log.error("[死信] 成团结算消息处理失败，需关注:{}", message);
    }
}
