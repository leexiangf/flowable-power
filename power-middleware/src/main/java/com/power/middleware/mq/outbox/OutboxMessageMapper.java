package com.power.middleware.mq.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxMessageMapper extends BaseMapper<OutboxMessage> {
}
