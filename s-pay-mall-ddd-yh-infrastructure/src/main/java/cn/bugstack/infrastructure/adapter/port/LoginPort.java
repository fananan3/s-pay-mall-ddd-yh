package cn.bugstack.infrastructure.adapter.port;

import cn.bugstack.domain.auth.adapter.port.ILoginPort;
import cn.bugstack.infrastructure.gateway.IWeixinApiService;
import cn.bugstack.infrastructure.gateway.dto.WeixinQrCodeRequestDTO;
import cn.bugstack.infrastructure.gateway.dto.WeixinQrCodeResponseDTO;
import cn.bugstack.infrastructure.gateway.dto.WeixinTemplateMessageDTO;
import cn.bugstack.infrastructure.gateway.dto.WeixinTokenResponseDTO;
import com.google.common.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Call;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/27 16:26
 */

@Slf4j
@Service
public class LoginPort implements ILoginPort {

    @Value("${weixin.config.app-id}")
    private String appid;
    @Value("${weixin.config.app-secret}")
    private String appSecret;
    @Value("${weixin.config.template_id}")
    private String template_id;
    @Resource
    private Cache<String, String> weixinAccessToken;
    @Resource
    private IWeixinApiService weixinApiService;

    @Override
    public String createQrCodeTicket() throws IOException {
        // 1 获取accessToken
        String accessToken = weixinAccessToken.getIfPresent(appid);
        if (accessToken == null){
            Call<WeixinTokenResponseDTO> call = weixinApiService.getToken("client_credential",appid,appSecret);
            WeixinTokenResponseDTO weixinTokenRes = call.execute().body();
            assert weixinTokenRes != null;
            accessToken = weixinTokenRes.getAccess_token();
            weixinAccessToken.put(appid,accessToken);
        }

        //2. 生成ticket
        WeixinQrCodeRequestDTO weixinQrCodeReq = WeixinQrCodeRequestDTO.builder()
                .expire_seconds(2592000)
                .action_name(WeixinQrCodeRequestDTO.ActionNameTypeVO.QR_SCENE.getCode())
                .action_info(WeixinQrCodeRequestDTO.ActionInfo.builder()
                        .scene(WeixinQrCodeRequestDTO.ActionInfo.Scene.builder()
                                .scene_id(100601)
                                .build())
                        .build())
                .build();
        Call<WeixinQrCodeResponseDTO> call = weixinApiService.createQrCode(accessToken,weixinQrCodeReq);
        WeixinQrCodeResponseDTO weixinQrCodeRes = call.execute().body();
        assert null != weixinQrCodeRes;
        return weixinQrCodeRes.getTicket();
    }

    @Override
    public void sendLoginTemplate(String openid) throws IOException {
        //1. 获取accessToken
        String accessToken = weixinAccessToken.getIfPresent(appid);
        if (null == accessToken){
            Call<WeixinTokenResponseDTO> call = weixinApiService.getToken("client_credential", appid, appSecret);
            WeixinTokenResponseDTO weixinTokenRes = call.execute().body();
            assert weixinTokenRes != null;
            accessToken = weixinTokenRes.getAccess_token();
            weixinAccessToken.put(appid, accessToken);
        }
        // 2. 发送模板消息
        Map<String, Map<String,String>> data = new HashMap<>();
        WeixinTemplateMessageDTO.put(data,WeixinTemplateMessageDTO.TemplateKey.USER,openid);
        WeixinTemplateMessageDTO templateMessageDTO = new WeixinTemplateMessageDTO(openid,template_id);
        templateMessageDTO.setUrl("https://gaga.plus");
        templateMessageDTO.setData(data);
        Call<Void> call = weixinApiService.sendMessage(accessToken, templateMessageDTO);
        call.execute();
    }
}
