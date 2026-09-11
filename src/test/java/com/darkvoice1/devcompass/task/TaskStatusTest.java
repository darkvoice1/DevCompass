package com.darkvoice1.devcompass.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.darkvoice1.devcompass.task.entity.TaskStatus;

/**
 * 验证任务状态之间的合法流转规则。
 */
class TaskStatusTest {

    /**
     * 验证待办和进行中任务支持正常推进、退回与取消。
     */
    @Test
    void shouldAllowActiveTaskTransitions() {
        assertThat(TaskStatus.TODO.canTransitionTo(TaskStatus.IN_PROGRESS)).isTrue();
        assertThat(TaskStatus.TODO.canTransitionTo(TaskStatus.COMPLETED)).isTrue();
        assertThat(TaskStatus.TODO.canTransitionTo(TaskStatus.CANCELLED)).isTrue();
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.TODO)).isTrue();
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.COMPLETED)).isTrue();
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.CANCELLED)).isTrue();
    }

    /**
     * 验证已结束任务只能重新打开为待办。
     */
    @Test
    void shouldOnlyReopenFinishedTaskAsTodo() {
        assertThat(TaskStatus.COMPLETED.canTransitionTo(TaskStatus.TODO)).isTrue();
        assertThat(TaskStatus.CANCELLED.canTransitionTo(TaskStatus.TODO)).isTrue();
        assertThat(TaskStatus.COMPLETED.canTransitionTo(TaskStatus.IN_PROGRESS)).isFalse();
        assertThat(TaskStatus.CANCELLED.canTransitionTo(TaskStatus.COMPLETED)).isFalse();
    }

    /**
     * 验证空目标和相同状态不属于状态流转。
     */
    @Test
    void shouldRejectMissingOrSameTargetStatus() {
        assertThat(TaskStatus.TODO.canTransitionTo(null)).isFalse();
        assertThat(TaskStatus.TODO.canTransitionTo(TaskStatus.TODO)).isFalse();
    }
}
