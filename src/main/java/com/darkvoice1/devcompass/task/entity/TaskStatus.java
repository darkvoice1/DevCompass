package com.darkvoice1.devcompass.task.entity;

/**
 * 任务当前状态。
 */
public enum TaskStatus {

    /** 待办。 */
    TODO,

    /** 进行中。 */
    IN_PROGRESS,

    /** 已完成。 */
    COMPLETED,

    /** 已取消。 */
    CANCELLED;

    /**
     * 判断当前状态能否流转到目标状态。
     *
     * @param targetStatus 目标状态
     * @return 可以流转时返回 true
     */
    public boolean canTransitionTo(TaskStatus targetStatus) {
        if (targetStatus == null || this == targetStatus) {
            return false;
        }

        return switch (this) {
            case TODO -> targetStatus == IN_PROGRESS
                    || targetStatus == COMPLETED
                    || targetStatus == CANCELLED;
            case IN_PROGRESS -> targetStatus == TODO
                    || targetStatus == COMPLETED
                    || targetStatus == CANCELLED;
            case COMPLETED, CANCELLED -> targetStatus == TODO;
        };
    }
}
