package com.darkvoice1.devcompass.search;

import static org.assertj.core.api.Assertions.assertThat;

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

        List<SearchItemResponse> projects = searchMapper.selectProjects("%搜索%");
        List<SearchItemResponse> tasks = searchMapper.selectTasks("%搜索%");

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

        List<SearchItemResponse> projects = searchMapper.selectProjects("%搜索%");
        List<SearchItemResponse> tasks = searchMapper.selectTasks("%搜索%");

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

        List<SearchItemResponse> matched = searchMapper.selectProjects("%100\\%%");
        List<SearchItemResponse> percentOnly = searchMapper.selectProjects("%\\%%");

        assertThat(matched).extracting(item -> item.getId())
                .contains(percentProject.getId())
                .doesNotContain(normalProject.getId());
        assertThat(percentOnly).extracting(item -> item.getId())
                .contains(percentProject.getId())
                .doesNotContain(normalProject.getId());
    }

    /**
     * 创建测试项目。
     */
    private Project createProject(String name, String description, String tags, boolean archived) {
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setTags(tags);
        project.setStatus(ProjectStatus.IN_PROGRESS);
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
