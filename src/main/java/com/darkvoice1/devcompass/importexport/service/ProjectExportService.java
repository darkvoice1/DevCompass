package com.darkvoice1.devcompass.importexport.service;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.darkvoice1.devcompass.activity.entity.Activity;
import com.darkvoice1.devcompass.activity.repository.ActivityMapper;
import com.darkvoice1.devcompass.attachment.entity.Attachment;
import com.darkvoice1.devcompass.attachment.repository.AttachmentMapper;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile.ActivityItem;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile.AttachmentItem;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile.PhaseItem;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile.ProjectItem;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile.TaskItem;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile.WorkLogItem;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectPhase;
import com.darkvoice1.devcompass.project.repository.ProjectMapper;
import com.darkvoice1.devcompass.project.repository.ProjectPhaseMapper;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.repository.TaskMapper;
import com.darkvoice1.devcompass.worklog.entity.WorkLog;
import com.darkvoice1.devcompass.worklog.repository.WorkLogMapper;

/**
 * 把一个项目当前可见的资料导出成 JSON 或任务 CSV。
 */
@Service
public class ProjectExportService {

    private static final String CSV_HEADER = "标题,状态,所属阶段名称,优先级,截止日期,是否阻塞";

    private final ProjectMapper projectMapper;

    private final ProjectPhaseMapper projectPhaseMapper;

    private final TaskMapper taskMapper;

    private final WorkLogMapper workLogMapper;

    private final ActivityMapper activityMapper;

    private final AttachmentMapper attachmentMapper;

    private final Clock clock;

    /**
     * 创建项目导出服务。
     *
     * @param projectMapper 项目数据访问对象
     * @param projectPhaseMapper 阶段数据访问对象
     * @param taskMapper 任务数据访问对象
     * @param workLogMapper 工作日志数据访问对象
     * @param activityMapper 动态数据访问对象
     * @param attachmentMapper 附件数据访问对象
     * @param clock 应用时钟
     */
    public ProjectExportService(ProjectMapper projectMapper, ProjectPhaseMapper projectPhaseMapper,
            TaskMapper taskMapper, WorkLogMapper workLogMapper, ActivityMapper activityMapper,
            AttachmentMapper attachmentMapper, Clock clock) {
        this.projectMapper = projectMapper;
        this.projectPhaseMapper = projectPhaseMapper;
        this.taskMapper = taskMapper;
        this.workLogMapper = workLogMapper;
        this.activityMapper = activityMapper;
        this.attachmentMapper = attachmentMapper;
        this.clock = clock;
    }

    /**
     * 导出指定项目。已删除项目拒绝，已归档项目可以导出。
     *
     * @param projectId 项目主键
     * @return 导出文件内容
     */
    public ProjectExportFile exportProject(Long projectId) {
        Project project = requireProject(projectId);
        List<ProjectPhase> phases = findPhases(projectId);
        List<Task> tasks = findTasks(projectId);
        ProjectExportFile file = new ProjectExportFile();
        file.setFormatVersion(ProjectExportFile.FORMAT_VERSION);
        file.setExportedAt(clock.instant());
        file.setProject(toProjectItem(project));
        file.setPhases(phases.stream().map(this::toPhaseItem).toList());
        file.setTasks(tasks.stream().map(this::toTaskItem).toList());
        file.setWorkLogs(findWorkLogs(tasks).stream().map(this::toWorkLogItem).toList());
        file.setActivities(findActivities(projectId).stream().map(this::toActivityItem).toList());
        file.setAttachments(findAttachments(projectId).stream().map(this::toAttachmentItem).toList());
        return file;
    }

