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
import com.darkvoice1.devcompass.project.dto.CreateProjectRequest;
import com.darkvoice1.devcompass.project.dto.ProjectDetailResponse;
import com.darkvoice1.devcompass.project.service.ProjectService;

/**
 * 验证真实业务操作会写入动态记录。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class ActivityRecordingIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ActivityMapper activityMapper;

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
     * 验证创建项目后可以按项目查到创建动态。
     */
    @Test
    void shouldRecordCreatedActivityWhenCreatingProject() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("动态闭环项目");

        ProjectDetailResponse project = projectService.createProject(request);

        List<Activity> items = activityMapper.selectList(new QueryWrapper<Activity>()
                .eq("project_id", project.getId())
                .orderByAsc("id"));
        assertThat(items).hasSize(1);
        Activity activity = items.get(0);
        assertThat(activity.getObjectType()).isEqualTo(ActivityObjectType.PROJECT);
        assertThat(activity.getObjectId()).isEqualTo(project.getId());
        assertThat(activity.getAction()).isEqualTo(ActivityAction.CREATED);
        assertThat(activity.getSummary()).isEqualTo("创建项目「动态闭环项目」");
        assertThat(activity.getCreatedAt()).isNotNull();
    }
}
