package cn.bugstack.api;

import cn.bugstack.api.dto.*;
import cn.bugstack.api.response.Response;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/27 21:54
 */
public interface IPayService {
    Response<CreatePayOrderResponseDTO> createPayOrder(CreatePayRequestDTO createPayRequestDTO);

    String groupBuyNotify(NotifyRequestDTO requestDTO);

    Response<QueryOrderListResponseDTO> queryUserOrderList(QueryOrderListRequestDTO requestDTO);

    Response<RefundOrderResponseDTO> refundOrder(RefundOrderRequestDTO requestDTO);

    Response<String> activePayNotify(String outTradeNo);


}
