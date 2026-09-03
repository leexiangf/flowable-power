package com.power.workflow.controller;

import com.power.common.model.PageQuery;
import com.power.common.model.PageResult;
import com.power.common.result.R;
import com.power.workflow.dto.CategorySaveRequest;
import com.power.workflow.dto.CategoryVO;
import com.power.workflow.service.CategoryAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 流程分类接口。
 */
@Tag(name = "流程分类", description = "流程分类 CRUD")
@RestController
@RequestMapping("/workflow/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryAppService categoryAppService;

    @Operation(summary = "新增分类", description = "权限码 workflow:category:edit")
    @PostMapping
    @PreAuthorize("@authz.permit('workflow:category:edit')")
    public R<CategoryVO> create(@Valid @RequestBody CategorySaveRequest request) {
        return R.ok(categoryAppService.create(request));
    }

    @Operation(summary = "更新分类", description = "权限码 workflow:category:edit")
    @PutMapping("/{id}")
    @PreAuthorize("@authz.permit('workflow:category:edit')")
    public R<CategoryVO> update(@PathVariable Long id, @Valid @RequestBody CategorySaveRequest request) {
        return R.ok(categoryAppService.update(id, request));
    }

    @Operation(summary = "删除分类", description = "权限码 workflow:category:edit")
    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.permit('workflow:category:edit')")
    public R<Void> delete(@PathVariable Long id) {
        categoryAppService.delete(id);
        return R.ok();
    }

    @Operation(summary = "分类分页", description = "权限码 workflow:category:list")
    @GetMapping
    @PreAuthorize("@authz.permit('workflow:category:list')")
    public R<PageResult<CategoryVO>> list(@Valid PageQuery page,
                                          @RequestParam(required = false) String keyword) {
        return R.ok(categoryAppService.list(page.getPageNum(), page.getPageSize(), keyword));
    }

    @Operation(summary = "启用中分类列表", description = "下拉选择；权限码 workflow:category:list")
    @GetMapping("/enabled")
    @PreAuthorize("@authz.permit('workflow:category:list')")
    public R<List<CategoryVO>> listEnabled() {
        return R.ok(categoryAppService.listEnabled());
    }

    @Operation(summary = "分类详情", description = "权限码 workflow:category:list")
    @GetMapping("/{id}")
    @PreAuthorize("@authz.permit('workflow:category:list')")
    public R<CategoryVO> detail(@PathVariable Long id) {
        return R.ok(categoryAppService.detail(id));
    }
}
