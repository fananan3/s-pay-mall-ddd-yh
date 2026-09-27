package cn.bugstack.infrastructure.gateway.dto;

import lombok.Data;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/9/26 15:42
 */
@Data
public class WeixinTokenResponseDTO {
    private String access_token;
    private int expires_in;
    private String errcode;
    private String errmsg;
}
