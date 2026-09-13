package com.darkvoice1.devcompass.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

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
import com.darkvoice1.devcompass.task.dto.TaskPageQueryRequest;
import com.darkvoice1.devcompass.task.dto.TaskPageResponse;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.entity.TaskPriority;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.task.repository.TaskMapper;
import com.darkvoice1.devcompass.task.service.TaskService;

/**
 * 验证任务分页查询在 PostgreSQL 中的完整执行链路。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class TaskPageQueryIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectPhaseMapper projectPhaseMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskService taskService;

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
     * 验证组合筛选、排序、分页和软删除过滤可以在真实数据库中同时生效。
     */
    @Test
    void shouldFilterSortAndPageTasksInPostgres() {
        Project project = new Project();
        project.setName("分页查询测试项目");
        projectMapper.insert(project);

        ProjectPhase phase = new ProjectPhase();
        phase.setProjectId(project.getId());
        phase.setName("开发阶段");
        projectPhaseMapper.insert(phase);

        Task matchingTask = task(project.getId(), phase.getId(), "实现分页接口",
                LocalDate.of(2026, 6, 1));
        taskMapper.insert(matchingTask);

        Task outsideDateTask = task(project.getId(), phase.getId(), "实现分页接口旧版本",
                LocalDate.of(2025, 6, 1));
        taskMapper.insert(outsideDateTask);

        Task deletedTask = task(project.getId(), phase.getId(), "实现分页接口已删除",
                LocalDate.of(2026, 6, 2));
        taskMapper.insert(deletedTask);
        taskMapper.softDeleteById(deletedTask.getId());

        TaskPageQueryRequest request = new TaskPageQueryRequest();
        request.setProjectId(project.getId());
        request.setPage(1L);
        request.setPageSize(10);
        request.setPhaseId(phase.getId());
        request.setStatus(TaskStatus.TODO);
        request.setPriority(TaskPriority.HIGH);
        request.setDueDateFrom(LocalDate.of(2026, 1, 1));
        request.setDueDateTo(LocalDate.of(2026, 12, 31));
        request.setKeyword("分页接口");
        request.setSortBy("dueDate");
        request.setSortDirection("asc");

        TaskPageResponse response = taskService.queryTasksPage(request);

        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.getRecords()).hasSize(1);
        assertThat(response.getRecords().getFirst().getTitle()).isEqualTo("实现分页接口");
    }

    /**
     * 创建用于集成测试的任务。
     */
    private Task task(Long projectId, Long phaseId, String title, LocalDate dueDate) {
        Task task = new Task();
        task.setProjectId(projectId);
        task.setPhaseId(phaseId);
        task.setTitle(title);
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.HIGH);
        task.setDueDate(dueDate);
        return task;
    }
}
