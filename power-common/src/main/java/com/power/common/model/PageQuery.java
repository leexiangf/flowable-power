package com.power.common.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageQuery {

    @Min(1)
    private long pageNum = 1;

    @Min(1)
    @Max(500)
    private long pageSize = 10;
}
