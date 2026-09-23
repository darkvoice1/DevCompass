package com.darkvoice1.devcompass.backup;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.ExecConfig;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.darkvoice1.devcompass.Application;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;

/**
 * 在测试数据库里演练一次备份和恢复。
 */
@Testcontainers
@EnabledIfDockerAvailable
@SpringBootTest(classes = Application.class)
class DatabaseBackupDrillTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private DataSource dataSource;

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
     * 验证项目被删掉后，可以从备份文件里恢复回来。
     */
    @Test
    void shouldRestoreProjectFromDatabaseDump() throws Exception {
        Project project = new Project();
        project.setName("备份演练项目");
        projectMapper.insert(project);
        Long projectId = project.getId();

        runInDatabase("pg_dump", "--data-only", "--column-inserts", "--table=project",
                "--file=/tmp/project-backup.sql");
        deleteProject(projectId);
        assertThat(projectMapper.selectById(projectId)).isNull();

        runInDatabase("psql", "--file=/tmp/project-backup.sql");

        Project restored = projectMapper.selectById(projectId);
        assertThat(restored).isNotNull();
        assertThat(restored.getName()).isEqualTo("备份演练项目");
    }

    /**
     * 在数据库容器里执行一条备份或恢复命令。
     *
     * @param command 容器内命令和参数
     */
    private void runInDatabase(String... command) throws Exception {
        String[] fullCommand = new String[command.length + 3];
        fullCommand[0] = command[0];
        fullCommand[1] = "--username=" + POSTGRES.getUsername();
        fullCommand[2] = "--dbname=" + POSTGRES.getDatabaseName();
        fullCommand[3] = "--no-password";
        System.arraycopy(command, 1, fullCommand, 4, command.length - 1);
        org.testcontainers.containers.Container.ExecResult result = POSTGRES.execInContainer(ExecConfig.builder()
                .command(fullCommand)
                .envVars(Map.of("PGPASSWORD", POSTGRES.getPassword()))
                .build());
        assertThat(result.getExitCode())
                .withFailMessage(result.getStdout() + System.lineSeparator() + result.getStderr())
                .isZero();
    }

    /**
     * 从数据库里真正删除项目，而不是只做软删除。
     *
     * @param projectId 项目主键
     */
    private void deleteProject(Long projectId) throws Exception {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM project WHERE id = ?")) {
            statement.setLong(1, projectId);
            statement.executeUpdate();
        }
    }
}
