package com.power.middleware.mq.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_outbox")
public class OutboxMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String topic;
    private String tag;
    private String payload;
    /** NEW / SENT / FAILED */
    private String status;
    private Integer retryCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
