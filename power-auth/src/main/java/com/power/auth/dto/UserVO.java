package com.power.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "用户详情")
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private Integer status;
    private String remark;
    private List<Long> roleIds;
    private List<String> roleCodes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
