package com.darkvoice1.devcompass.importexport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.darkvoice1.devcompass.activity.entity.Activity;
import com.darkvoice1.devcompass.activity.repository.ActivityMapper;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile;
import com.darkvoice1.devcompass.importexport.dto.ProjectImportResponse;
import com.darkvoice1.devcompass.importexport.service.ProjectImportService;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectPhase;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.repository.ProjectPhaseMapper;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.repository.TaskMapper;
import com.darkvoice1.devcompass.worklog.entity.WorkLog;
import com.darkvoice1.devcompass.worklog.repository.WorkLogMapper;

/**
 * 验证项目 JSON 导入的校验和编号重连。
 */
class ProjectImportServiceTest {

    private ProjectMapper projectMapper;

    private ProjectPhaseMapper projectPhaseMapper;

    private TaskMapper taskMapper;

    private WorkLogMapper workLogMapper;

    private ActivityMapper activityMapper;

    private ProjectImportService projectImportService;

    /**
     * 初始化导入服务。写入时由测试分配新编号。
     */
    @BeforeEach
    void setUp() {
        projectMapper = mock(ProjectMapper.class);
        projectPhaseMapper = mock(ProjectPhaseMapper.class);
        taskMapper = mock(TaskMapper.class);
        workLogMapper = mock(WorkLogMapper.class);
        activityMapper = mock(ActivityMapper.class);
        projectImportService = new ProjectImportService(
                projectMapper, projectPhaseMapper, taskMapper, workLogMapper, activityMapper);
        when(projectMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            invocation.getArgument(0, Project.class).setId(100L);
            return 1;
        }).when(projectMapper).insert(any(Project.class));
        doAnswer(invocation -> {
            invocation.getArgument(0, ProjectPhase.class).setId(200L);
            return 1;
        }).when(projectPhaseMapper).insert(any(ProjectPhase.class));
        doAnswer(invocation -> {
            invocation.getArgument(0, Task.class).setId(300L);
            return 1;
        }).when(taskMapper).insert(any(Task.class));
    }

    /**
     * 验证导入后任务仍挂在对应阶段，附件不写入数据库。
     */
    @Test
    void shouldImportProjectAndRemapIds() {
        ProjectImportResponse response = projectImportService.importProject(validFile());

        assertThat(response.getProjectId()).isEqualTo(100L);
        assertThat(response.getProjectName()).isEqualTo("研发罗盘");
        assertThat(response.getSkippedAttachmentCount()).isEqualTo(1);
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskMapper).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getProjectId()).isEqualTo(100L);
        assertThat(taskCaptor.getValue().getPhaseId()).isEqualTo(200L);
        assertThat(taskCaptor.getValue().getTitle()).isEqualTo("导出项目");
        ArgumentCaptor<WorkLog> workLogCaptor = ArgumentCaptor.forClass(WorkLog.class);
        verify(workLogMapper).insert(workLogCaptor.capture());
        assertThat(workLogCaptor.getValue().getTaskId()).isEqualTo(300L);
        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).insert(activityCaptor.capture());
        assertThat(activityCaptor.getValue().getProjectId()).isEqualTo(100L);
        assertThat(activityCaptor.getValue().getObjectId()).isEqualTo(300L);
    }

    /**
     * 验证版本不对、缺少名称或同名项目时不会写入。
     */
    @Test
    void shouldRejectInvalidFileBeforeInsert() {
        ProjectExportFile wrongVersion = validFile();
        wrongVersion.setFormatVersion(2);
        assertValidation(wrongVersion, "导入文件版本不正确");

        ProjectExportFile missingName = validFile();
        missingName.getProject().setName("  ");
        assertValidation(missingName, "项目名称不能为空");

        ProjectExportFile missingPhase = validFile();
        missingPhase.getTasks().get(0).setPhaseId(99L);
        assertValidation(missingPhase, "任务引用了不存在的阶段");

        when(projectMapper.selectCount(any())).thenReturn(1L);
        assertBusiness(validFile(), "已存在同名项目，不能覆盖");
        verify(projectMapper, never()).insert(any(Project.class));
    }

    private void assertValidation(ProjectExportFile file, String message) {
        assertThatThrownBy(() -> projectImportService.importProject(file))
                .isInstanceOf(BusinessException.class)
                .hasMessage(message)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    private void assertBusiness(ProjectExportFile file, String message) {
        assertThatThrownBy(() -> projectImportService.importProject(file))
                .isInstanceOf(BusinessException.class)
                .hasMessage(message)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.BUSINESS_ERROR));
    }

    private ProjectExportFile validFile() {
        ProjectExportFile file = new ProjectExportFile();
        file.setFormatVersion(ProjectExportFile.FORMAT_VERSION);
        ProjectExportFile.ProjectItem project = new ProjectExportFile.ProjectItem();
        project.setId(8L);
        project.setName(" 研发罗盘 ");
        project.setStatus("IN_PROGRESS");
        project.setTags("后端,Java");
        file.setProject(project);
        ProjectExportFile.PhaseItem phase = new ProjectExportFile.PhaseItem();
        phase.setId(3L);
        phase.setName("开发实现");
        phase.setSortOrder(1);
        file.setPhases(List.of(phase));
        ProjectExportFile.TaskItem task = new ProjectExportFile.TaskItem();
        task.setId(15L);
        task.setPhaseId(3L);
        task.setTitle("导出项目");
        task.setStatus("TODO");
        task.setPriority("MEDIUM");
        file.setTasks(List.of(task));
        ProjectExportFile.WorkLogItem workLog = new ProjectExportFile.WorkLogItem();
        workLog.setTaskId(15L);
        workLog.setLogDate(LocalDate.of(2026, 9, 22));
        workLog.setSummaryContent("完成导入");
        file.setWorkLogs(List.of(workLog));
        ProjectExportFile.ActivityItem activity = new ProjectExportFile.ActivityItem();
        activity.setObjectType("TASK");
        activity.setObjectId(15L);
        activity.setAction("CREATED");
        activity.setSummary("创建任务「导出项目」");
        file.setActivities(List.of(activity));
        ProjectExportFile.AttachmentItem attachment = new ProjectExportFile.AttachmentItem();
        attachment.setOriginalFileName("设计图.png");
        file.setAttachments(List.of(attachment));
        return file;
    }
}
