package cn.bugstack.domain.auth.adapter.port;

import java.io.IOException;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/27 16:20
 */
public interface ILoginPort {
    String createQrCodeTicket() throws IOException;

    void sendLoginTemplate(String openid) throws IOException;
}
