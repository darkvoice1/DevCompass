package com.darkvoice1.devcompass.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.darkvoice1.devcompass.Application;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectPhase;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.repository.ProjectPhaseMapper;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.repository.TaskMapper;

/**
 * 验证任务实体的审计字段映射。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class TaskMapperIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectPhaseMapper projectPhaseMapper;

    @Autowired
    private TaskMapper taskMapper;

    /**
     * 将测试容器连接信息注入 Spring 数据源配置。
     */
    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /**
     * 验证新建任务的审计字段可以正确映射。
     */
    @Test
    void shouldMapTaskAuditFields() {
        Project project = new Project();
        project.setName("研发罗盘");
        projectMapper.insert(project);

        ProjectPhase phase = new ProjectPhase();
        phase.setProjectId(project.getId());
        phase.setName("开发实现");
        projectPhaseMapper.insert(phase);

        Task task = new Task();
        task.setProjectId(project.getId());
        task.setPhaseId(phase.getId());
        task.setTitle("实现任务阶段关联");
        taskMapper.insert(task);

        Task stored = taskMapper.selectById(task.getId());
        assertThat(stored).isNotNull();
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getUpdatedAt()).isNotNull();
        assertThat(stored.getDeletedAt()).isNull();
    }

    /**
     * 验证任务软删除后默认查询会过滤记录，恢复后可再次查询。
     */
    @Test
    void shouldSoftDeleteAndRestoreTask() {
        Project project = new Project();
        project.setName("研发罗盘");
        projectMapper.insert(project);

        ProjectPhase phase = new ProjectPhase();
        phase.setProjectId(project.getId());
        phase.setName("开发实现");
        projectPhaseMapper.insert(phase);

        Task task = new Task();
        task.setProjectId(project.getId());
        task.setPhaseId(phase.getId());
        task.setTitle("待删除任务");
        taskMapper.insert(task);

        assertThat(taskMapper.softDeleteById(task.getId())).isEqualTo(1);
        assertThat(taskMapper.selectById(task.getId())).isNull();
        assertThat(taskMapper.selectDeletedById(task.getId())).isNotNull();

        assertThat(taskMapper.restoreById(task.getId())).isEqualTo(1);
        assertThat(taskMapper.selectById(task.getId())).isNotNull();
    }
}
