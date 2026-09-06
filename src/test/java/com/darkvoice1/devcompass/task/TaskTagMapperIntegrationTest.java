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
import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.tag.entity.Tag;
import com.darkvoice1.devcompass.tag.repository.TagMapper;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.entity.TaskPriority;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.task.entity.TaskTag;
import com.darkvoice1.devcompass.task.repository.TaskMapper;
import com.darkvoice1.devcompass.task.repository.TaskTagMapper;

/**
 * 验证任务、标签及关联表的 PostgreSQL 持久化集成。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class TaskTagMapperIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private TaskTagMapper taskTagMapper;

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
     * 验证任务、标签和多对多关联可以新增并按主键查询。
     */
    @Test
    void shouldInsertAndFindTaskTagModel() {
        Project project = new Project();
        project.setName("任务模型验证项目");
        project.setStatus(ProjectStatus.PLANNED);
        projectMapper.insert(project);

        Task task = new Task();
        task.setProjectId(project.getId());
        task.setTitle("设计任务表");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.HIGH);
        task.setDueDate(LocalDate.of(2026, 10, 1));
        task.setEstimatedHours(8);
        taskMapper.insert(task);

        Tag tag = new Tag();
        tag.setProjectId(project.getId());
        tag.setName("后端");
        tagMapper.insert(tag);

        TaskTag taskTag = new TaskTag();
        taskTag.setTaskId(task.getId());
        taskTag.setTagId(tag.getId());
        taskTagMapper.insert(taskTag);

        Task storedTask = taskMapper.selectById(task.getId());
        Tag storedTag = tagMapper.selectById(tag.getId());
        TaskTag storedTaskTag = taskTagMapper.selectById(taskTag.getId());

        assertThat(storedTask.getProjectId()).isEqualTo(project.getId());
        assertThat(storedTask.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(storedTask.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(storedTask.getEstimatedHours()).isEqualTo(8);
        assertThat(storedTag.getProjectId()).isEqualTo(project.getId());
        assertThat(storedTag.getName()).isEqualTo("后端");
        assertThat(storedTaskTag.getTaskId()).isEqualTo(task.getId());
        assertThat(storedTaskTag.getTagId()).isEqualTo(tag.getId());
    }
}
