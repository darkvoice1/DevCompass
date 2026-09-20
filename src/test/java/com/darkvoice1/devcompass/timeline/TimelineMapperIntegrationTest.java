package com.darkvoice1.devcompass.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

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
import com.darkvoice1.devcompass.project.entity.ProgressMode;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectPhase;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.repository.ProjectPhaseMapper;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.entity.TaskPriority;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.task.repository.TaskMapper;
import com.darkvoice1.devcompass.timeline.dto.TimelineEventResponse;
import com.darkvoice1.devcompass.timeline.dto.TimelineEventType;
import com.darkvoice1.devcompass.timeline.repository.TimelineMapper;

/**
 * 验证时间线任务事件在真实 PostgreSQL 上的日期范围和排除规则。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class TimelineMapperIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private TimelineMapper timelineMapper;

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
     * 验证范围内的任务会出现，范围外、无截止日期、已取消、已删除和归档项目中的任务不出现。
     */
    @Test
    void shouldSelectTaskEventsInsideDateRange() {
        Project project = createProject("时间线项目", false);
        ProjectPhase phase = createPhase(project.getId());
        Task inRange = createTask(project.getId(), phase.getId(), "范围内任务",
                TaskStatus.TODO, LocalDate.of(2026, 9, 16), TaskPriority.HIGH);
        Task completed = createTask(project.getId(), phase.getId(), "范围内已完成",
                TaskStatus.COMPLETED, LocalDate.of(2026, 9, 20), TaskPriority.MEDIUM);
        createTask(project.getId(), phase.getId(), "范围前任务", TaskStatus.TODO,
                LocalDate.of(2026, 8, 31), TaskPriority.MEDIUM);
        createTask(project.getId(), phase.getId(), "范围后任务", TaskStatus.TODO,
                LocalDate.of(2026, 10, 1), TaskPriority.MEDIUM);
        createTask(project.getId(), phase.getId(), "没有截止日期", TaskStatus.TODO,
                null, TaskPriority.MEDIUM);
        createTask(project.getId(), phase.getId(), "已取消任务", TaskStatus.CANCELLED,
                LocalDate.of(2026, 9, 18), TaskPriority.MEDIUM);
        Task deleted = createTask(project.getId(), phase.getId(), "已删除任务",
                TaskStatus.TODO, LocalDate.of(2026, 9, 19), TaskPriority.MEDIUM);
        taskMapper.softDeleteById(deleted.getId());

        Project archived = createProject("归档时间线项目", true);
        ProjectPhase archivedPhase = createPhase(archived.getId());
        createTask(archived.getId(), archivedPhase.getId(), "归档项目任务",
                TaskStatus.TODO, LocalDate.of(2026, 9, 16), TaskPriority.MEDIUM);

        List<TimelineEventResponse> items = timelineMapper.selectTaskEvents(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(items).extracting(item -> item.getId())
                .contains(inRange.getId(), completed.getId())
                .doesNotContain(deleted.getId());
        assertThat(items).extracting(item -> item.getTitle())
                .contains("范围内任务", "范围内已完成")
                .doesNotContain("范围前任务", "范围后任务", "没有截止日期", "已取消任务",
                        "已删除任务", "归档项目任务");
        assertThat(items.stream()
                .filter(item -> inRange.getId().equals(item.getId()))
                .findFirst()
                .orElseThrow()).satisfies(item -> {
                    assertThat(item.getType()).isEqualTo(TimelineEventType.TASK);
                    assertThat(item.getDate()).isEqualTo(LocalDate.of(2026, 9, 16));
                    assertThat(item.getProjectName()).isEqualTo("时间线项目");
                    assertThat(item.getPriority()).isEqualTo(TaskPriority.HIGH);
                    assertThat(item.getStatus()).isEqualTo(TaskStatus.TODO);
                });
    }

    /**
     * 验证目标日期在范围内的项目会出现，无目标日期、归档和已删除项目不出现。
     */
    @Test
    void shouldSelectProjectEventsInsideDateRange() {
        Project inRange = createProject("目标日期项目", false, LocalDate.of(2026, 9, 15),
                ProjectStatus.IN_PROGRESS);
        Project completed = createProject("已完成目标项目", false, LocalDate.of(2026, 9, 28),
                ProjectStatus.COMPLETED);
        createProject("没有目标日期", false, null, ProjectStatus.IN_PROGRESS);
        createProject("范围外目标项目", false, LocalDate.of(2026, 10, 2),
                ProjectStatus.IN_PROGRESS);
        createProject("归档目标项目", true, LocalDate.of(2026, 9, 15),
                ProjectStatus.IN_PROGRESS);
        Project deleted = createProject("删除目标项目", false, LocalDate.of(2026, 9, 16),
                ProjectStatus.IN_PROGRESS);
        projectMapper.softDeleteById(deleted.getId());

        List<TimelineEventResponse> items = timelineMapper.selectProjectEvents(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(items).extracting(item -> item.getId())
                .contains(inRange.getId(), completed.getId())
                .doesNotContain(deleted.getId());
        assertThat(items).extracting(item -> item.getTitle())
                .contains("目标日期项目", "已完成目标项目")
                .doesNotContain("没有目标日期", "范围外目标项目", "归档目标项目", "删除目标项目");
        assertThat(items.stream()
                .filter(item -> inRange.getId().equals(item.getId()))
                .findFirst()
                .orElseThrow()).satisfies(item -> {
                    assertThat(item.getType()).isEqualTo(TimelineEventType.PROJECT);
                    assertThat(item.getDate()).isEqualTo(LocalDate.of(2026, 9, 15));
                    assertThat(item.getProjectId()).isEqualTo(inRange.getId());
                    assertThat(item.isCompleted()).isFalse();
                });
        assertThat(items.stream()
                .filter(item -> completed.getId().equals(item.getId()))
                .findFirst()
                .orElseThrow()
                .isCompleted()).isTrue();
    }

    /**
     * 验证跨月日期范围会同时返回上月末和本月初的任务与项目。
     */
    @Test
    void shouldSelectEventsAcrossMonths() {
        Project project = createProject("跨月时间线项目", false, LocalDate.of(2026, 8, 31),
                ProjectStatus.IN_PROGRESS);
        ProjectPhase phase = createPhase(project.getId());
        Task septemberTask = createTask(project.getId(), phase.getId(), "九月初任务",
                TaskStatus.TODO, LocalDate.of(2026, 9, 5), TaskPriority.MEDIUM);

        List<TimelineEventResponse> tasks = timelineMapper.selectTaskEvents(
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 10));
        List<TimelineEventResponse> projects = timelineMapper.selectProjectEvents(
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 10));

        assertThat(tasks).extracting(item -> item.getId()).contains(septemberTask.getId());
        assertThat(tasks).extracting(item -> item.getTitle()).contains("九月初任务");
        assertThat(projects).extracting(item -> item.getId()).contains(project.getId());
        assertThat(projects).extracting(item -> item.getDate())
                .contains(LocalDate.of(2026, 8, 31));
    }

    /**
     * 创建测试项目。
     */
    private Project createProject(String name, boolean archived) {
        return createProject(name, archived, null, ProjectStatus.IN_PROGRESS);
    }

    /**
     * 创建指定目标日期和状态的测试项目。
     */
    private Project createProject(String name, boolean archived, LocalDate targetDate,
            ProjectStatus status) {
        Project project = new Project();
        project.setName(name);
        project.setStatus(status);
        project.setProgressMode(ProgressMode.AUTO);
        project.setAutoProgress(20);
        project.setTargetDate(targetDate);
        project.setArchived(archived);
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
    private Task createTask(Long projectId, Long phaseId, String title, TaskStatus status,
            LocalDate dueDate, TaskPriority priority) {
        Task task = new Task();
        task.setProjectId(projectId);
        task.setPhaseId(phaseId);
        task.setTitle(title);
        task.setStatus(status);
        task.setDueDate(dueDate);
        task.setPriority(priority);
        taskMapper.insert(task);
        return task;
    }
}
