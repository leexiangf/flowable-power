package com.power.common.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 统一分页查询入参。
 */
@Data
public class PageQuery {

    /** 页码，从 1 开始 */
    @Min(1)
    private long pageNum = 1;

    /** 每页条数，最大 500 */
    @Min(1)
    @Max(500)
    private long pageSize = 10;
}
