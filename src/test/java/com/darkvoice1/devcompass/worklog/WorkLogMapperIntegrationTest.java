package com.darkvoice1.devcompass.worklog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

    /**
     * 验证日志日期范围查询只返回范围内的记录。
     */
    @Test
    void shouldQueryWorkLogsWithinDateRange() {
        Project project = new Project();
        project.setName("日期范围查询项目");
        projectMapper.insert(project);

        ProjectPhase phase = new ProjectPhase();
        phase.setProjectId(project.getId());
        phase.setName("日志查询阶段");
        projectPhaseMapper.insert(phase);

        WorkLog beforeRange = createWorkLog(project, phase, "范围前任务", LocalDate.of(2026, 10, 1));
        WorkLog withinRange = createWorkLog(project, phase, "范围内任务", LocalDate.of(2026, 10, 2));
        WorkLog afterRange = createWorkLog(project, phase, "范围后任务", LocalDate.of(2026, 10, 3));

        List<WorkLog> logs = workLogMapper.selectList(new QueryWrapper<WorkLog>()
                .ge("log_date", LocalDate.of(2026, 10, 2))
                .le("log_date", LocalDate.of(2026, 10, 2))
                .orderByDesc("log_date"));

        assertThat(logs).extracting(WorkLog::getId).containsExactly(withinRange.getId());
        assertThat(logs).extracting(WorkLog::getId)
                .doesNotContain(beforeRange.getId(), afterRange.getId());
    }

    /**
     * 创建指定日期的测试工作日志。
     */
    private WorkLog createWorkLog(Project project, ProjectPhase phase, String title, LocalDate logDate) {
        Task task = new Task();
        task.setProjectId(project.getId());
        task.setPhaseId(phase.getId());
        task.setTitle(title);
        taskMapper.insert(task);

        WorkLog workLog = new WorkLog();
        workLog.setTaskId(task.getId());
        workLog.setLogDate(logDate);
        workLog.setSummaryContent(title + "已完成");
        workLog.setSpentMinutes(30);
        workLogMapper.insert(workLog);
        return workLog;
    }
}
