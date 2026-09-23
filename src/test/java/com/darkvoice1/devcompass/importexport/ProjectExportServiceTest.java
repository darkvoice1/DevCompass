package com.darkvoice1.devcompass.importexport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.darkvoice1.devcompass.activity.entity.Activity;
import com.darkvoice1.devcompass.activity.entity.ActivityAction;
import com.darkvoice1.devcompass.activity.entity.ActivityObjectType;
import com.darkvoice1.devcompass.activity.repository.ActivityMapper;
import com.darkvoice1.devcompass.attachment.entity.Attachment;
import com.darkvoice1.devcompass.attachment.repository.AttachmentMapper;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile;
import com.darkvoice1.devcompass.importexport.service.ProjectExportService;
import com.darkvoice1.devcompass.project.entity.ProgressMode;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectPhase;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.repository.ProjectPhaseMapper;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.entity.TaskPriority;
import com.darkvoice1.devcompass.task.entity.TaskStatus;
import com.darkvoice1.devcompass.task.repository.TaskMapper;
import com.darkvoice1.devcompass.worklog.entity.WorkLog;
import com.darkvoice1.devcompass.worklog.repository.WorkLogMapper;

/**
 * 验证项目 JSON 导出的内容和查询范围。
 */
class ProjectExportServiceTest {

    private ProjectMapper projectMapper;

    private ProjectPhaseMapper projectPhaseMapper;

    private TaskMapper taskMapper;

    private WorkLogMapper workLogMapper;

    private ActivityMapper activityMapper;

    private AttachmentMapper attachmentMapper;

    private ProjectExportService projectExportService;

