package cn.bugstack.infrastructure.gateway;


import cn.bugstack.infrastructure.gateway.dto.LockMarketPayOrderRequestDTO;
import cn.bugstack.infrastructure.gateway.dto.LockMarketPayOrderResponseDTO;
import cn.bugstack.infrastructure.gateway.response.Response;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团营销
 * @create 2025-02-06 14:03
 */
public interface IGroupBuyMarketService {


    @POST("/api/v1/gbm/trade/lock_market_pay_order/")
    Call<Response<LockMarketPayOrderResponseDTO>> lockMarketPayOrder(@Body LockMarketPayOrderRequestDTO requestDTO);
}
