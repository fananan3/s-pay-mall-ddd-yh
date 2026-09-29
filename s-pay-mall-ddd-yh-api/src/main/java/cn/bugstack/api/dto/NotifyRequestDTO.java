package cn.bugstack.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Description 回调请求对象
 * @Author fanyiheng
 * @Date 2026/9/28 21:28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotifyRequestDTO {
    /** 组队ID */
    private String teamId;
    /** 外部单号 */
    private List<String> outTradeNoList;
}
