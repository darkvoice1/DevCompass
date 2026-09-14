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
import com.darkvoice1.devcompass.project.entity.ProgressMode;
import com.darkvoice1.devcompass.project.dto.UpdateProjectProgressRequest;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.service.ProgressService;
import com.darkvoice1.devcompass.task.repository.TaskMapper;

/**
 * 验证项目自动进度计算和人工校准服务。
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

    /**
     * 验证手动模式会保存人工进度和校准原因。
     */
    @Test
    void shouldSaveManualProgressAndReason() {
        Project project = new Project();
        project.setId(1L);
        project.setAutoProgress(40);
        when(projectMapper.selectById(1L)).thenReturn(project);
        UpdateProjectProgressRequest request = new UpdateProjectProgressRequest();
        request.setMode(ProgressMode.MANUAL);
        request.setManualProgress(60);
        request.setProgressReason("核心功能已经完成");

        var response = progressService.updateProjectProgress(1L, request);

        assertThat(response.getMode()).isEqualTo(ProgressMode.MANUAL);
        assertThat(response.getProgress()).isEqualTo(60);
        assertThat(response.getProgressReason()).isEqualTo("核心功能已经完成");
        verify(projectMapper).updateById(project);
    }

    /**
     * 验证手动模式缺少校准原因时会被拒绝。
     */
    @Test
    void shouldRejectManualProgressWithoutReason() {
        when(projectMapper.selectById(1L)).thenReturn(new Project());
        UpdateProjectProgressRequest request = new UpdateProjectProgressRequest();
        request.setMode(ProgressMode.MANUAL);
        request.setManualProgress(60);

        assertThatThrownBy(() -> progressService.updateProjectProgress(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("手动模式必须填写校准原因");
    }

    /**
     * 验证切回自动模式时会重新计算并清除人工校准数据。
     */
    @Test
    void shouldRecalculateWhenSwitchingBackToAutoMode() {
        Project project = new Project();
        project.setId(1L);
        project.setProgressMode(ProgressMode.MANUAL);
        project.setManualProgress(80);
        project.setProgressReason("临时校准");
        when(projectMapper.selectById(1L)).thenReturn(project);
        when(taskMapper.selectCount(any()))
                .thenReturn(3L)
                .thenReturn(1L);
        UpdateProjectProgressRequest request = new UpdateProjectProgressRequest();
        request.setMode(ProgressMode.AUTO);

        var response = progressService.updateProjectProgress(1L, request);

        assertThat(response.getMode()).isEqualTo(ProgressMode.AUTO);
        assertThat(response.getProgress()).isEqualTo(33);
        assertThat(response.getManualProgress()).isNull();
        assertThat(response.getProgressReason()).isNull();
        verify(projectMapper).updateById(project);
    }
}
