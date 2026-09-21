package com.darkvoice1.devcompass.activity.entity;

/**
 * 动态记录对应的操作类型。
 */
public enum ActivityAction {

    /** 新建对象。 */
    CREATED,

    /** 编辑对象。 */
    UPDATED,

    /** 变更状态。 */
    STATUS_CHANGED,

    /** 归档对象。 */
    ARCHIVED,

    /** 从归档或软删除中恢复。 */
    RESTORED,

    /** 软删除对象。 */
    DELETED
}
