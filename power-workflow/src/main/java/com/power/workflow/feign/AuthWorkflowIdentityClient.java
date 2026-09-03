package com.power.workflow.feign;

import com.power.common.result.R;
import com.power.workflow.dto.WorkflowUserView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 对接 power-auth 工作流身份查询。
 */
@FeignClient(name = "power-auth", url = "${power.feign.auth-url:}", path = "/auth/workflow")
public interface AuthWorkflowIdentityClient {

    @GetMapping("/users/{userId}/roles")
    R<List<String>> listRoleCodes(@PathVariable("userId") Long userId);

    @GetMapping("/users/{userId}")
    R<WorkflowUserView> getUser(@PathVariable("userId") Long userId);
}
