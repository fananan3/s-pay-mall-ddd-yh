package cn.bugstack.trigger.http;


import cn.bugstack.api.IPayService;
import cn.bugstack.api.dto.CreatePayOrderResponseDTO;
import cn.bugstack.api.dto.CreatePayRequestDTO;
import cn.bugstack.api.dto.NotifyRequestDTO;
import cn.bugstack.api.response.Response;
import cn.bugstack.domain.order.model.entity.PayOrderEntity;
import cn.bugstack.domain.order.model.entity.ShopCartEntity;
import cn.bugstack.domain.order.model.valobj.MarketTypeVO;
import cn.bugstack.domain.order.service.IOrderService;
import cn.bugstack.types.enums.ResponseCode;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/alipay/")
public class AliPayController implements IPayService {

    @Value("${alipay.alipay_public_key}")
    private String alipayPublicKey;

    @Resource
    private IOrderService orderService;

    /**
     * http://localhost:8080/api/v1/alipay/create_pay_order
     *
     * {
     *     "userId": "10001",
     *     "productId": "100001"
     * }
     */
    @Override
    @RequestMapping(value = "create_pay_order", method =  RequestMethod.POST)
    public Response<CreatePayOrderResponseDTO> createPayOrder(@RequestBody CreatePayRequestDTO createPayRequestDTO){
        try {
            log.info("商品下单，根据商品ID创建支付单开始 userId:{} productId:{}", createPayRequestDTO.getUserId(), createPayRequestDTO.getUserId());
            String userId = createPayRequestDTO.getUserId();
            String productId = createPayRequestDTO.getProductId();
            String teamId = createPayRequestDTO.getTeamId();
            Integer marketType = createPayRequestDTO.getMarketType();
            // 下单
            PayOrderEntity payOrderRes = orderService.createOrder(ShopCartEntity.builder()
                    .userId(userId)
                    .productId(productId)
                    .teamId(teamId)
                    .marketTypeVO(MarketTypeVO.valueOf(marketType))
                    .activityId(createPayRequestDTO.getActivityId())
                    .build());

            // 请求的是拼团营销单，但订单最终没有营销优惠（market_type 回落为 0）：
            // 说明营销锁单失败（拼团名额已用完、活动失效、拼团服务不可用等），本单已按原价创建，
            // 必须让前端把这件事明确告诉用户，避免用户以为自己在按拼团价付款。
            boolean marketFallback = MarketTypeVO.GROUP_BUY_MARKET.getCode().equals(marketType)
                    && !MarketTypeVO.GROUP_BUY_MARKET.getCode().equals(payOrderRes.getMarketType());

            log.info("商品下单，根据商品ID创建支付单完成 userId:{} productId:{} orderId:{} marketType:{} payAmount:{} marketFallback:{}",
                    userId, productId, payOrderRes.getOrderId(), payOrderRes.getMarketType(), payOrderRes.getPayAmount(), marketFallback);

            return Response.<CreatePayOrderResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(CreatePayOrderResponseDTO.builder()
                            .orderId(payOrderRes.getOrderId())
                            .payUrl(payOrderRes.getPayUrl())
                            .marketType(payOrderRes.getMarketType())
                            .payAmount(payOrderRes.getPayAmount())
                            .marketDeductionAmount(payOrderRes.getMarketDeductionAmount())
                            .marketFallback(marketFallback)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("商品下单，根据商品ID创建支付单失败 userId:{} productId:{}", createPayRequestDTO.getUserId(), createPayRequestDTO.getUserId(), e);
            return Response.<CreatePayOrderResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * http://xfg-studio.natapp1.cc/api/v1/alipay/alipay_notify_url
     */
    @RequestMapping(value = "alipay_notify_url", method = RequestMethod.POST)
    public String payNotify(HttpServletRequest request) throws AlipayApiException, ParseException {
        log.info("支付回调，消息接收 {}", request.getParameter("trade_status"));

        if (!request.getParameter("trade_status").equals("TRADE_SUCCESS")) {
            return "false";
        }

        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            params.put(name, request.getParameter(name));
        }

        String tradeNo = params.get("out_trade_no");
        String gmtPayment = params.get("gmt_payment");
        String alipayTradeNo = params.get("trade_no");

        String sign = params.get("sign");
        String content = AlipaySignature.getSignCheckContentV1(params);
        boolean checkSignature = AlipaySignature.rsa256CheckContent(content, sign, alipayPublicKey, "UTF-8"); // 验证签名
        // 支付宝验签
        if (!checkSignature) {
            return "false";
        }

        // 验签通过
        log.info("支付回调，交易名称: {}", params.get("subject"));
        log.info("支付回调，交易状态: {}", params.get("trade_status"));
        log.info("支付回调，支付宝交易凭证号: {}", params.get("trade_no"));
        log.info("支付回调，商户订单号: {}", params.get("out_trade_no"));
        log.info("支付回调，交易金额: {}", params.get("total_amount"));
        log.info("支付回调，买家在支付宝唯一id: {}", params.get("buyer_id"));
        log.info("支付回调，买家付款时间: {}", params.get("gmt_payment"));
        log.info("支付回调，买家付款金额: {}", params.get("buyer_pay_amount"));
        log.info("支付回调，支付回调，更新订单 {}", tradeNo);
        // 更新订单为已支付
        orderService.changeOrderPaySuccess(tradeNo,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(params.get("gmt_payment")));

        return "success";
    }

    @Override
    @RequestMapping(value = "group_buy_notify", method = RequestMethod.POST)
    public String groupBuyNotify(@RequestBody NotifyRequestDTO requestDTO){
        log.info("拼团回调，组队完成，结算开始 {}", JSON.toJSONString(requestDTO));

        try{
            // 营销结算
            List<String> outTradeNoList = requestDTO.getOutTradeNoList();
            orderService.changeOrderMarketSettlement(outTradeNoList);
            return "success";
        }catch (Exception e){
            log.info("拼团回调，组队完成，结算失败 {}", JSON.toJSONString(requestDTO));
            return "error";
        }
    }

}