    /**
     * 把项目中未删除的任务导出成 CSV。阶段只写名称。
     *
     * @param projectId 项目主键
     * @return 带编码标记的 CSV 文本
     */
    public String exportTasksCsv(Long projectId) {
        requireProject(projectId);
        Map<Long, String> phaseNames = new HashMap<>();
        for (ProjectPhase phase : findPhases(projectId)) {
            phaseNames.put(phase.getId(), phase.getName());
        }
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append(CSV_HEADER).append("\r\n");
        for (Task task : findTasks(projectId)) {
            String phaseName = task.getPhaseId() == null ? "" : phaseNames.getOrDefault(task.getPhaseId(), "");
            csv.append(csvField(task.getTitle())).append(',')
                    .append(csvField(statusLabel(task))).append(',')
                    .append(csvField(phaseName)).append(',')
                    .append(csvField(priorityLabel(task))).append(',')
                    .append(csvField(task.getDueDate() == null ? "" : task.getDueDate().toString())).append(',')
                    .append(csvField(task.isBlocked() ? "是" : "否"))
                    .append("\r\n");
        }
        return csv.toString();
    }

    /**
     * 查询项目。已删除项目查不到，已归档项目仍然可以导出。
     *
     * @param projectId 项目主键
     * @return 项目
     */
    private Project requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "项目不存在");
        }
        return project;
    }

    /**
     * 按排序号读取未删除阶段。
     *
     * @param projectId 项目主键
     * @return 阶段列表
     */
    private List<ProjectPhase> findPhases(Long projectId) {
        QueryWrapper<ProjectPhase> query = new QueryWrapper<>();
        query.eq("project_id", projectId).orderByAsc("sort_order").orderByAsc("id");
        return projectPhaseMapper.selectList(query);
    }

    /**
     * 按编号读取未删除任务。
     *
     * @param projectId 项目主键
     * @return 任务列表
     */
    private List<Task> findTasks(Long projectId) {
        QueryWrapper<Task> query = new QueryWrapper<>();
        query.eq("project_id", projectId).orderByAsc("id");
        return taskMapper.selectList(query);
    }

    /**
     * 读取这些任务下未删除的工作日志。没有任务时不再查询。
     *
     * @param tasks 项目任务
     * @return 工作日志列表
     */
    private List<WorkLog> findWorkLogs(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        List<Long> taskIds = tasks.stream().map(task -> task.getId()).toList();
        QueryWrapper<WorkLog> query = new QueryWrapper<>();
        query.in("task_id", taskIds).orderByAsc("log_date").orderByAsc("id");
        return workLogMapper.selectList(query);
    }

    /**
     * 按发生时间读取项目动态。
     *
     * @param projectId 项目主键
     * @return 动态列表
     */
    private List<Activity> findActivities(Long projectId) {
        QueryWrapper<Activity> query = new QueryWrapper<>();
        query.eq("project_id", projectId).orderByAsc("created_at").orderByAsc("id");
        return activityMapper.selectList(query);
    }

    /**
     * 按上传时间读取未删除附件。
     *
     * @param projectId 项目主键
     * @return 附件列表
     */
    private List<Attachment> findAttachments(Long projectId) {
        QueryWrapper<Attachment> query = new QueryWrapper<>();
        query.eq("project_id", projectId).orderByAsc("created_at").orderByAsc("id");
        return attachmentMapper.selectList(query);
    }

    private ProjectItem toProjectItem(Project project) {
        ProjectItem item = new ProjectItem();
        item.setId(project.getId());
        item.setName(project.getName());
        item.setDescription(project.getDescription());
        item.setStatus(project.getStatus() == null ? null : project.getStatus().name());
        item.setProgressMode(project.getProgressMode() == null ? null : project.getProgressMode().name());
        item.setAutoProgress(project.getAutoProgress());
        item.setManualProgress(project.getManualProgress());
        item.setProgressReason(project.getProgressReason());
        item.setTargetDate(project.getTargetDate());
        item.setTechStack(project.getTechStack());
        item.setTags(project.getTags());
        item.setArchived(project.isArchived());
        item.setArchivedAt(project.getArchivedAt());
        item.setCreatedAt(project.getCreatedAt());
        item.setUpdatedAt(project.getUpdatedAt());
        return item;
    }

    private PhaseItem toPhaseItem(ProjectPhase phase) {
        PhaseItem item = new PhaseItem();
        item.setId(phase.getId());
        item.setName(phase.getName());
        item.setDescription(phase.getDescription());
        item.setSortOrder(phase.getSortOrder());
        item.setCreatedAt(phase.getCreatedAt());
        item.setUpdatedAt(phase.getUpdatedAt());
        return item;
    }

    private TaskItem toTaskItem(Task task) {
        TaskItem item = new TaskItem();
        item.setId(task.getId());
        item.setPhaseId(task.getPhaseId());
        item.setTitle(task.getTitle());
        item.setDescription(task.getDescription());
        item.setStatus(task.getStatus() == null ? null : task.getStatus().name());
        item.setPriority(task.getPriority() == null ? null : task.getPriority().name());
        item.setDueDate(task.getDueDate());
        item.setEstimatedHours(task.getEstimatedHours());
        item.setBlocked(task.isBlocked());
        item.setBlockerReason(task.getBlockerReason());
        item.setCreatedAt(task.getCreatedAt());
        item.setUpdatedAt(task.getUpdatedAt());
        return item;
    }

    private WorkLogItem toWorkLogItem(WorkLog workLog) {
        WorkLogItem item = new WorkLogItem();
        item.setId(workLog.getId());
        item.setTaskId(workLog.getTaskId());
        item.setLogDate(workLog.getLogDate());
        item.setPlanContent(workLog.getPlanContent());
        item.setSummaryContent(workLog.getSummaryContent());
        item.setCommitHashes(workLog.getCommitHashes());
        item.setSpentMinutes(workLog.getSpentMinutes());
        item.setBlockerReason(workLog.getBlockerReason());
        item.setCreatedAt(workLog.getCreatedAt());
        item.setUpdatedAt(workLog.getUpdatedAt());
        return item;
    }

    private ActivityItem toActivityItem(Activity activity) {
        ActivityItem item = new ActivityItem();
        item.setId(activity.getId());
        item.setObjectType(activity.getObjectType() == null ? null : activity.getObjectType().name());
        item.setObjectId(activity.getObjectId());
        item.setAction(activity.getAction() == null ? null : activity.getAction().name());
        item.setSummary(activity.getSummary());
        item.setCreatedAt(activity.getCreatedAt());
        return item;
    }

    /**
     * 把任务状态写成中文。
     *
     * @param task 任务
     * @return 状态文字
     */
    private String statusLabel(Task task) {
        if (task.getStatus() == null) {
            return "";
        }
        return switch (task.getStatus()) {
            case TODO -> "待办";
            case IN_PROGRESS -> "进行中";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
        };
    }

    /**
     * 把任务优先级写成中文。
     *
     * @param task 任务
     * @return 优先级文字
     */
    private String priorityLabel(Task task) {
        if (task.getPriority() == null) {
            return "";
        }
        return switch (task.getPriority()) {
            case LOW -> "低";
            case MEDIUM -> "中";
            case HIGH -> "高";
            case URGENT -> "紧急";
        };
    }

    /**
     * 按 CSV 规则转义单元格。含逗号、引号或换行时用引号包起来。
     *
     * @param value 单元格原文
     * @return 可以放进 CSV 的文本
     */
    private String csvField(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean mustQuote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        if (!mustQuote) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /**
     * 只复制附件登记，不复制存储编号。
     *
     * @param attachment 附件实体
     * @return 导出用的附件信息
     */
    private AttachmentItem toAttachmentItem(Attachment attachment) {
        AttachmentItem item = new AttachmentItem();
        item.setOriginalFileName(attachment.getOriginalFileName());
        item.setContentType(attachment.getContentType());
        item.setSizeBytes(attachment.getSizeBytes());
        item.setCreatedAt(attachment.getCreatedAt());
        return item;
    }
}
