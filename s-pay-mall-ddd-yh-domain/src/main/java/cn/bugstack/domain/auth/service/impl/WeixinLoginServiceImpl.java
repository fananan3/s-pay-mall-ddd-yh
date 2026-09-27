package cn.bugstack.domain.auth.service.impl;


import cn.bugstack.domain.auth.adapter.port.ILoginPort;
import cn.bugstack.domain.auth.service.ILoginService;
import com.google.common.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import javax.annotation.Resource;
import java.io.IOException;

/**
 * @Description 实现微信登陆接口
 * @Author fanyiheng
 * @Date 2026/9/26 15:32
 */

@Service
@Slf4j
public class WeixinLoginServiceImpl implements ILoginService {



    @Resource
    private Cache<String, String> openidToken;

    @Resource
    private ILoginPort loginPort;


    @Override
    public String createQrCodeTicket() throws Exception {
        return loginPort.createQrCodeTicket();
    }

    @Override
    public String checkLogin(String ticket) {
        return openidToken.getIfPresent(ticket);
    }

    @Override
    public void saveLoginState(String ticket, String openid) throws IOException {
        // 保存登录信息
        openidToken.put(ticket, openid);
        // 发送模板消息
        loginPort.sendLoginTemplate(openid);
    }
}
