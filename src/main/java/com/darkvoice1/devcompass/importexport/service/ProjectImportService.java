package com.darkvoice1.devcompass.importexport.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.darkvoice1.devcompass.activity.entity.Activity;
import com.darkvoice1.devcompass.activity.entity.ActivityAction;
import com.darkvoice1.devcompass.activity.entity.ActivityObjectType;
import com.darkvoice1.devcompass.activity.repository.ActivityMapper;
import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile.ActivityItem;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile.PhaseItem;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile.ProjectItem;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile.TaskItem;
import com.darkvoice1.devcompass.importexport.dto.ProjectExportFile.WorkLogItem;
import com.darkvoice1.devcompass.importexport.dto.ProjectImportResponse;
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
 * 把导出的项目 JSON 导入成一个新项目。
 */
@Service
public class ProjectImportService {

    private static final Pattern COMMIT_HASHES = Pattern.compile(
            "^[0-9a-fA-F]{7,12}(\\s*,\\s*[0-9a-fA-F]{7,12})*$");

    private final ProjectMapper projectMapper;

    private final ProjectPhaseMapper projectPhaseMapper;

    private final TaskMapper taskMapper;

    private final WorkLogMapper workLogMapper;

    private final ActivityMapper activityMapper;

    /**
     * 创建项目导入服务。
     *
     * @param projectMapper 项目数据访问对象
     * @param projectPhaseMapper 阶段数据访问对象
     * @param taskMapper 任务数据访问对象
     * @param workLogMapper 工作日志数据访问对象
     * @param activityMapper 动态数据访问对象
     */
    public ProjectImportService(ProjectMapper projectMapper, ProjectPhaseMapper projectPhaseMapper,
            TaskMapper taskMapper, WorkLogMapper workLogMapper, ActivityMapper activityMapper) {
        this.projectMapper = projectMapper;
        this.projectPhaseMapper = projectPhaseMapper;
        this.taskMapper = taskMapper;
        this.workLogMapper = workLogMapper;
        this.activityMapper = activityMapper;
    }

