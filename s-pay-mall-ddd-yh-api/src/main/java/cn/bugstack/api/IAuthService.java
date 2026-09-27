package cn.bugstack.api;

import cn.bugstack.api.response.Response;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/27 16:58
 */
public interface IAuthService {

     Response<String> weixinQrCodeTicket();
     Response<String> checkLogin(String ticket);
}
