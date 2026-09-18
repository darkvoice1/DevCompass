package com.darkvoice1.devcompass.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.darkvoice1.devcompass.Application;
import com.darkvoice1.devcompass.dashboard.dto.DashboardProjectRow;
import com.darkvoice1.devcompass.dashboard.service.DashboardService;
import com.darkvoice1.devcompass.project.entity.ProgressMode;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectPhase;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.repository.ProjectPhaseMapper;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.task.repository.TaskMapper;
import com.darkvoice1.devcompass.worklog.entity.WorkLog;
import com.darkvoice1.devcompass.worklog.repository.WorkLogMapper;

/**
 * 验证仪表盘项目筛选和最近活跃时间聚合查询。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class DashboardMapperIntegrationTest {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
     * 验证状态、完整标签和工作日志活跃时间可以组合筛选项目。
     */
    @Test
    void shouldFilterProjectsByStatusTagAndRecentActivity() {
        Project project = createProject("研发罗盘", ProjectStatus.IN_PROGRESS, "后端, 学习项目");
        ProjectPhase phase = createPhase(project.getId());
        Task task = createTask(project.getId(), phase.getId());
        WorkLog workLog = createWorkLog(task.getId());

        Instant oldUpdatedAt = Instant.now().minus(60, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant recentUpdatedAt = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        jdbcTemplate.update("UPDATE project SET updated_at = ? WHERE id = ?",
                Timestamp.from(oldUpdatedAt), project.getId());
        jdbcTemplate.update("UPDATE task SET updated_at = ? WHERE id = ?",
                Timestamp.from(oldUpdatedAt), task.getId());
        jdbcTemplate.update("UPDATE work_log SET updated_at = ? WHERE id = ?",
                Timestamp.from(recentUpdatedAt), workLog.getId());

        List<DashboardProjectRow> matchingProjects = projectMapper.selectDashboardProjects(
                ProjectStatus.IN_PROGRESS, "后端", 30, DashboardService.DUE_SOON_DAYS);

        assertThat(matchingProjects).hasSize(1);
        assertThat(matchingProjects.get(0).getId()).isEqualTo(project.getId());
        assertThat(matchingProjects.get(0).getUpdatedAt()).isEqualTo(recentUpdatedAt);
        assertThat(projectMapper.selectDashboardProjects(
                ProjectStatus.IN_PROGRESS, "后", 30, DashboardService.DUE_SOON_DAYS)).isEmpty();
        assertThat(projectMapper.selectDashboardProjects(
                ProjectStatus.IN_PROGRESS, "后端", 1, DashboardService.DUE_SOON_DAYS)).isEmpty();
    }

    /**
     * 验证逾期和即将到期任务会计入健康度统计，已完成、已取消和已删除任务不计入。
     */
    @Test
    void shouldCountOverdueAndDueSoonTasksForHealth() {
        Project project = createProject("健康度项目", ProjectStatus.IN_PROGRESS, "健康度");
        ProjectPhase phase = createPhase(project.getId());
        createTask(project.getId(), phase.getId(), "逾期任务", TaskStatus.TODO,
                LocalDate.now().minusDays(2));
        createTask(project.getId(), phase.getId(), "即将到期任务", TaskStatus.IN_PROGRESS,
                LocalDate.now().plusDays(1));
        createTask(project.getId(), phase.getId(), "已完成逾期任务", TaskStatus.COMPLETED,
                LocalDate.now().minusDays(2));
        createTask(project.getId(), phase.getId(), "已取消逾期任务", TaskStatus.CANCELLED,
                LocalDate.now().minusDays(2));
        createTask(project.getId(), phase.getId(), "没有截止日期", TaskStatus.TODO, null);
        Task deletedTask = createTask(project.getId(), phase.getId(), "已删除逾期任务",
                TaskStatus.TODO, LocalDate.now().minusDays(2));
        taskMapper.softDeleteById(deletedTask.getId());

        List<DashboardProjectRow> projects = projectMapper.selectDashboardProjects(
                null, null, null, DashboardService.DUE_SOON_DAYS);

        assertThat(projects).extracting(row -> row.getId()).contains(project.getId());
        DashboardProjectRow matchingProject = projects.stream()
                .filter(row -> project.getId().equals(row.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(matchingProject.getOverdueTaskCount()).isEqualTo(1);
        assertThat(matchingProject.getDueSoonTaskCount()).isEqualTo(1);
        assertThat(matchingProject.getTargetDate()).isNull();
    }

    /**
     * 创建测试项目。
     */
    private Project createProject(String name, ProjectStatus status, String tags) {
        Project project = new Project();
        project.setName(name);
        project.setStatus(status);
        project.setProgressMode(ProgressMode.AUTO);
        project.setAutoProgress(40);
        project.setTags(tags);
        projectMapper.insert(project);
        return project;
    }

    /**
     * 创建测试项目阶段。
     */
    private ProjectPhase createPhase(Long projectId) {
        ProjectPhase phase = new ProjectPhase();
        phase.setProjectId(projectId);
        phase.setName("开发实现");
        projectPhaseMapper.insert(phase);
        return phase;
    }

    /**
     * 创建测试任务。
     */
    private Task createTask(Long projectId, Long phaseId) {
        return createTask(projectId, phaseId, "实现仪表盘筛选", TaskStatus.TODO, null);
    }

    /**
     * 创建指定状态和截止日期的测试任务。
     */
    private Task createTask(Long projectId, Long phaseId, String title, TaskStatus status,
            LocalDate dueDate) {
        Task task = new Task();
        task.setProjectId(projectId);
        task.setPhaseId(phaseId);
        task.setTitle(title);
        task.setStatus(status);
        task.setDueDate(dueDate);
        taskMapper.insert(task);
        return task;
    }

    /**
     * 创建测试工作日志。
     */
    private WorkLog createWorkLog(Long taskId) {
        WorkLog workLog = new WorkLog();
        workLog.setTaskId(taskId);
        workLog.setLogDate(LocalDate.now());
        workLog.setSummaryContent("完成仪表盘筛选");
        workLog.setCommitHashes("01f2797");
        workLog.setSpentMinutes(60);
        workLogMapper.insert(workLog);
        return workLog;
    }
}
