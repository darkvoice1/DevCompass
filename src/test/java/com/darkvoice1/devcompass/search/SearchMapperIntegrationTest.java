package com.darkvoice1.devcompass.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
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
import com.darkvoice1.devcompass.project.entity.ProgressMode;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectPhase;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.repository.ProjectPhaseMapper;
import com.darkvoice1.devcompass.search.dto.SearchItemResponse;
import com.darkvoice1.devcompass.search.dto.SearchItemType;
import com.darkvoice1.devcompass.search.repository.SearchMapper;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.task.repository.TaskMapper;
import com.darkvoice1.devcompass.worklog.entity.WorkLog;
import com.darkvoice1.devcompass.worklog.repository.WorkLogMapper;

/**
 * 验证全局搜索在真实 PostgreSQL 上的关键字匹配和排除规则。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class SearchMapperIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private SearchMapper searchMapper;

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
     * 验证项目名称、描述、标签和任务标题、描述都能被关键字命中。
     */
    @Test
    void shouldMatchProjectAndTaskKeywordFields() {
        Project project = createProject("搜索罗盘项目", "个人研发助手", "搜索,后端", false);
        ProjectPhase phase = createPhase(project.getId());
        Task titleTask = createTask(project.getId(), phase.getId(), "实现搜索接口", "补充测试",
                TaskStatus.TODO);
        createTask(project.getId(), phase.getId(), "写文档", "搜索设计说明", TaskStatus.TODO);

        List<SearchItemResponse> projects = searchProjects("%搜索%");
        List<SearchItemResponse> tasks = searchTasks("%搜索%");

        assertThat(projects).extracting(item -> item.getId()).contains(project.getId());
        assertThat(projects).extracting(item -> item.getType()).containsOnly(SearchItemType.PROJECT);
        assertThat(tasks).extracting(item -> item.getId()).contains(titleTask.getId());
        assertThat(tasks).extracting(item -> item.getTitle())
                .contains("实现搜索接口", "写文档");
        assertThat(tasks).extracting(item -> item.getProjectName()).contains("搜索罗盘项目");
    }

    /**
     * 验证已归档项目、已删除项目和已删除任务不会被搜到。
     */
    @Test
    void shouldExcludeArchivedAndDeletedRecords() {
        Project visible = createProject("可见搜索项目", "正常项目", "可见", false);
        Project archived = createProject("归档搜索项目", "已归档", "归档", true);
        Project deleted = createProject("删除搜索项目", "已删除", "删除", false);
        projectMapper.softDeleteById(deleted.getId());

        ProjectPhase visiblePhase = createPhase(visible.getId());
        ProjectPhase archivedPhase = createPhase(archived.getId());
        Task visibleTask = createTask(visible.getId(), visiblePhase.getId(), "可见搜索任务", null,
                TaskStatus.TODO);
        createTask(archived.getId(), archivedPhase.getId(), "归档项目搜索任务", null, TaskStatus.TODO);
        Task deletedTask = createTask(visible.getId(), visiblePhase.getId(), "已删除搜索任务", null,
                TaskStatus.TODO);
        taskMapper.softDeleteById(deletedTask.getId());

        List<SearchItemResponse> projects = searchProjects("%搜索%");
        List<SearchItemResponse> tasks = searchTasks("%搜索%");

        assertThat(projects).extracting(item -> item.getId())
                .contains(visible.getId())
                .doesNotContain(archived.getId(), deleted.getId());
        assertThat(tasks).extracting(item -> item.getId())
                .contains(visibleTask.getId())
                .doesNotContain(deletedTask.getId());
        assertThat(tasks).extracting(item -> item.getTitle())
                .doesNotContain("归档项目搜索任务", "已删除搜索任务");
    }

    /**
     * 验证用户输入的百分号按字面量匹配，不会当成通配符命中所有项目。
     */
    @Test
    void shouldEscapeLikeWildcards() {
        Project percentProject = createProject("百分号项目", "完成度100%验收", null, false);
        Project normalProject = createProject("普通搜索项目", "没有百分号", null, false);

        List<SearchItemResponse> matched = searchProjects("%100\\%%");
        List<SearchItemResponse> percentOnly = searchProjects("%\\%%");

        assertThat(matched).extracting(item -> item.getId())
                .contains(percentProject.getId())
                .doesNotContain(normalProject.getId());
        assertThat(percentOnly).extracting(item -> item.getId())
                .contains(percentProject.getId())
                .doesNotContain(normalProject.getId());
    }

    /**
     * 验证按项目 ID 筛选时不会串到其他项目。
     */
    @Test
    void shouldFilterByProjectId() {
        Project matching = createProject("筛选项目甲", "搜索筛选", "筛选", false);
        Project other = createProject("筛选项目乙", "搜索筛选", "筛选", false);
        ProjectPhase matchingPhase = createPhase(matching.getId());
        ProjectPhase otherPhase = createPhase(other.getId());
        Task matchingTask = createTask(matching.getId(), matchingPhase.getId(), "筛选任务甲", null,
                TaskStatus.TODO);
        createTask(other.getId(), otherPhase.getId(), "筛选任务乙", null, TaskStatus.TODO);

        List<SearchItemResponse> projects = searchMapper.selectProjects(
                "%筛选%", matching.getId(), null, null, null);
        List<SearchItemResponse> tasks = searchMapper.selectTasks(
                "%筛选%", matching.getId(), null, null, null);

        assertThat(projects).extracting(item -> item.getId())
                .contains(matching.getId())
                .doesNotContain(other.getId());
        assertThat(tasks).extracting(item -> item.getId())
                .contains(matchingTask.getId());
        assertThat(tasks).extracting(item -> item.getTitle())
                .contains("筛选任务甲")
                .doesNotContain("筛选任务乙");
    }

    /**
     * 验证按状态筛选项目和任务。
     */
    @Test
    void shouldFilterByStatus() {
        Project inProgress = createProject("进行中筛选项目", "状态筛选", "状态", false,
                ProjectStatus.IN_PROGRESS);
        createProject("已暂停筛选项目", "状态筛选", "状态", false, ProjectStatus.PAUSED);
        ProjectPhase phase = createPhase(inProgress.getId());
        Task todoTask = createTask(inProgress.getId(), phase.getId(), "待办筛选任务", null,
                TaskStatus.TODO);
        createTask(inProgress.getId(), phase.getId(), "进行中筛选任务", null, TaskStatus.IN_PROGRESS);

        List<SearchItemResponse> projects = searchMapper.selectProjects(
                "%状态筛选%", null, ProjectStatus.IN_PROGRESS, null, null);
        List<SearchItemResponse> tasks = searchMapper.selectTasks(
                "%筛选任务%", null, TaskStatus.TODO, null, null);

        assertThat(projects).extracting(item -> item.getId())
                .contains(inProgress.getId());
        assertThat(projects).extracting(item -> item.getTitle())
                .doesNotContain("已暂停筛选项目");
        assertThat(tasks).extracting(item -> item.getId()).contains(todoTask.getId());
        assertThat(tasks).extracting(item -> item.getTitle())
                .contains("待办筛选任务")
                .doesNotContain("进行中筛选任务");
    }

    /**
     * 验证更新日期含当天，明天的范围不包含今天新建的数据。
     */
    @Test
    void shouldFilterByUpdatedDateIncludingToday() {
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zone);
        Instant updatedFrom = today.atStartOfDay(zone).toInstant();
        Instant updatedTo = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant tomorrowFrom = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant tomorrowTo = today.plusDays(2).atStartOfDay(zone).toInstant();

        Project project = createProject("日期筛选项目", "日期筛选", "日期", false);

        List<SearchItemResponse> todayItems = searchMapper.selectProjects(
                "%日期筛选%", null, null, updatedFrom, updatedTo);
        List<SearchItemResponse> tomorrowItems = searchMapper.selectProjects(
                "%日期筛选%", null, null, tomorrowFrom, tomorrowTo);

        assertThat(todayItems).extracting(item -> item.getId()).contains(project.getId());
        assertThat(tomorrowItems).extracting(item -> item.getId()).doesNotContain(project.getId());
    }

    /**
     * 验证工作日志可按计划和总结关键字命中，并带上任务跳转字段。
     */
    @Test
    void shouldMatchWorkLogKeywordFields() {
        Project project = createProject("日志搜索项目", "日志", "日志", false);
        ProjectPhase phase = createPhase(project.getId());
        Task task = createTask(project.getId(), phase.getId(), "完成搜索接口", null, TaskStatus.TODO);
        WorkLog workLog = createWorkLog(task.getId(), LocalDate.of(2026, 9, 19),
                "准备测试数据", "完成统一搜索接口");

        List<SearchItemResponse> items = searchWorkLogs("%统一搜索%");

        assertThat(items).extracting(item -> item.getId()).contains(workLog.getId());
        assertThat(items).extracting(item -> item.getType()).containsOnly(SearchItemType.WORK_LOG);
        assertThat(items).extracting(item -> item.getTitle()).contains("完成搜索接口");
        assertThat(items).extracting(item -> item.getTaskId()).contains(task.getId());
        assertThat(items).extracting(item -> item.getProjectId()).contains(project.getId());
        assertThat(items).extracting(item -> item.getSummary()).contains("完成统一搜索接口");
    }

    /**
     * 验证已删除日志、已删除任务和归档项目下的日志不会被搜到。
     */
    @Test
    void shouldExcludeDeletedAndArchivedWorkLogs() {
        Project visible = createProject("可见日志项目", "日志排除", "日志", false);
        Project archived = createProject("归档日志项目", "日志排除", "日志", true);
        ProjectPhase visiblePhase = createPhase(visible.getId());
        ProjectPhase archivedPhase = createPhase(archived.getId());
        Task visibleTask = createTask(visible.getId(), visiblePhase.getId(), "可见日志任务", null,
                TaskStatus.TODO);
        Task archivedTask = createTask(archived.getId(), archivedPhase.getId(), "归档日志任务", null,
                TaskStatus.TODO);
        WorkLog visibleLog = createWorkLog(visibleTask.getId(), LocalDate.of(2026, 9, 19),
                null, "可见日志总结");
        createWorkLog(archivedTask.getId(), LocalDate.of(2026, 9, 19), null, "归档日志总结");
        Task extraTask = createTask(visible.getId(), visiblePhase.getId(), "待删除日志任务", null,
                TaskStatus.TODO);
        WorkLog deletedLog = createWorkLog(extraTask.getId(), LocalDate.of(2026, 9, 18),
                null, "已删除日志总结");
        workLogMapper.deleteById(deletedLog.getId());

        List<SearchItemResponse> items = searchWorkLogs("%日志总结%");

        assertThat(items).extracting(item -> item.getId()).contains(visibleLog.getId());
        assertThat(items).extracting(item -> item.getSummary())
                .contains("可见日志总结")
                .doesNotContain("归档日志总结", "已删除日志总结");
    }

    /**
     * 验证工作日志按日志日期筛选，包含当天。
     */
    @Test
    void shouldFilterWorkLogsByLogDate() {
        Project project = createProject("日志日期项目", "日志日期", "日期", false);
        ProjectPhase phase = createPhase(project.getId());
        Task task = createTask(project.getId(), phase.getId(), "日志日期任务", null, TaskStatus.TODO);
        WorkLog todayLog = createWorkLog(task.getId(), LocalDate.of(2026, 9, 19),
                null, "当天日志");
        createWorkLog(createTask(project.getId(), phase.getId(), "昨天日志任务", null,
                TaskStatus.TODO).getId(), LocalDate.of(2026, 9, 18), null, "昨天日志");

        List<SearchItemResponse> items = searchMapper.selectWorkLogs(
                "%日志%", null, LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 19));

        assertThat(items).extracting(item -> item.getId()).contains(todayLog.getId());
        assertThat(items).extracting(item -> item.getSummary())
                .contains("当天日志")
                .doesNotContain("昨天日志");
    }

    /**
     * 按关键字查询项目，不附加其他筛选。
     */
    private List<SearchItemResponse> searchProjects(String keyword) {
        return searchMapper.selectProjects(keyword, null, null, null, null);
    }

    /**
     * 按关键字查询任务，不附加其他筛选。
     */
    private List<SearchItemResponse> searchTasks(String keyword) {
        return searchMapper.selectTasks(keyword, null, null, null, null);
    }

    /**
     * 按关键字查询工作日志，不附加其他筛选。
     */
    private List<SearchItemResponse> searchWorkLogs(String keyword) {
        return searchMapper.selectWorkLogs(keyword, null, null, null);
    }

    /**
     * 创建测试工作日志。
     */
    private WorkLog createWorkLog(Long taskId, LocalDate logDate, String planContent,
            String summaryContent) {
        WorkLog workLog = new WorkLog();
        workLog.setTaskId(taskId);
        workLog.setLogDate(logDate);
        workLog.setPlanContent(planContent);
        workLog.setSummaryContent(summaryContent);
        workLog.setSpentMinutes(0);
        workLogMapper.insert(workLog);
        return workLog;
    }

    /**
     * 创建测试项目。
     */
    private Project createProject(String name, String description, String tags, boolean archived) {
        return createProject(name, description, tags, archived, ProjectStatus.IN_PROGRESS);
    }

    /**
     * 创建指定状态的测试项目。
     */
    private Project createProject(String name, String description, String tags, boolean archived,
            ProjectStatus status) {
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setTags(tags);
        project.setStatus(status);
        project.setProgressMode(ProgressMode.AUTO);
        project.setAutoProgress(20);
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
    private Task createTask(Long projectId, Long phaseId, String title, String description,
            TaskStatus status) {
        Task task = new Task();
        task.setProjectId(projectId);
        task.setPhaseId(phaseId);
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        taskMapper.insert(task);
        return task;
    }
}
