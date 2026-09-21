package com.darkvoice1.devcompass.activity;

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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.darkvoice1.devcompass.Application;
import com.darkvoice1.devcompass.activity.entity.Activity;
import com.darkvoice1.devcompass.activity.entity.ActivityAction;
import com.darkvoice1.devcompass.activity.entity.ActivityObjectType;
import com.darkvoice1.devcompass.activity.repository.ActivityMapper;
import com.darkvoice1.devcompass.activity.service.ActivityService;
import com.darkvoice1.devcompass.project.entity.ProgressMode;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;

/**
 * 验证动态记录能在真实 PostgreSQL 中写入，并按项目查出。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class ActivityMapperIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ProjectMapper projectMapper;

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
     * 验证同一项目下的多条动态可以插入，并按项目查询出来。
     */
    @Test
    void shouldInsertAndSelectActivitiesByProject() {
        Project project = createProject("动态项目");
        Project otherProject = createProject("其他项目");

        activityService.record(project.getId(), ActivityObjectType.PROJECT, project.getId(),
                ActivityAction.CREATED, "创建项目「动态项目」");
        activityService.record(project.getId(), ActivityObjectType.TASK, 99L,
                ActivityAction.STATUS_CHANGED, "任务状态从 TODO 变为 COMPLETED");
        activityService.record(otherProject.getId(), ActivityObjectType.PROJECT,
                otherProject.getId(), ActivityAction.CREATED, "创建项目「其他项目」");

        List<Activity> items = activityMapper.selectList(new QueryWrapper<Activity>()
                .eq("project_id", project.getId())
                .orderByAsc("id"));

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getObjectType()).isEqualTo(ActivityObjectType.PROJECT);
        assertThat(items.get(0).getObjectId()).isEqualTo(project.getId());
        assertThat(items.get(0).getAction()).isEqualTo(ActivityAction.CREATED);
        assertThat(items.get(0).getSummary()).isEqualTo("创建项目「动态项目」");
        assertThat(items.get(0).getCreatedAt()).isNotNull();
        assertThat(items.get(1).getObjectType()).isEqualTo(ActivityObjectType.TASK);
        assertThat(items.get(1).getAction()).isEqualTo(ActivityAction.STATUS_CHANGED);
        assertThat(items).extracting(item -> item.getSummary())
                .doesNotContain("创建项目「其他项目」");
    }

    /**
     * 创建测试项目。
     */
    private Project createProject(String name) {
        Project project = new Project();
        project.setName(name);
        project.setStatus(ProjectStatus.IN_PROGRESS);
        project.setProgressMode(ProgressMode.AUTO);
        project.setAutoProgress(0);
        projectMapper.insert(project);
        return project;
    }
}
