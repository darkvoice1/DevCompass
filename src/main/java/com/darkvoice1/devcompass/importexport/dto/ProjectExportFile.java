package com.darkvoice1.devcompass.importexport.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 一个项目的导出文件。附件只含名称、类型和大小，不含文件内容。
 */
public class ProjectExportFile {

    /**
     * 当前导出格式版本。导入时必须对上这个数字。
     */
    public static final int FORMAT_VERSION = 1;

    private int formatVersion;

    private Instant exportedAt;

    private ProjectItem project;

    private List<PhaseItem> phases = new ArrayList<>();

    private List<TaskItem> tasks = new ArrayList<>();

    private List<WorkLogItem> workLogs = new ArrayList<>();

    private List<ActivityItem> activities = new ArrayList<>();

    private List<AttachmentItem> attachments = new ArrayList<>();

    public int getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(int formatVersion) {
        this.formatVersion = formatVersion;
    }

    public Instant getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(Instant exportedAt) {
        this.exportedAt = exportedAt;
    }

    public ProjectItem getProject() {
        return project;
    }

    public void setProject(ProjectItem project) {
        this.project = project;
    }

    public List<PhaseItem> getPhases() {
        return phases;
    }

    public void setPhases(List<PhaseItem> phases) {
        this.phases = phases;
    }

    public List<TaskItem> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskItem> tasks) {
        this.tasks = tasks;
    }

    public List<WorkLogItem> getWorkLogs() {
        return workLogs;
    }

    public void setWorkLogs(List<WorkLogItem> workLogs) {
        this.workLogs = workLogs;
    }

    public List<ActivityItem> getActivities() {
        return activities;
    }

    public void setActivities(List<ActivityItem> activities) {
        this.activities = activities;
    }

    public List<AttachmentItem> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentItem> attachments) {
        this.attachments = attachments;
    }

    /**
     * 导出文件中的项目。
     */
    public static class ProjectItem {

        private Long id;

        private String name;

        private String description;

        private String status;

        private String progressMode;

        private Integer autoProgress;

        private Integer manualProgress;

        private String progressReason;

        private LocalDate targetDate;

        private String techStack;

        private String tags;

        private boolean archived;

        private Instant archivedAt;

        private Instant createdAt;

        private Instant updatedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getProgressMode() {
            return progressMode;
        }

        public void setProgressMode(String progressMode) {
            this.progressMode = progressMode;
        }

        public Integer getAutoProgress() {
            return autoProgress;
        }

        public void setAutoProgress(Integer autoProgress) {
            this.autoProgress = autoProgress;
        }

        public Integer getManualProgress() {
            return manualProgress;
        }

        public void setManualProgress(Integer manualProgress) {
            this.manualProgress = manualProgress;
        }

        public String getProgressReason() {
            return progressReason;
        }

        public void setProgressReason(String progressReason) {
            this.progressReason = progressReason;
        }

        public LocalDate getTargetDate() {
            return targetDate;
        }

        public void setTargetDate(LocalDate targetDate) {
            this.targetDate = targetDate;
        }

        public String getTechStack() {
            return techStack;
        }

        public void setTechStack(String techStack) {
            this.techStack = techStack;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }

        public boolean isArchived() {
            return archived;
        }

        public void setArchived(boolean archived) {
            this.archived = archived;
        }

        public Instant getArchivedAt() {
            return archivedAt;
        }

        public void setArchivedAt(Instant archivedAt) {
            this.archivedAt = archivedAt;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    /**
     * 导出文件中的阶段。
     */
    public static class PhaseItem {

        private Long id;

        private String name;

        private String description;

        private Integer sortOrder;

        private Instant createdAt;

        private Instant updatedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    /**
     * 导出文件中的任务。
     */
    public static class TaskItem {

        private Long id;

        private Long phaseId;

        private String title;

        private String description;

        private String status;

        private String priority;

        private LocalDate dueDate;

        private Integer estimatedHours;

        private boolean blocked;

        private String blockerReason;

        private Instant createdAt;

        private Instant updatedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getPhaseId() {
            return phaseId;
        }

        public void setPhaseId(Long phaseId) {
            this.phaseId = phaseId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public LocalDate getDueDate() {
            return dueDate;
        }

        public void setDueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
        }

        public Integer getEstimatedHours() {
            return estimatedHours;
        }

        public void setEstimatedHours(Integer estimatedHours) {
            this.estimatedHours = estimatedHours;
        }

        public boolean isBlocked() {
            return blocked;
        }

        public void setBlocked(boolean blocked) {
            this.blocked = blocked;
        }

        public String getBlockerReason() {
            return blockerReason;
        }

        public void setBlockerReason(String blockerReason) {
            this.blockerReason = blockerReason;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    /**
     * 导出文件中的工作日志。
     */
    public static class WorkLogItem {

        private Long id;

        private Long taskId;

        private LocalDate logDate;

        private String planContent;

        private String summaryContent;

        private String commitHashes;

        private Integer spentMinutes;

        private String blockerReason;

        private Instant createdAt;

        private Instant updatedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getTaskId() {
            return taskId;
        }

        public void setTaskId(Long taskId) {
            this.taskId = taskId;
        }

        public LocalDate getLogDate() {
            return logDate;
        }

        public void setLogDate(LocalDate logDate) {
            this.logDate = logDate;
        }

        public String getPlanContent() {
            return planContent;
        }

        public void setPlanContent(String planContent) {
            this.planContent = planContent;
        }

        public String getSummaryContent() {
            return summaryContent;
        }

        public void setSummaryContent(String summaryContent) {
            this.summaryContent = summaryContent;
        }

        public String getCommitHashes() {
            return commitHashes;
        }

        public void setCommitHashes(String commitHashes) {
            this.commitHashes = commitHashes;
        }

        public Integer getSpentMinutes() {
            return spentMinutes;
        }

        public void setSpentMinutes(Integer spentMinutes) {
            this.spentMinutes = spentMinutes;
        }

        public String getBlockerReason() {
            return blockerReason;
        }

        public void setBlockerReason(String blockerReason) {
            this.blockerReason = blockerReason;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    /**
     * 导出文件中的项目动态。
     */
    public static class ActivityItem {

        private Long id;

        private String objectType;

        private Long objectId;

        private String action;

        private String summary;

        private Instant createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getObjectType() {
            return objectType;
        }

        public void setObjectType(String objectType) {
            this.objectType = objectType;
        }

        public Long getObjectId() {
            return objectId;
        }

        public void setObjectId(Long objectId) {
            this.objectId = objectId;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }

    /**
     * 导出文件中的附件登记。没有文件内容和磁盘路径。
     */
    public static class AttachmentItem {

        private String originalFileName;

        private String contentType;

        private Long sizeBytes;

        private Instant createdAt;

        public String getOriginalFileName() {
            return originalFileName;
        }

        public void setOriginalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public Long getSizeBytes() {
            return sizeBytes;
        }

        public void setSizeBytes(Long sizeBytes) {
            this.sizeBytes = sizeBytes;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }
}
