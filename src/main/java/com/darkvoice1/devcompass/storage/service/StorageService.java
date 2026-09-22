package com.darkvoice1.devcompass.storage.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.common.time.DevCompassProperties;
import com.darkvoice1.devcompass.storage.dto.StoredFile;

/**
 * 保存、读取和删除附件文件。
 * 当前只支持 local，文件写到配置的本机目录，磁盘文件名使用生成的编号。
 */
@Service
public class StorageService {

    private static final long ONE_MB = 1024L * 1024;

    private static final Pattern STORAGE_KEY = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "gif", "image/gif",
            "webp", "image/webp",
            "pdf", "application/pdf",
            "txt", "text/plain",
            "md", "text/markdown",
            "zip", "application/zip");

    private final Path rootDirectory;

    private final long maxSizeBytes;

    /**
     * 按应用配置创建存储服务。
     *
     * @param properties 应用自定义配置，其中包含附件目录和大小上限
     */
    public StorageService(DevCompassProperties properties) {
        if (properties == null || properties.getStorage() == null) {
            throw new IllegalArgumentException("附件目录不能为空");
        }
        DevCompassProperties.Storage storage = properties.getStorage();
        requireLocalStorage(storage.getType());
        if (storage.getLocalDir() == null || storage.getLocalDir().isBlank()) {
            throw new IllegalArgumentException("附件目录不能为空");
        }
        if (storage.getMaxSizeBytes() <= 0) {
            throw new IllegalArgumentException("文件大小上限必须大于0");
        }
        this.rootDirectory = Path.of(storage.getLocalDir().trim())
                .toAbsolutePath()
                .normalize();
        this.maxSizeBytes = storage.getMaxSizeBytes();
    }

    /**
     * 确认当前使用本机存储。MinIO 只保留配置名，选中时拒绝启动。
     *
     * @param type 配置中的存储类型
     */
    private void requireLocalStorage(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("存储类型不能为空");
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        if ("local".equals(normalized)) {
            return;
        }
        if ("minio".equals(normalized)) {
            throw new IllegalArgumentException("MinIO 还没实现，请继续使用 local");
        }
        throw new IllegalArgumentException("不支持的存储类型，请使用 local");
    }

    /**
     * 校验文件名、大小和类型后，把内容写入附件目录。
     *
     * @param originalFileName 用户看到的原文件名
     * @param content 文件内容
     * @return 保存结果
     */
    public StoredFile save(String originalFileName, byte[] content) {
        String fileName = normalizeFileName(originalFileName);
        validateContent(content);
        String contentType = contentTypeOf(fileName);
        String storageKey = UUID.randomUUID().toString();
        Path target = resolveStoredPath(storageKey);
        try {
            Files.createDirectories(rootDirectory);
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
        } catch (IOException exception) {
            throw storageFailure("保存文件失败", exception);
        }
        return new StoredFile(storageKey, fileName, contentType, content.length);
    }

    /**
     * 读取编号对应的文件。编号不合法或文件不存在时拒绝。
     *
     * @param storageKey 存储编号
     * @return 文件内容
     */
    public byte[] load(String storageKey) {
        Path target = resolveStoredPath(storageKey);
        if (!Files.isRegularFile(target)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "文件不存在");
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException exception) {
            throw storageFailure("读取文件失败", exception);
        }
    }

    /**
     * 删除编号对应的文件。文件已经不存在时不再报错。
     *
     * @param storageKey 存储编号
     */
    public void delete(String storageKey) {
        Path target = resolveStoredPath(storageKey);
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw storageFailure("删除文件失败", exception);
        }
    }

    /**
     * 整理原文件名，并拒绝空名称和带路径的名称。
     *
     * @param originalFileName 用户传来的文件名
     * @return 去掉首尾空白后的文件名
     */
    private String normalizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "文件名不能为空");
        }
        String fileName = originalFileName.trim();
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "文件名不能包含路径");
        }
        return fileName;
    }

    /**
     * 拒绝空文件和超过上限的文件。
     *
     * @param content 文件内容
     */
    private void validateContent(byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "文件不能为空");
        }
        if (content.length > maxSizeBytes) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, maxSizeMessage());
        }
    }

    /**
     * 按最后一个后缀判断是否允许保存，并得到对应的文件类型。
     *
     * @param fileName 整理后的文件名
     * @return 文件类型
     */
    private String contentTypeOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不支持的文件类型");
        }
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        String contentType = CONTENT_TYPES.get(extension);
        if (contentType == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不支持的文件类型");
        }
        return contentType;
    }

    /**
     * 把存储编号解析成附件目录内的路径。带路径的编号会在这里被拒绝。
     *
     * @param storageKey 存储编号
     * @return 附件文件路径
     */
    private Path resolveStoredPath(String storageKey) {
        if (storageKey == null || !STORAGE_KEY.matcher(storageKey).matches()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "存储编号不合法");
        }
        Path target = rootDirectory.resolve(storageKey).normalize();
        if (!target.startsWith(rootDirectory)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "存储编号不合法");
        }
        return target;
    }

    /**
     * 把配置的字节上限说成可读的大小。
     *
     * @return 给调用方看的大小说明
     */
    private String maxSizeMessage() {
        if (maxSizeBytes % ONE_MB == 0) {
            return "文件大小不能超过" + (maxSizeBytes / ONE_MB) + "MB";
        }
        return "文件大小不能超过" + maxSizeBytes + "字节";
    }

    /**
     * 把磁盘读写失败转换成统一的系统错误。
     *
     * @param message 给调用方看的说明
     * @param exception 原始读写异常
     * @return 业务异常
     */
    private BusinessException storageFailure(String message, IOException exception) {
        BusinessException businessException = new BusinessException(ErrorCode.INTERNAL_ERROR, message);
        businessException.initCause(exception);
        return businessException;
    }
}
