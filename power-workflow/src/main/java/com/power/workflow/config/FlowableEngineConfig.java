package com.power.workflow.config;

import com.power.workflow.constant.ProcessKeys;
import com.power.workflow.event.ProcessLifecycleOutboxListener;
import com.power.workflow.service.ProcessDefinitionAppService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Flowable Process 引擎扩展点。
 * <p>
 * 身份以 power-auth RBAC 为准（assignee=userId，candidateGroups=roleCode），不同步 ACT_ID_*。
 */
@Slf4j
@Configuration
public class FlowableEngineConfig {

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> processEngineConfigurer() {
        return conf -> {
            conf.setActivityFontName("SansSerif");
            conf.setLabelFontName("SansSerif");
            conf.setAnnotationFontName("SansSerif");
            conf.setEnableEventDispatcher(true);
        };
    }

    @Bean
    public ApplicationRunner builtinProcessBootstrap(
            RepositoryService repositoryService,
            ProcessDefinitionAppService processDefinitionAppService) {
        return (ApplicationArguments args) -> {
            bootstrapIfAbsent(repositoryService, processDefinitionAppService,
                    ProcessKeys.LEAVE, "processes/leave.bpmn20.xml", "leave.bpmn20.xml");
            bootstrapIfAbsent(repositoryService, processDefinitionAppService,
                    ProcessKeys.EXPENSE, "processes/expense.bpmn20.xml", "expense.bpmn20.xml");
            bootstrapIfAbsent(repositoryService, processDefinitionAppService,
                    ProcessKeys.COUNTERSIGN_SEQ, "processes/countersign-seq.bpmn20.xml", "countersign-seq.bpmn20.xml");
            // 或签完成条件已调整：强制升版，避免旧定义无「一票否决提前结束」
            refreshBuiltin(repositoryService, processDefinitionAppService,
                    ProcessKeys.COUNTERSIGN_OR, "processes/countersign-or.bpmn20.xml", "countersign-or.bpmn20.xml");
        };
    }

    /**
     * 引擎就绪后注册生命周期 Outbox 监听，避免与 HistoryService 循环依赖。
     */
    @Bean
    public ApplicationRunner registerProcessLifecycleOutboxListener(
            ProcessEngine processEngine,
            ProcessLifecycleOutboxListener listener) {
        return args -> {
            processEngine.getProcessEngineConfiguration()
                    .getEventDispatcher()
                    .addEventListener(
                            listener,
                            FlowableEngineEventType.PROCESS_COMPLETED,
                            FlowableEngineEventType.PROCESS_CANCELLED);
            log.info("Registered ProcessLifecycleOutboxListener");
        };
    }

    private static void bootstrapIfAbsent(
            RepositoryService repositoryService,
            ProcessDefinitionAppService processDefinitionAppService,
            String key,
            String classpath,
            String resourceName) {
        long count = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(key)
                .count();
        if (count > 0) {
            log.info("{} process already deployed, skip bootstrap", key);
            return;
        }
        processDefinitionAppService.deployClasspathResource(classpath, resourceName);
        log.info("Bootstrapped {} process definition", key);
    }

    /**
     * 已部署但缺少关键完成条件时升版部署一次（避免每次启动都打新版本）。
     */
    private static void refreshBuiltin(
            RepositoryService repositoryService,
            ProcessDefinitionAppService processDefinitionAppService,
            String key,
            String classpath,
            String resourceName) {
        ProcessDefinition latest = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(key)
                .latestVersion()
                .singleResult();
        if (latest == null) {
            processDefinitionAppService.deployClasspathResource(classpath, resourceName);
            log.info("Bootstrapped {} process definition", key);
            return;
        }
        try (InputStream in = repositoryService.getResourceAsStream(latest.getDeploymentId(), resourceName)) {
            if (in == null) {
                processDefinitionAppService.deployClasspathResource(classpath, resourceName);
                log.info("Re-deployed {} (resource missing in deployment)", key);
                return;
            }
            String xml = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            if (xml.contains("approved == false")) {
                log.info("{} process already has reject early-exit, skip refresh", key);
                return;
            }
        } catch (Exception ex) {
            log.warn("Check {} BPMN failed: {}, will re-deploy", key, ex.getMessage());
        }
        processDefinitionAppService.deployClasspathResource(classpath, resourceName);
        log.info("Refreshed {} process definition for reject early-exit", key);
    }
}
