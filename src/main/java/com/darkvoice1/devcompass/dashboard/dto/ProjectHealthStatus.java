package com.darkvoice1.devcompass.dashboard.dto;

/**
 * 项目健康度。
 */
public enum ProjectHealthStatus {

    /** 没有逾期，也没有即将到期的事项。 */
    HEALTHY,

    /** 有即将到期的任务，或未完成项目的目标日期即将到达。 */
    AT_RISK,

    /** 有已逾期任务，或未完成项目的目标日期已经过期。 */
    OVERDUE
}
