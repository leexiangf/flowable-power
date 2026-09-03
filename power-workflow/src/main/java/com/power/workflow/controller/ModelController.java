package com.power.workflow.controller;

import com.power.common.model.PageQuery;
import com.power.common.model.PageResult;
import com.power.common.result.R;
import com.power.workflow.dto.ModelSaveRequest;
import com.power.workflow.dto.ModelVO;
import com.power.workflow.dto.ProcessDefinitionVO;
import com.power.workflow.service.ModelAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程模型草稿接口（供 bpmn-js 保存与部署）。
 */
@Tag(name = "流程模型", description = "BPMN 模型草稿保存与部署")
@RestController
@RequestMapping("/workflow/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelAppService modelAppService;

    @Operation(summary = "保存模型草稿", description = "按 modelKey 新建或覆盖；权限码 workflow:model:edit")
    @PostMapping
    @PreAuthorize("@authz.permit('workflow:model:edit')")
    public R<ModelVO> save(@Valid @RequestBody ModelSaveRequest request) {
        return R.ok(modelAppService.save(request));
    }

    @Operation(summary = "模型详情", description = "含 BPMN XML；权限码 workflow:model:list")
    @GetMapping("/{id}")
    @PreAuthorize("@authz.permit('workflow:model:list')")
    public R<ModelVO> detail(@PathVariable Long id) {
        return R.ok(modelAppService.detail(id));
    }

    @Operation(summary = "模型分页", description = "列表不含 XML；权限码 workflow:model:list")
    @GetMapping
    @PreAuthorize("@authz.permit('workflow:model:list')")
    public R<PageResult<ModelVO>> list(@Valid PageQuery page,
                                       @RequestParam(required = false) String keyword) {
        return R.ok(modelAppService.list(page.getPageNum(), page.getPageSize(), keyword));
    }

    @Operation(summary = "删除模型", description = "权限码 workflow:model:edit")
    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.permit('workflow:model:edit')")
    public R<Void> delete(@PathVariable Long id) {
        modelAppService.delete(id);
        return R.ok();
    }

    @Operation(summary = "部署模型", description = "将草稿 BPMN 部署到引擎；权限码 workflow:definition:deploy")
    @PostMapping("/{id}/deploy")
    @PreAuthorize("@authz.permit('workflow:definition:deploy')")
    public R<ProcessDefinitionVO> deploy(@PathVariable Long id) {
        return R.ok(modelAppService.deploy(id));
    }
}
