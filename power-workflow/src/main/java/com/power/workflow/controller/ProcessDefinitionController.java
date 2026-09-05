package com.power.workflow.controller;

import com.power.common.model.PageQuery;
import com.power.common.model.PageResult;
import com.power.common.result.R;
import com.power.workflow.dto.ProcessDefinitionVO;
import com.power.workflow.service.ProcessDefinitionAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 流程定义管理接口。
 */
@Tag(name = "流程定义", description = "BPMN 部署、定义列表、挂起/激活")
@RestController
@RequestMapping("/workflow/definitions")
@RequiredArgsConstructor
public class ProcessDefinitionController {

    private final ProcessDefinitionAppService processDefinitionAppService;

    /**
     * 上传并部署 BPMN。
     *
     * @param file BPMN 文件
     * @return 流程定义视图
     */
    @Operation(summary = "上传部署 BPMN", description = "上传 .bpmn / .bpmn20.xml，部署后返回最新流程定义。权限码 workflow:definition:deploy。")
    @PostMapping(value = "/deploy", consumes = "multipart/form-data")
    @PreAuthorize("@authz.permit('workflow:definition:deploy')")
    public R<ProcessDefinitionVO> deploy(
            @Parameter(description = "BPMN 文件", required = true) @RequestParam("file") MultipartFile file) {
        return R.ok(processDefinitionAppService.deploy(file));
    }

    /**
     * 分页查询最新流程定义。
     *
     * @param page      分页参数
     * @param suspended 挂起筛选
     * @param category  分类编码
     * @return 分页结果
     */
    @Operation(summary = "流程定义列表", description = "按 key 取最新版本分页。suspended/category 可筛选。权限码 workflow:definition:list。")
    @GetMapping
    @PreAuthorize("@authz.permit('workflow:definition:list')")
    public R<PageResult<ProcessDefinitionVO>> list(@Valid PageQuery page,
                                                   @Parameter(description = "是否挂起：true/false，空表示全部")
                                                   @RequestParam(required = false) Boolean suspended,
                                                   @Parameter(description = "分类编码")
                                                   @RequestParam(required = false) String category) {
        return R.ok(processDefinitionAppService.listLatest(
                page.getPageNum(), page.getPageSize(), suspended, category));
    }

    /**
     * 可发起流程定义列表。
     *
     * @param page 分页参数
     * @return 分页结果
     */
    @Operation(summary = "可发起流程列表", description = "未挂起的最新定义，供发起页选择。权限码 workflow:instance:start。")
    @GetMapping("/startable")
    @PreAuthorize("@authz.permit('workflow:instance:start')")
    public R<PageResult<ProcessDefinitionVO>> startable(@Valid PageQuery page) {
        return R.ok(processDefinitionAppService.listStartable(page.getPageNum(), page.getPageSize()));
    }

    /**
     * 挂起流程定义。
     *
     * @param processDefinitionId 流程定义 ID
     * @return 空成功响应
     */
    @Operation(summary = "挂起流程定义", description = "挂起后不可再启动新实例。权限码 workflow:definition:suspend。")
    @PostMapping("/{processDefinitionId}/suspend")
    @PreAuthorize("@authz.permit('workflow:definition:suspend')")
    public R<Void> suspend(@PathVariable String processDefinitionId) {
        processDefinitionAppService.suspend(processDefinitionId);
        return R.ok();
    }

    /**
     * 激活流程定义。
     *
     * @param processDefinitionId 流程定义 ID
     * @return 空成功响应
     */
    @Operation(summary = "激活流程定义", description = "取消挂起。权限码 workflow:definition:suspend。")
    @PostMapping("/{processDefinitionId}/activate")
    @PreAuthorize("@authz.permit('workflow:definition:suspend')")
    public R<Void> activate(@PathVariable String processDefinitionId) {
        processDefinitionAppService.activate(processDefinitionId);
        return R.ok();
    }

    /**
     * 获取流程定义 BPMN XML。
     *
     * @param processDefinitionId 流程定义 ID
     * @return BPMN XML 文本
     */
    @Operation(summary = "获取 BPMN XML", description = "返回指定定义的 BPMN 文本。权限码 workflow:definition:list。")
    @GetMapping("/{processDefinitionId}/xml")
    @PreAuthorize("@authz.permit('workflow:definition:list')")
    public R<String> xml(@PathVariable String processDefinitionId) {
        return R.ok(processDefinitionAppService.getBpmnXml(processDefinitionId));
    }

    @Operation(summary = "下载 BPMN XML", description = "attachment 下载。权限码 workflow:definition:list。")
    @GetMapping("/{processDefinitionId}/xml/download")
    @PreAuthorize("@authz.permit('workflow:definition:list')")
    public ResponseEntity<byte[]> downloadXml(@PathVariable String processDefinitionId) {
        String xml = processDefinitionAppService.getBpmnXml(processDefinitionId);
        String filename = processDefinitionAppService.resolveBpmnFilename(processDefinitionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Operation(summary = "删除部署", description = "cascade=true 级联删除实例；权限码 workflow:definition:remove")
    @DeleteMapping("/deployments/{deploymentId}")
    @PreAuthorize("@authz.permit('workflow:definition:remove')")
    public R<Void> deleteDeployment(@PathVariable String deploymentId,
                                    @RequestParam(defaultValue = "false") boolean cascade) {
        processDefinitionAppService.deleteDeployment(deploymentId, cascade);
        return R.ok();
    }
}
