package com.power.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.common.model.PageResult;
import com.power.workflow.domain.WfCategory;
import com.power.workflow.dto.CategorySaveRequest;
import com.power.workflow.dto.CategoryVO;
import com.power.workflow.mapper.WfCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程分类管理。
 */
@Service
@RequiredArgsConstructor
public class CategoryAppService {

    private final WfCategoryMapper categoryMapper;

    /**
     * 新增分类。
     */
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO create(CategorySaveRequest request) {
        WfCategory exists = categoryMapper.selectOne(new LambdaQueryWrapper<WfCategory>()
                .eq(WfCategory::getCode, request.getCode().trim())
                .last("limit 1"));
        if (exists != null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "分类编码已存在");
        }
        WfCategory entity = new WfCategory();
        fill(entity, request);
        categoryMapper.insert(entity);
        return toVo(entity);
    }

    /**
     * 更新分类。
     */
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO update(Long id, CategorySaveRequest request) {
        WfCategory entity = categoryMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        WfCategory conflict = categoryMapper.selectOne(new LambdaQueryWrapper<WfCategory>()
                .eq(WfCategory::getCode, request.getCode().trim())
                .ne(WfCategory::getId, id)
                .last("limit 1"));
        if (conflict != null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "分类编码已存在");
        }
        fill(entity, request);
        categoryMapper.updateById(entity);
        return toVo(entity);
    }

    /**
     * 删除分类（逻辑删除）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (categoryMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        categoryMapper.deleteById(id);
    }

    /**
     * 分类详情。
     */
    public CategoryVO detail(Long id) {
        WfCategory entity = categoryMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        return toVo(entity);
    }

    /**
     * 分类分页列表。
     */
    public PageResult<CategoryVO> list(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<WfCategory> qw = new LambdaQueryWrapper<WfCategory>()
                .orderByAsc(WfCategory::getSort)
                .orderByDesc(WfCategory::getId);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(WfCategory::getCode, keyword).or().like(WfCategory::getName, keyword));
        }
        Page<WfCategory> page = categoryMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<CategoryVO> records = page.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 启用中的全部分类（下拉用）。
     */
    public List<CategoryVO> listEnabled() {
        return categoryMapper.selectList(new LambdaQueryWrapper<WfCategory>()
                        .eq(WfCategory::getStatus, 1)
                        .orderByAsc(WfCategory::getSort))
                .stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    private void fill(WfCategory entity, CategorySaveRequest request) {
        entity.setCode(request.getCode().trim());
        entity.setName(request.getName().trim());
        entity.setSort(request.getSort() == null ? 0 : request.getSort());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setRemark(request.getRemark());
    }

    private CategoryVO toVo(WfCategory entity) {
        return CategoryVO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .sort(entity.getSort())
                .status(entity.getStatus())
                .remark(entity.getRemark())
                .createTime(entity.getCreateTime())
                .build();
    }
}