    /**
     * 校验并导入一份项目文件。任何一项不合法时，不会留下半份新项目。
     *
     * @param file 导出的 JSON 内容
     * @return 新项目编号和跳过的附件数量
     */
    @Transactional
    public ProjectImportResponse importProject(ProjectExportFile file) {
        if (file == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "导入内容不能为空");
        }
        if (file.getFormatVersion() != ProjectExportFile.FORMAT_VERSION) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "导入文件版本不正确");
        }
        ProjectItem source = file.getProject();
        if (source == null || source.getName() == null || source.getName().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "项目名称不能为空");
        }
        String projectName = source.getName().trim();
        if (projectName.length() > 200) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "项目名称长度不能超过200个字符");
        }
        if (projectMapper.selectCount(new QueryWrapper<Project>().eq("name", projectName)) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已存在同名项目，不能覆盖");
        }

        List<PhaseItem> phases = listOf(file.getPhases());
        List<TaskItem> tasks = listOf(file.getTasks());
        List<WorkLogItem> workLogs = listOf(file.getWorkLogs());
        List<ActivityItem> activities = listOf(file.getActivities());
        Set<Long> phaseIds = validatePhases(phases);
        Set<Long> taskIds = validateTasks(tasks, phaseIds);
        validateWorkLogs(workLogs, taskIds);
        validateActivities(activities, phaseIds, taskIds);

        Project project = insertProject(source, projectName);
        Map<Long, Long> newPhaseIds = insertPhases(project.getId(), phases);
        Map<Long, Long> newTaskIds = insertTasks(project.getId(), tasks, newPhaseIds);
        insertWorkLogs(workLogs, newTaskIds);
        insertActivities(project.getId(), activities, newPhaseIds, newTaskIds);

        ProjectImportResponse response = new ProjectImportResponse();
        response.setProjectId(project.getId());
        response.setProjectName(projectName);
        response.setSkippedAttachmentCount(listOf(file.getAttachments()).size());
        return response;
    }

    /**
     * 检查阶段编号和名称。编号要能在后面把任务连回来。
     *
     * @param phases 文件中的阶段
     * @return 文件里的阶段编号
     */
    private Set<Long> validatePhases(List<PhaseItem> phases) {
        Set<Long> ids = new HashSet<>();
        for (PhaseItem phase : phases) {
            if (phase.getId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "阶段编号不能为空");
            }
            if (!ids.add(phase.getId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "阶段编号重复");
            }
            if (phase.getName() == null || phase.getName().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "阶段名称不能为空");
            }
            if (phase.getName().trim().length() > 200) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "阶段名称长度不能超过200个字符");
            }
            if (phase.getSortOrder() != null && phase.getSortOrder() < 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "阶段排序不能小于0");
            }
        }
        return ids;
    }

    /**
     * 检查任务标题、状态，以及它引用的阶段是否在文件里。
     *
     * @param tasks 文件中的任务
     * @param phaseIds 文件里的阶段编号
     * @return 文件里的任务编号
     */
    private Set<Long> validateTasks(List<TaskItem> tasks, Set<Long> phaseIds) {
        Set<Long> ids = new HashSet<>();
        for (TaskItem task : tasks) {
            if (task.getId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "任务编号不能为空");
            }
            if (!ids.add(task.getId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "任务编号重复");
            }
            if (task.getTitle() == null || task.getTitle().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "任务标题不能为空");
            }
            if (task.getTitle().trim().length() > 200) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "任务标题长度不能超过200个字符");
            }
            if (task.getPhaseId() != null && !phaseIds.contains(task.getPhaseId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "任务引用了不存在的阶段");
            }
            parseEnum(TaskStatus.class, task.getStatus(), "任务状态不正确");
            parseEnum(TaskPriority.class, task.getPriority(), "任务优先级不正确");
            if (task.getEstimatedHours() != null && task.getEstimatedHours() < 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "任务预计工时不能小于0");
            }
        }
        return ids;
    }

    /**
     * 检查工作日志。一个任务最多一条，提交编号要符合现有格式。
     *
     * @param workLogs 文件中的工作日志
     * @param taskIds 文件里的任务编号
     */
    private void validateWorkLogs(List<WorkLogItem> workLogs, Set<Long> taskIds) {
        Set<Long> seenTaskIds = new HashSet<>();
        for (WorkLogItem workLog : workLogs) {
            if (workLog.getTaskId() == null || !taskIds.contains(workLog.getTaskId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "工作日志引用了不存在的任务");
            }
            if (!seenTaskIds.add(workLog.getTaskId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "同一任务不能有多条工作日志");
            }
            if (workLog.getLogDate() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "工作日志日期不能为空");
            }
            if (workLog.getSpentMinutes() != null && workLog.getSpentMinutes() < 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "工作日志花费时间不能小于0");
            }
            if (workLog.getCommitHashes() != null && !workLog.getCommitHashes().isBlank()
                    && !COMMIT_HASHES.matcher(workLog.getCommitHashes().trim()).matches()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "工作日志提交编号格式不正确");
            }
        }
    }

    /**
     * 检查动态的类型、动作，以及它指向的旧编号是否在文件里。
     *
     * @param activities 文件中的动态
     * @param phaseIds 文件里的阶段编号
     * @param taskIds 文件里的任务编号
     */
    private void validateActivities(List<ActivityItem> activities, Set<Long> phaseIds, Set<Long> taskIds) {
        for (ActivityItem activity : activities) {
            ActivityObjectType objectType = parseEnum(ActivityObjectType.class, activity.getObjectType(), "动态对象类型不正确");
            if (objectType == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "动态对象类型不正确");
            }
            if (parseEnum(ActivityAction.class, activity.getAction(), "动态动作不正确") == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "动态动作不正确");
            }
            if (activity.getSummary() == null || activity.getSummary().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "动态摘要不能为空");
            }
            if (activity.getSummary().trim().length() > 500) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "动态摘要长度不能超过500个字符");
            }
            if (activity.getObjectId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "动态引用了不存在的对象");
            }
            boolean known = switch (objectType) {
                case PROJECT -> true;
                case PHASE -> phaseIds.contains(activity.getObjectId());
                case TASK -> taskIds.contains(activity.getObjectId());
            };
            if (!known) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "动态引用了不存在的对象");
            }
        }
    }

    /**
     * 写入新项目。数据库重新分配编号。
     *
     * @param source 文件中的项目
     * @param projectName 去掉首尾空白后的名称
     * @return 已保存的项目
     */
    private Project insertProject(ProjectItem source, String projectName) {
        ProjectStatus status = parseEnum(ProjectStatus.class, source.getStatus(), "项目状态不正确");
        ProgressMode progressMode = parseEnum(ProgressMode.class, source.getProgressMode(), "项目进度模式不正确");
        if (source.getAutoProgress() != null && (source.getAutoProgress() < 0 || source.getAutoProgress() > 100)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "项目进度必须在0到100之间");
        }
        if (source.getManualProgress() != null && (source.getManualProgress() < 0 || source.getManualProgress() > 100)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "项目进度必须在0到100之间");
        }
        Project project = new Project();
        project.setName(projectName);
        project.setDescription(source.getDescription());
        project.setStatus(status == null ? ProjectStatus.PLANNED : status);
        project.setProgressMode(progressMode == null ? ProgressMode.AUTO : progressMode);
        project.setAutoProgress(source.getAutoProgress() == null ? 0 : source.getAutoProgress());
        project.setManualProgress(source.getManualProgress());
        project.setProgressReason(source.getProgressReason());
        project.setTargetDate(source.getTargetDate());
        project.setTechStack(source.getTechStack());
        project.setTags(source.getTags());
        project.setArchived(source.isArchived());
        project.setArchivedAt(source.getArchivedAt());
        project.setCreatedAt(source.getCreatedAt());
        project.setUpdatedAt(source.getUpdatedAt());
        projectMapper.insert(project);
        return project;
    }

    /**
     * 写入阶段，并记下旧编号到新编号的对应关系。
     *
     * @param projectId 新项目编号
     * @param phases 文件中的阶段
     * @return 旧编号到新编号
     */
    private Map<Long, Long> insertPhases(Long projectId, List<PhaseItem> phases) {
        Map<Long, Long> newIds = new HashMap<>();
        for (PhaseItem source : phases) {
            ProjectPhase phase = new ProjectPhase();
            phase.setProjectId(projectId);
            phase.setName(source.getName().trim());
            phase.setDescription(source.getDescription());
            phase.setSortOrder(source.getSortOrder() == null ? 0 : source.getSortOrder());
            phase.setCreatedAt(source.getCreatedAt());
            phase.setUpdatedAt(source.getUpdatedAt());
            projectPhaseMapper.insert(phase);
            newIds.put(source.getId(), phase.getId());
        }
        return newIds;
    }

    /**
     * 写入任务，并把阶段编号换成新编号。
     *
     * @param projectId 新项目编号
     * @param tasks 文件中的任务
     * @param newPhaseIds 阶段编号对照
     * @return 旧任务编号到新编号
     */
    private Map<Long, Long> insertTasks(Long projectId, List<TaskItem> tasks, Map<Long, Long> newPhaseIds) {
        Map<Long, Long> newIds = new HashMap<>();
        for (TaskItem source : tasks) {
            TaskStatus status = parseEnum(TaskStatus.class, source.getStatus(), "任务状态不正确");
            TaskPriority priority = parseEnum(TaskPriority.class, source.getPriority(), "任务优先级不正确");
            Task task = new Task();
            task.setProjectId(projectId);
            task.setPhaseId(source.getPhaseId() == null ? null : newPhaseIds.get(source.getPhaseId()));
            task.setTitle(source.getTitle().trim());
            task.setDescription(source.getDescription());
            task.setStatus(status == null ? TaskStatus.TODO : status);
            task.setPriority(priority == null ? TaskPriority.MEDIUM : priority);
            task.setDueDate(source.getDueDate());
            task.setEstimatedHours(source.getEstimatedHours());
            task.setBlocked(source.isBlocked());
            task.setBlockerReason(source.getBlockerReason());
            task.setCreatedAt(source.getCreatedAt());
            task.setUpdatedAt(source.getUpdatedAt());
            taskMapper.insert(task);
            newIds.put(source.getId(), task.getId());
        }
        return newIds;
    }

    /**
     * 写入工作日志，并把任务编号换成新编号。
     *
     * @param workLogs 文件中的工作日志
     * @param newTaskIds 任务编号对照
     */
    private void insertWorkLogs(List<WorkLogItem> workLogs, Map<Long, Long> newTaskIds) {
        for (WorkLogItem source : workLogs) {
            WorkLog workLog = new WorkLog();
            workLog.setTaskId(newTaskIds.get(source.getTaskId()));
            workLog.setLogDate(source.getLogDate());
            workLog.setPlanContent(source.getPlanContent());
            workLog.setSummaryContent(source.getSummaryContent());
            workLog.setCommitHashes(blankToNull(source.getCommitHashes()));
            workLog.setSpentMinutes(source.getSpentMinutes() == null ? 0 : source.getSpentMinutes());
            workLog.setBlockerReason(source.getBlockerReason());
            workLog.setCreatedAt(source.getCreatedAt());
            workLog.setUpdatedAt(source.getUpdatedAt());
            workLogMapper.insert(workLog);
        }
    }

    /**
     * 写入动态。项目、阶段和任务都改成新编号。
     *
     * @param projectId 新项目编号
     * @param activities 文件中的动态
     * @param newPhaseIds 阶段编号对照
     * @param newTaskIds 任务编号对照
     */
    private void insertActivities(Long projectId, List<ActivityItem> activities,
            Map<Long, Long> newPhaseIds, Map<Long, Long> newTaskIds) {
        for (ActivityItem source : activities) {
            ActivityObjectType objectType = parseEnum(ActivityObjectType.class, source.getObjectType(), "动态对象类型不正确");
            ActivityAction action = parseEnum(ActivityAction.class, source.getAction(), "动态动作不正确");
            Activity activity = new Activity();
            activity.setProjectId(projectId);
            activity.setObjectType(objectType);
            activity.setObjectId(switch (objectType) {
                case PROJECT -> projectId;
                case PHASE -> newPhaseIds.get(source.getObjectId());
                case TASK -> newTaskIds.get(source.getObjectId());
            });
            activity.setAction(action);
            activity.setSummary(source.getSummary().trim());
            activity.setCreatedAt(source.getCreatedAt());
            activityMapper.insert(activity);
        }
    }

    /**
     * 把文字转成枚举。空值留给调用方决定默认值。
     *
     * @param type 枚举类型
     * @param value 文件中的文字
     * @param message 无法识别时的提示
     * @return 枚举值，空文字时返回 null
     */
    private <T extends Enum<T>> T parseEnum(Class<T> type, String value, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
    }

    private <T> List<T> listOf(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
