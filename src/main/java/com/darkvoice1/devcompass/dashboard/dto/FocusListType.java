package com.darkvoice1.devcompass.dashboard.dto;

/**
 * 首页焦点清单类型。
 */
public enum FocusListType {

    /** 本周到期的未完成任务。 */
    THIS_WEEK,

    /** 已逾期的任务，以及目标日期已过的未完成项目。 */
    OVERDUE,

    /** 今天起 7 天内到期的未完成任务。 */
    DUE_SOON,

    /** 已标记阻塞且尚未完成的任务。 */
    BLOCKED
}
