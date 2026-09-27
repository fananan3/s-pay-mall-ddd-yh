package cn.bugstack.infrastructure.gateway.dto;

import lombok.Data;

/**
 * @Description 获取微信登录二维码响应对象
 * @Author fanyiheng
 * @Date 2026/9/26 15:49
 */
@Data
public class WeixinQrCodeResponseDTO {
    private String ticket;
    private Long expire_seconds;
    private String url;
}
