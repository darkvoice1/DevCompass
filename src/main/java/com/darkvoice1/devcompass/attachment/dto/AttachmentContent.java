package com.darkvoice1.devcompass.attachment.dto;

/**
 * 一次附件下载的文件内容。
 */
public class AttachmentContent {

    private final String originalFileName;

    private final String contentType;

    private final byte[] content;

    /**
     * 创建下载内容。
     *
     * @param originalFileName 原文件名
     * @param contentType 文件类型
     * @param content 文件字节
     */
    public AttachmentContent(String originalFileName, String contentType, byte[] content) {
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.content = content;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }
}
