package cn.bugstack.api.dto;

import lombok.Data;

/**
 * @Description
 * @Author fanyiheng
 * @Date 2026/10/7 21:59
 */
@Data
public class QueryOrderListRequestDTO {

    /** 用户ID */
    private String userId;
    /** 分页参数：大于此ID的记录 */
    private Long lastId;
    /** 每页数量 */
    private Integer pageSize = 10;
}
