package com.power.middleware.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.power.middleware.security.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        String username = SecurityUtils.currentUsername();
        if (username == null) {
            username = "system";
        }
        strictInsertFill(metaObject, "createBy", String.class, username);
        strictInsertFill(metaObject, "updateBy", String.class, username);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        String username = SecurityUtils.currentUsername();
        if (username == null) {
            username = "system";
        }
        strictUpdateFill(metaObject, "updateBy", String.class, username);
    }
}