    /**
     * 使用固定时钟初始化导出服务。
     */
    @BeforeEach
    void setUp() {
        projectMapper = mock(ProjectMapper.class);
        projectPhaseMapper = mock(ProjectPhaseMapper.class);
        taskMapper = mock(TaskMapper.class);
        workLogMapper = mock(WorkLogMapper.class);
        activityMapper = mock(ActivityMapper.class);
        attachmentMapper = mock(AttachmentMapper.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-22T02:00:00Z"), ZoneId.of("Asia/Shanghai"));
        projectExportService = new ProjectExportService(projectMapper, projectPhaseMapper, taskMapper,
                workLogMapper, activityMapper, attachmentMapper, clock);
    }

    /**
     * 验证归档项目也能导出，并且附件不含存储编号。
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldExportVisibleProjectData() {
        when(projectMapper.selectById(8L)).thenReturn(project());
        when(projectPhaseMapper.selectList(any())).thenReturn(List.of(phase()));
        when(taskMapper.selectList(any())).thenReturn(List.of(task()));
        when(workLogMapper.selectList(any())).thenReturn(List.of(workLog()));
        when(activityMapper.selectList(any())).thenReturn(List.of(activity()));
        when(attachmentMapper.selectList(any())).thenReturn(List.of(attachment()));

        ProjectExportFile file = projectExportService.exportProject(8L);

        assertThat(file.getFormatVersion()).isEqualTo(1);
        assertThat(file.getExportedAt()).isEqualTo(Instant.parse("2026-09-22T02:00:00Z"));
        assertThat(file.getProject().getName()).isEqualTo("研发罗盘");
        assertThat(file.getProject().getTags()).isEqualTo("后端,Java");
        assertThat(file.getProject().isArchived()).isTrue();
        assertThat(file.getProject().getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(file.getPhases()).extracting(item -> item.getName()).containsExactly("开发实现");
        assertThat(file.getTasks()).extracting(item -> item.getTitle()).containsExactly("导出项目");
        assertThat(file.getTasks().get(0).getPhaseId()).isEqualTo(3L);
        assertThat(file.getWorkLogs()).extracting(item -> item.getTaskId()).containsExactly(15L);
        assertThat(file.getActivities()).extracting(item -> item.getSummary()).containsExactly("创建项目「研发罗盘」");
        assertThat(file.getAttachments()).extracting(item -> item.getOriginalFileName()).containsExactly("设计图.png");
        assertThat(file.getAttachments().get(0).getContentType()).isEqualTo("image/png");
        assertThat(file.getAttachments().get(0).getSizeBytes()).isEqualTo(3L);

        ArgumentCaptor<QueryWrapper<WorkLog>> workLogQuery = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(workLogMapper).selectList(workLogQuery.capture());
        assertThat(workLogQuery.getValue().getCustomSqlSegment()).contains("task_id");
    }

    /**
     * 验证没有任务时不查询工作日志。
     */
    @Test
    void shouldSkipWorkLogsWhenProjectHasNoTask() {
        when(projectMapper.selectById(8L)).thenReturn(project());
        when(projectPhaseMapper.selectList(any())).thenReturn(List.of());
        when(taskMapper.selectList(any())).thenReturn(List.of());
        when(activityMapper.selectList(any())).thenReturn(List.of());
        when(attachmentMapper.selectList(any())).thenReturn(List.of());

        ProjectExportFile file = projectExportService.exportProject(8L);

        assertThat(file.getTasks()).isEmpty();
        assertThat(file.getWorkLogs()).isEmpty();
        verify(workLogMapper, never()).selectList(any());
    }

    /**
     * 验证任务 CSV 使用中文表头，并写出阶段名称。
     */
    @Test
    void shouldExportTasksAsCsv() {
        when(projectMapper.selectById(8L)).thenReturn(project());
        when(projectPhaseMapper.selectList(any())).thenReturn(List.of(phase()));
        Task quoted = task();
        quoted.setId(16L);
        quoted.setTitle("设计,评审");
        quoted.setPhaseId(null);
        quoted.setBlocked(true);
        quoted.setStatus(TaskStatus.IN_PROGRESS);
        quoted.setPriority(TaskPriority.HIGH);
        quoted.setDueDate(null);
        when(taskMapper.selectList(any())).thenReturn(List.of(task(), quoted));

        String csv = projectExportService.exportTasksCsv(8L);

        assertThat(csv).startsWith("\uFEFF标题,状态,所属阶段名称,优先级,截止日期,是否阻塞\r\n");
        assertThat(csv).contains("导出项目,待办,开发实现,中,2026-09-30,否\r\n");
        assertThat(csv).contains("\"设计,评审\",进行中,,高,,是\r\n");
    }

    /**
     * 验证没有任务时 CSV 仍保留表头。
     */
    @Test
    void shouldExportCsvHeaderWhenProjectHasNoTask() {
        when(projectMapper.selectById(8L)).thenReturn(project());
        when(projectPhaseMapper.selectList(any())).thenReturn(List.of());
        when(taskMapper.selectList(any())).thenReturn(List.of());

        String csv = projectExportService.exportTasksCsv(8L);

        assertThat(csv).isEqualTo("\uFEFF标题,状态,所属阶段名称,优先级,截止日期,是否阻塞\r\n");
    }

    /**
     * 验证项目不存在时拒绝导出。
     */
    @Test
    void shouldRejectMissingProject() {
        when(projectMapper.selectById(8L)).thenReturn(null);

        assertThatThrownBy(() -> projectExportService.exportProject(8L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目不存在");
        assertThatThrownBy(() -> projectExportService.exportTasksCsv(8L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目不存在");
        verify(taskMapper, never()).selectList(any());
    }

    private Project project() {
        Project project = new Project();
        project.setId(8L);
        project.setName("研发罗盘");
        project.setDescription("个人研发管理");
        project.setStatus(ProjectStatus.IN_PROGRESS);
        project.setProgressMode(ProgressMode.AUTO);
        project.setAutoProgress(20);
        project.setTags("后端,Java");
        project.setArchived(true);
        project.setArchivedAt(Instant.parse("2026-09-21T00:00:00Z"));
        return project;
    }

    private ProjectPhase phase() {
        ProjectPhase phase = new ProjectPhase();
        phase.setId(3L);
        phase.setProjectId(8L);
        phase.setName("开发实现");
        phase.setSortOrder(1);
        return phase;
    }

    private Task task() {
        Task task = new Task();
        task.setId(15L);
        task.setProjectId(8L);
        task.setPhaseId(3L);
        task.setTitle("导出项目");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.MEDIUM);
        task.setDueDate(LocalDate.of(2026, 9, 30));
        return task;
    }

    private WorkLog workLog() {
        WorkLog workLog = new WorkLog();
        workLog.setId(20L);
        workLog.setTaskId(15L);
        workLog.setLogDate(LocalDate.of(2026, 9, 22));
        workLog.setSummaryContent("完成导出格式");
        return workLog;
    }

    private Activity activity() {
        Activity activity = new Activity();
        activity.setId(4L);
        activity.setProjectId(8L);
        activity.setObjectType(ActivityObjectType.PROJECT);
        activity.setObjectId(8L);
        activity.setAction(ActivityAction.CREATED);
        activity.setSummary("创建项目「研发罗盘」");
        activity.setCreatedAt(Instant.parse("2026-09-20T00:00:00Z"));
        return activity;
    }

    private Attachment attachment() {
        Attachment attachment = new Attachment();
        attachment.setId(9L);
        attachment.setProjectId(8L);
        attachment.setOriginalFileName("设计图.png");
        attachment.setContentType("image/png");
        attachment.setSizeBytes(3L);
        attachment.setStorageKey("11111111-1111-1111-1111-111111111111");
        attachment.setCreatedAt(Instant.parse("2026-09-22T01:00:00Z"));
        return attachment;
    }
}
