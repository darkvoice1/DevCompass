package com.darkvoice1.devcompass.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
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
import com.darkvoice1.devcompass.dashboard.dto.FocusListItemKind;
import com.darkvoice1.devcompass.dashboard.dto.FocusListItemResponse;
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

/**
 * 验证本周任务清单在真实 PostgreSQL 上的筛选规则。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class FocusListMapperIntegrationTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

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
     * 验证本周清单包含今天和本周边界，排除上周、已完成、已取消、已删除和归档项目中的任务。
     */
    @Test
    void shouldSelectThisWeekTasksAndExcludeInvalidOnes() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = today.with(DayOfWeek.SUNDAY);

        Project project = createProject("本周清单项目", ProjectStatus.IN_PROGRESS, null, false);
        ProjectPhase phase = createPhase(project.getId());
        Task todayTask = createTask(project.getId(), phase.getId(), "今天任务",
                TaskStatus.TODO, today);
        Task mondayTask = createTask(project.getId(), phase.getId(), "周一任务",
                TaskStatus.IN_PROGRESS, monday);
        Task sundayTask = createTask(project.getId(), phase.getId(), "周日任务",
                TaskStatus.TODO, sunday);
        createTask(project.getId(), phase.getId(), "上周任务", TaskStatus.TODO, monday.minusDays(1));
        createTask(project.getId(), phase.getId(), "下周任务", TaskStatus.TODO, sunday.plusDays(1));
        createTask(project.getId(), phase.getId(), "已完成任务", TaskStatus.COMPLETED, today);
        createTask(project.getId(), phase.getId(), "已取消任务", TaskStatus.CANCELLED, today);
        createTask(project.getId(), phase.getId(), "没有截止日期", TaskStatus.TODO, null);
        Task deletedTask = createTask(project.getId(), phase.getId(), "已删除任务",
                TaskStatus.TODO, today);
        taskMapper.softDeleteById(deletedTask.getId());

        Project archivedProject = createProject("归档清单项目", ProjectStatus.IN_PROGRESS, null, true);
        ProjectPhase archivedPhase = createPhase(archivedProject.getId());
        createTask(archivedProject.getId(), archivedPhase.getId(), "归档项目任务",
                TaskStatus.TODO, today);

        List<FocusListItemResponse> items = taskMapper.selectFocusListTasks(monday, sunday);

        assertThat(items).extracting(item -> item.getTaskId())
                .contains(todayTask.getId(), mondayTask.getId(), sundayTask.getId())
                .doesNotContain(deletedTask.getId());
        assertThat(items).extracting(item -> item.getTitle())
                .contains("今天任务", "周一任务", "周日任务")
                .doesNotContain("上周任务", "下周任务", "已完成任务", "已取消任务",
                        "没有截止日期", "已删除任务", "归档项目任务");
        assertThat(items.stream()
                .filter(item -> project.getId().equals(item.getProjectId()))
                .toList()).extracting(item -> item.getItemKind())
                .containsOnly(FocusListItemKind.TASK);
    }

    /**
     * 验证逾期清单包含昨天的任务和目标日期已过的项目，不包含今天的任务和已完成项目。
     */
    @Test
    void shouldSelectOverdueTasksAndDelayedProjects() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate yesterday = today.minusDays(1);

        Project project = createProject("逾期清单项目", ProjectStatus.IN_PROGRESS, yesterday, false);
        ProjectPhase phase = createPhase(project.getId());
        Task overdueTask = createTask(project.getId(), phase.getId(), "逾期任务",
                TaskStatus.TODO, yesterday);
        createTask(project.getId(), phase.getId(), "今天任务", TaskStatus.TODO, today);
        createTask(project.getId(), phase.getId(), "已完成逾期任务", TaskStatus.COMPLETED, yesterday);

        Project delayedProject = createProject("目标日期已过项目", ProjectStatus.IN_PROGRESS,
                yesterday, false);
        createProject("已完成但仍过期项目", ProjectStatus.COMPLETED, yesterday, false);
        createProject("归档延期项目", ProjectStatus.IN_PROGRESS, yesterday, true);

        List<FocusListItemResponse> overdueTasks = taskMapper.selectFocusListTasks(null, yesterday);
        List<FocusListItemResponse> overdueProjects = projectMapper.selectOverdueFocusProjects(today);

        assertThat(overdueTasks).extracting(item -> item.getTaskId()).contains(overdueTask.getId());
        assertThat(overdueTasks).extracting(item -> item.getTitle())
                .contains("逾期任务")
                .doesNotContain("今天任务", "已完成逾期任务");
        assertThat(overdueProjects).extracting(item -> item.getProjectId())
                .contains(project.getId(), delayedProject.getId());
        assertThat(overdueProjects).extracting(item -> item.getTitle())
                .contains("逾期清单项目", "目标日期已过项目")
                .doesNotContain("已完成但仍过期项目", "归档延期项目");
        assertThat(overdueProjects).extracting(item -> item.getItemKind())
                .containsOnly(FocusListItemKind.PROJECT);
        assertThat(overdueProjects).allSatisfy(item -> assertThat(item.getTaskId()).isNull());
    }

    /**
     * 验证即将到期清单包含今天和第 7 天，不包含昨天和第 8 天。
     */
    @Test
    void shouldSelectDueSoonTasksWithinSevenDays() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate daySeven = today.plusDays(DashboardService.DUE_SOON_DAYS);
        LocalDate dayEight = daySeven.plusDays(1);

        Project project = createProject("即将到期项目", ProjectStatus.IN_PROGRESS, null, false);
        ProjectPhase phase = createPhase(project.getId());
        Task todayTask = createTask(project.getId(), phase.getId(), "今天到期",
                TaskStatus.TODO, today);
        Task daySevenTask = createTask(project.getId(), phase.getId(), "第七天到期",
                TaskStatus.IN_PROGRESS, daySeven);
        createTask(project.getId(), phase.getId(), "昨天到期", TaskStatus.TODO, today.minusDays(1));
        createTask(project.getId(), phase.getId(), "第八天到期", TaskStatus.TODO, dayEight);

        List<FocusListItemResponse> items = taskMapper.selectFocusListTasks(today, daySeven);

        assertThat(items).extracting(item -> item.getTaskId())
                .contains(todayTask.getId(), daySevenTask.getId());
        assertThat(items).extracting(item -> item.getTitle())
                .contains("今天到期", "第七天到期")
                .doesNotContain("昨天到期", "第八天到期");
    }

    /**
     * 验证阻塞清单只包含未完成的阻塞任务，已完成、已取消、已删除和归档项目中的任务不计入。
     */
    @Test
    void shouldSelectBlockedTasksAndExcludeInvalidOnes() {
        Project project = createProject("阻塞清单项目", ProjectStatus.IN_PROGRESS, null, false);
        ProjectPhase phase = createPhase(project.getId());
        Task blockedTask = createBlockedTask(project.getId(), phase.getId(), "卡住的任务",
                TaskStatus.IN_PROGRESS, "依赖登录接口");
        createBlockedTask(project.getId(), phase.getId(), "没有截止日期的阻塞", TaskStatus.TODO, null);
        createTask(project.getId(), phase.getId(), "未阻塞任务", TaskStatus.TODO, null);
        createBlockedTask(project.getId(), phase.getId(), "已完成但仍阻塞", TaskStatus.COMPLETED, "旧原因");
        createBlockedTask(project.getId(), phase.getId(), "已取消但仍阻塞", TaskStatus.CANCELLED, "旧原因");
        Task deletedTask = createBlockedTask(project.getId(), phase.getId(), "已删除阻塞",
                TaskStatus.TODO, "旧原因");
        taskMapper.softDeleteById(deletedTask.getId());

        Project archivedProject = createProject("归档阻塞项目", ProjectStatus.IN_PROGRESS, null, true);
        ProjectPhase archivedPhase = createPhase(archivedProject.getId());
        createBlockedTask(archivedProject.getId(), archivedPhase.getId(), "归档项目阻塞",
                TaskStatus.TODO, "旧原因");

        List<FocusListItemResponse> items = taskMapper.selectBlockedFocusListTasks();

        assertThat(items).extracting(item -> item.getTaskId()).contains(blockedTask.getId());
        assertThat(items).extracting(item -> item.getTitle())
                .contains("卡住的任务", "没有截止日期的阻塞")
                .doesNotContain("未阻塞任务", "已完成但仍阻塞", "已取消但仍阻塞",
                        "已删除阻塞", "归档项目阻塞");
        assertThat(items.stream()
                .filter(item -> blockedTask.getId().equals(item.getTaskId()))
                .findFirst()
                .orElseThrow()
                .getBlockerReason()).isEqualTo("依赖登录接口");
    }

    /**
     * 验证逾期、即将到期和阻塞三类清单不会把对方的典型数据算进去。
     */
    @Test
    void shouldKeepOverdueDueSoonAndBlockedListsSeparate() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate yesterday = today.minusDays(1);
        LocalDate daySeven = today.plusDays(DashboardService.DUE_SOON_DAYS);

        Project project = createProject("清单隔离项目", ProjectStatus.IN_PROGRESS, null, false);
        ProjectPhase phase = createPhase(project.getId());
        Task overdueTask = createTask(project.getId(), phase.getId(), "隔离-逾期任务",
                TaskStatus.TODO, yesterday);
        Task dueSoonTask = createTask(project.getId(), phase.getId(), "隔离-即将到期任务",
                TaskStatus.TODO, today);
        Task blockedTask = createBlockedTask(project.getId(), phase.getId(), "隔离-阻塞任务",
                TaskStatus.TODO, "等待评审");

        List<FocusListItemResponse> overdueItems = taskMapper.selectFocusListTasks(null, yesterday);
        List<FocusListItemResponse> dueSoonItems = taskMapper.selectFocusListTasks(today, daySeven);
        List<FocusListItemResponse> blockedItems = taskMapper.selectBlockedFocusListTasks();

        assertThat(overdueItems).extracting(item -> item.getTaskId())
                .contains(overdueTask.getId())
                .doesNotContain(dueSoonTask.getId(), blockedTask.getId());
        assertThat(dueSoonItems).extracting(item -> item.getTaskId())
                .contains(dueSoonTask.getId())
                .doesNotContain(overdueTask.getId(), blockedTask.getId());
        assertThat(blockedItems).extracting(item -> item.getTaskId())
                .contains(blockedTask.getId())
                .doesNotContain(overdueTask.getId(), dueSoonTask.getId());
    }

    /**
     * 创建测试项目。
     */
    private Project createProject(String name, ProjectStatus status, LocalDate targetDate,
            boolean archived) {
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
     * 创建指定阻塞原因的测试任务。
     */
    private Task createBlockedTask(Long projectId, Long phaseId, String title, TaskStatus status,
            String blockerReason) {
        Task task = new Task();
        task.setProjectId(projectId);
        task.setPhaseId(phaseId);
        task.setTitle(title);
        task.setStatus(status);
        task.setBlocked(true);
        task.setBlockerReason(blockerReason);
        taskMapper.insert(task);
        return task;
    }
}
