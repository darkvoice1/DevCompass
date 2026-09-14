package com.darkvoice1.devcompass.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.service.ProgressService;
import com.darkvoice1.devcompass.task.repository.TaskMapper;

/**
 * 验证项目自动进度计算服务。
 */
class ProgressServiceTest {

    private ProjectMapper projectMapper;
    private TaskMapper taskMapper;
    private ProgressService progressService;

    /**
     * 初始化项目进度服务及 Mapper 模拟对象。
     */
    @BeforeEach
    void setUp() {
        projectMapper = mock(ProjectMapper.class);
        taskMapper = mock(TaskMapper.class);
        progressService = new ProgressService(projectMapper, taskMapper);
    }

    /**
     * 验证没有任务时自动进度为零。
     */
    @Test
    void shouldReturnZeroWhenProjectHasNoTasks() {
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        when(taskMapper.selectCount(any())).thenReturn(0L);

        assertThat(progressService.calculateAutoProgress(1L)).isZero();
    }

    /**
     * 验证自动进度按已完成任务占比计算。
     */
    @Test
    void shouldCalculateCompletedTaskPercentage() {
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        when(taskMapper.selectCount(any()))
                .thenReturn(5L)
                .thenReturn(2L);

        assertThat(progressService.calculateAutoProgress(1L)).isEqualTo(40);
    }

    /**
     * 验证重新计算后会保存项目自动进度。
     */
    @Test
    void shouldRefreshAndSaveAutoProgress() {
        Project project = new Project();
        project.setId(1L);
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(taskMapper.selectCount(any()))
                .thenReturn(4L)
                .thenReturn(4L);

        assertThat(progressService.refreshAutoProgress(1L)).isEqualTo(100);
        assertThat(project.getAutoProgress()).isEqualTo(100);
        assertThat(project.getUpdatedAt()).isNotNull();
        verify(projectMapper).updateById(project);
    }

    /**
     * 验证项目不存在时不执行任务统计。
     */
    @Test
    void shouldRejectProgressCalculationForMissingProject() {
        when(projectMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> progressService.calculateAutoProgress(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目不存在");
    }
}
