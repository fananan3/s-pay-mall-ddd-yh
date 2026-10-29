package cn.bugstack.infrastructure.gateway;


import cn.bugstack.infrastructure.gateway.dto.*;
import cn.bugstack.infrastructure.gateway.response.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "gbm-service", path = "/api/v1/gbm/trade")
public interface IGroupBuyMarketFeignClient {

    /**
     * 营销锁单
     *
     * @param requestDTO 锁单商品信息
     * @return 锁单结果信息
     */
    @PostMapping("/lock_market_pay_order/")
    Response<LockMarketPayOrderResponseDTO> lockMarketPayOrder(@RequestBody LockMarketPayOrderRequestDTO requestDTO);

    /**
     * 营销结算
     *
     * @param req 结算商品信息
     * @return 结算结果信息
     */
    @PostMapping("/settlement_market_pay_order/")
    Response<SettlementMarketPayOrderResponseDTO> settlementMarketPayOrder(@RequestBody SettlementMarketPayOrderRequestDTO req);

    /**
     * 营销拼团退单
     *
     * @param req 退单请求信息
     * @return 退单结果信息
     */
    @PostMapping("/refund_market_pay_order")
    Response<RefundMarketPayOrderResponseDTO> refundMarketPayOrder(@RequestBody RefundMarketPayOrderRequestDTO req);
}
