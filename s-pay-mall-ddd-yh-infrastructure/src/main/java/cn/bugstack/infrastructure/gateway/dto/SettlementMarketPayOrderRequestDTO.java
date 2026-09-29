package cn.bugstack.infrastructure.gateway.dto;

import lombok.Data;

import java.util.Date;

/**
 * @Description 结算请求对象
 * @Author fanyiheng
 * @Date 2026/9/23 23:20
 */
@Data
public class SettlementMarketPayOrderRequestDTO {
    /** 渠道 */
    private String source;
    /** 来源 */
    private String channel;
    /** 用户ID */
    private String userId;
    /** 外部交易单号 */
    private String outTradeNo;
    /** 外部交易时间 */
//    @DateTimeFormat
//    @JsonFormat
    private Date outTradeTime;
}
