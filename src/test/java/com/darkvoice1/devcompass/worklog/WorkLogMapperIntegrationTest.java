package com.darkvoice1.devcompass.worklog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
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
import com.darkvoice1.devcompass.worklog.entity.WorkLog;
import com.darkvoice1.devcompass.worklog.repository.WorkLogMapper;

/**
 * 验证工作日志与任务的一对一持久化映射。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class WorkLogMapperIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectPhaseMapper projectPhaseMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private WorkLogMapper workLogMapper;

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
     * 验证工作日志可绑定任务，且同一任务不能重复关联日志。
     */
    @Test
    void shouldPersistWorkLogAndAssociations() {
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
        task.setTitle("创建工作日志数据模型");
        taskMapper.insert(task);

        WorkLog workLog = new WorkLog();
        workLog.setTaskId(task.getId());
        workLog.setLogDate(LocalDate.of(2026, 9, 16));
        workLog.setPlanContent("完成数据库迁移");
        workLog.setSummaryContent("已完成数据模型设计");
        workLog.setSpentMinutes(90);
        workLog.setBlockerReason("暂无阻塞");
        workLogMapper.insert(workLog);

        WorkLog storedWorkLog = workLogMapper.selectById(workLog.getId());
        assertThat(storedWorkLog).isNotNull();
        assertThat(storedWorkLog.getTaskId()).isEqualTo(task.getId());
        assertThat(storedWorkLog.getLogDate()).isEqualTo(LocalDate.of(2026, 9, 16));
        assertThat(storedWorkLog.getSpentMinutes()).isEqualTo(90);
        assertThat(storedWorkLog.getCreatedAt()).isNotNull();
        assertThat(storedWorkLog.getUpdatedAt()).isNotNull();

        WorkLog duplicateWorkLog = new WorkLog();
        duplicateWorkLog.setTaskId(task.getId());
        duplicateWorkLog.setLogDate(LocalDate.of(2026, 9, 17));
        assertThatThrownBy(() -> workLogMapper.insert(duplicateWorkLog))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
