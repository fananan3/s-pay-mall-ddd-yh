package cn.bugstack.domain.auth.service;

import java.io.IOException;

/**
 * @Description  微信服务
 * @Author fanyiheng
 * @Date 2026/9/26 15:28
 */
public interface ILoginService {

    String createQrCodeTicket() throws Exception;

    String checkLogin(String ticket);

    void saveLoginState(String ticket,String openid)throws IOException;
}
