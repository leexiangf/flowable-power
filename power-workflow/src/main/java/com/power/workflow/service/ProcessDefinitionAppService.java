package com.power.workflow.service;

import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.common.model.PageResult;
import com.power.workflow.dto.ProcessDefinitionVO;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程定义：部署、列表、挂起/激活、读取 BPMN。
 */
@Service
@RequiredArgsConstructor
public class ProcessDefinitionAppService {

    private final RepositoryService repositoryService;

    /**
     * 上传并部署 BPMN 文件。
     *
     * @param file BPMN 文件（.bpmn / .bpmn20.xml）
     * @return 部署后的流程定义视图
     */
    public ProcessDefinitionVO deploy(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请上传 BPMN 文件");
        }
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename) || !(filename.endsWith(".bpmn") || filename.endsWith(".bpmn20.xml") || filename.endsWith(".xml"))) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅支持 .bpmn / .bpmn20.xml");
        }
        try (InputStream in = file.getInputStream()) {
            Deployment deployment = repositoryService.createDeployment()
                    .name(filename)
                    .addInputStream(filename, in)
                    .deploy();
            ProcessDefinition def = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();
            if (def == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "部署成功但未解析到流程定义，请检查 BPMN");
            }
            return toVo(def, deployment);
        } catch (BizException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "读取上传文件失败");
        } catch (Exception ex) {
            throw new BizException(ErrorCode.BAD_REQUEST, "BPMN 部署失败: " + ex.getMessage());
        }
    }

    /**
     * 从 classpath 部署流程资源（用于内置 leave 等启动引导）。
     *
     * @param resourcePath classpath 相对路径
     * @param name         部署资源名
     * @return 流程定义视图
     */
    public ProcessDefinitionVO deployClasspathResource(String resourcePath, String name) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "找不到流程资源: " + resourcePath);
            }
            Deployment deployment = repositoryService.createDeployment()
                    .name(name)
                    .addInputStream(name, in)
                    .deploy();
            ProcessDefinition def = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();
            return toVo(def, deployment);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "classpath 部署失败: " + ex.getMessage());
        }
    }

    /**
     * 分页查询各 key 最新版本流程定义。
     *
     * @param pageNum   页码
     * @param pageSize  每页条数
     * @param suspended 挂起筛选：true/false/null(全部)
     * @param category  分类编码，可空
     * @return 分页结果
     */
    public PageResult<ProcessDefinitionVO> listLatest(long pageNum, long pageSize, Boolean suspended, String category) {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().latestVersion();
        if (Boolean.TRUE.equals(suspended)) {
            query.suspended();
        } else if (Boolean.FALSE.equals(suspended)) {
            query.active();
        }
        if (StringUtils.hasText(category)) {
            query.processDefinitionCategory(category.trim());
        }
        long total = query.count();
        List<ProcessDefinition> list = query.orderByProcessDefinitionKey().asc()
                .listPage((int) ((pageNum - 1) * pageSize), (int) pageSize);
        List<ProcessDefinitionVO> records = list.stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, total, pageNum, pageSize);
    }

    /**
     * 分页查询各 key 最新版本流程定义。
     */
    public PageResult<ProcessDefinitionVO> listLatest(long pageNum, long pageSize, Boolean suspended) {
        return listLatest(pageNum, pageSize, suspended, null);
    }

    /**
     * 可发起流程列表（最新且未挂起）。
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageResult<ProcessDefinitionVO> listStartable(long pageNum, long pageSize) {
        return listLatest(pageNum, pageSize, false);
    }

    /**
     * 挂起流程定义（不可再启动新实例）。
     *
     * @param processDefinitionId 流程定义 ID
     */
    public void suspend(String processDefinitionId) {
        ProcessDefinition def = requireDefinition(processDefinitionId);
        if (def.isSuspended()) {
            return;
        }
        repositoryService.suspendProcessDefinitionById(processDefinitionId);
    }

    /**
     * 激活已挂起的流程定义。
     *
     * @param processDefinitionId 流程定义 ID
     */
    public void activate(String processDefinitionId) {
        ProcessDefinition def = requireDefinition(processDefinitionId);
        if (!def.isSuspended()) {
            return;
        }
        repositoryService.activateProcessDefinitionById(processDefinitionId);
    }

    /**
     * 读取流程定义对应的 BPMN XML 文本。
     *
     * @param processDefinitionId 流程定义 ID
     * @return BPMN XML
     */
    public String getBpmnXml(String processDefinitionId) {
        ProcessDefinition def = requireDefinition(processDefinitionId);
        try (InputStream in = repositoryService.getResourceAsStream(def.getDeploymentId(), def.getResourceName())) {
            if (in == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "BPMN 资源不存在");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "读取 BPMN 失败");
        }
    }

    /**
     * 按 ID 获取流程定义，不存在则抛业务异常。
     *
     * @param processDefinitionId 流程定义 ID
     * @return Flowable 流程定义
     */
    public ProcessDefinition requireDefinition(String processDefinitionId) {
        ProcessDefinition def = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        if (def == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "流程定义不存在");
        }
        return def;
    }

    /**
     * 按 key 获取最新且未挂起的流程定义。
     *
     * @param key 流程定义 key
     * @return Flowable 流程定义
     */
    public ProcessDefinition requireLatestByKey(String key) {
        ProcessDefinition def = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(key)
                .latestVersion()
                .active()
                .singleResult();
        if (def == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "流程定义不存在或已挂起: " + key);
        }
        return def;
    }

    private ProcessDefinitionVO toVo(ProcessDefinition def) {
        Deployment deployment = repositoryService.createDeploymentQuery()
                .deploymentId(def.getDeploymentId())
                .singleResult();
        return toVo(def, deployment);
    }

    private ProcessDefinitionVO toVo(ProcessDefinition def, Deployment deployment) {
        return ProcessDefinitionVO.builder()
                .id(def.getId())
                .key(def.getKey())
                .name(def.getName())
                .version(def.getVersion())
                .deploymentId(def.getDeploymentId())
                .category(def.getCategory())
                .suspended(def.isSuspended())
                .deploymentTime(deployment == null ? null : deployment.getDeploymentTime())
                .build();
    }
}
