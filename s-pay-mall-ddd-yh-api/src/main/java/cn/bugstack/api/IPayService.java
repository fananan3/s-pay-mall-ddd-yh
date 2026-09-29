package cn.bugstack.api;

import cn.bugstack.api.dto.CreatePayRequestDTO;
import cn.bugstack.api.dto.NotifyRequestDTO;
import cn.bugstack.api.response.Response;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/27 21:54
 */
public interface IPayService {
    Response<String> createPayOrder(CreatePayRequestDTO createPayRequestDTO);

    String groupBuyNotify(NotifyRequestDTO requestDTO);
}
