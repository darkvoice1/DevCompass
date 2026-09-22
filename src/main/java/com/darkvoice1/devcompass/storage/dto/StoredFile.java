package com.darkvoice1.devcompass.storage.dto;

/**
 * 一次文件保存的结果。
 */
public class StoredFile {

    private final String storageKey;

    private final String originalFileName;

    private final String contentType;

    private final long sizeBytes;

    /**
     * 创建保存结果。
     *
     * @param storageKey 磁盘上使用的存储编号
     * @param originalFileName 去掉首尾空白后的原文件名
     * @param contentType 根据后缀得到的文件类型
     * @param sizeBytes 文件字节数
     */
    public StoredFile(String storageKey, String originalFileName, String contentType, long sizeBytes) {
        this.storageKey = storageKey;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
