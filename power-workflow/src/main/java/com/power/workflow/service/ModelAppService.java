package com.power.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.common.model.PageResult;
import com.power.workflow.domain.WfModel;
import com.power.workflow.dto.ModelSaveRequest;
import com.power.workflow.dto.ModelVO;
import com.power.workflow.dto.ProcessDefinitionVO;
import com.power.workflow.mapper.WfModelMapper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程模型草稿（bpmn-js 保存）与部署。
 */
@Service
@RequiredArgsConstructor
public class ModelAppService {

    private final WfModelMapper modelMapper;
    private final RepositoryService repositoryService;

    /**
     * 新建或按 modelKey 覆盖保存草稿。
     */
    @Transactional(rollbackFor = Exception.class)
    public ModelVO save(ModelSaveRequest request) {
        String key = request.getModelKey().trim();
        WfModel entity = modelMapper.selectOne(new LambdaQueryWrapper<WfModel>()
                .eq(WfModel::getModelKey, key)
                .last("limit 1"));
        if (entity == null) {
            entity = new WfModel();
            entity.setModelKey(key);
            entity.setVersion(1);
        } else {
            entity.setVersion((entity.getVersion() == null ? 1 : entity.getVersion()) + 1);
        }
        entity.setName(request.getName().trim());
        entity.setCategoryCode(StringUtils.hasText(request.getCategoryCode()) ? request.getCategoryCode().trim() : null);
        entity.setBpmnXml(request.getBpmnXml());
        entity.setRemark(request.getRemark());
        if (entity.getId() == null) {
            modelMapper.insert(entity);
        } else {
            modelMapper.updateById(entity);
        }
        return toVo(entity);
    }

    /**
     * 模型详情（含 XML）。
     */
    public ModelVO detail(Long id) {
        WfModel entity = requireModel(id);
        return toVo(entity);
    }

    /**
     * 模型分页列表（默认不返回超长 XML，详情接口再取）。
     */
    public PageResult<ModelVO> list(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<WfModel> qw = new LambdaQueryWrapper<WfModel>()
                .orderByDesc(WfModel::getUpdateTime);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(WfModel::getModelKey, keyword).or().like(WfModel::getName, keyword));
        }
        Page<WfModel> page = modelMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        List<ModelVO> records = page.getRecords().stream().map(this::toListVo).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 删除模型草稿。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireModel(id);
        modelMapper.deleteById(id);
    }

    /**
     * 将草稿部署到 Flowable。
     */
    @Transactional(rollbackFor = Exception.class)
    public ProcessDefinitionVO deploy(Long id) {
        WfModel model = requireModel(id);
        if (!StringUtils.hasText(model.getBpmnXml())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "模型 XML 为空");
        }
        String resourceName = model.getModelKey() + ".bpmn20.xml";
        Deployment deployment = repositoryService.createDeployment()
                .name(model.getName())
                .category(model.getCategoryCode())
                .addString(resourceName, model.getBpmnXml())
                .deploy();
        ProcessDefinition def = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        if (def == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "部署成功但未解析到流程定义，请检查 BPMN");
        }
        if (StringUtils.hasText(model.getCategoryCode())) {
            repositoryService.setProcessDefinitionCategory(def.getId(), model.getCategoryCode());
            def = repositoryService.createProcessDefinitionQuery().processDefinitionId(def.getId()).singleResult();
        }
        return ProcessDefinitionVO.builder()
                .id(def.getId())
                .key(def.getKey())
                .name(def.getName())
                .version(def.getVersion())
                .deploymentId(def.getDeploymentId())
                .category(def.getCategory())
                .suspended(def.isSuspended())
                .deploymentTime(deployment.getDeploymentTime())
                .build();
    }

    private WfModel requireModel(Long id) {
        WfModel entity = modelMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模型不存在");
        }
        return entity;
    }

    private ModelVO toVo(WfModel entity) {
        return ModelVO.builder()
                .id(entity.getId())
                .modelKey(entity.getModelKey())
                .name(entity.getName())
                .categoryCode(entity.getCategoryCode())
                .bpmnXml(entity.getBpmnXml())
                .version(entity.getVersion())
                .remark(entity.getRemark())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    private ModelVO toListVo(WfModel entity) {
        return ModelVO.builder()
                .id(entity.getId())
                .modelKey(entity.getModelKey())
                .name(entity.getName())
                .categoryCode(entity.getCategoryCode())
                .version(entity.getVersion())
                .remark(entity.getRemark())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
