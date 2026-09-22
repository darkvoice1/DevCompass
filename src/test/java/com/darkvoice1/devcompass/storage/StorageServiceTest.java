package com.darkvoice1.devcompass.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.darkvoice1.devcompass.common.exception.BusinessException;
import com.darkvoice1.devcompass.common.exception.ErrorCode;
import com.darkvoice1.devcompass.common.time.DevCompassProperties;
import com.darkvoice1.devcompass.storage.dto.StoredFile;
import com.darkvoice1.devcompass.storage.service.StorageService;

/**
 * 验证附件存储的保存、读取、删除和校验。
 */
class StorageServiceTest {

    @TempDir
    private Path tempDir;

    private StorageService storage;

    /**
     * 使用临时目录，并把单文件上限设为 1024 字节。
     */
    @BeforeEach
    void setUp() {
        storage = newStorage(1024);
    }

    /**
     * 验证文件按生成的编号保存，原文件名不会出现在磁盘上。
     */
    @Test
    void shouldSaveLoadAndDeleteByGeneratedKey() throws IOException {
        byte[] content = new byte[] {1, 2, 3, 4};
        StoredFile stored = storage.save("  设计图.png  ", content);

        assertThat(stored.getStorageKey()).matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
        assertThat(stored.getOriginalFileName()).isEqualTo("设计图.png");
        assertThat(stored.getContentType()).isEqualTo("image/png");
        assertThat(stored.getSizeBytes()).isEqualTo(4);
        assertThat(tempDir.resolve(stored.getStorageKey())).exists();
        assertThat(tempDir.resolve("设计图.png")).doesNotExist();
        assertThat(storage.load(stored.getStorageKey())).containsExactly(content);

        storage.delete(stored.getStorageKey());
        assertThat(Files.exists(tempDir.resolve(stored.getStorageKey()))).isFalse();
        assertBusinessError(() -> storage.load(stored.getStorageKey()),
                ErrorCode.BUSINESS_ERROR, "文件不存在");
    }

    /**
     * 验证两个同名文件会得到不同编号，内容互不覆盖。
     */
    @Test
    void shouldKeepSameOriginalNameAsSeparateFiles() {
        StoredFile first = storage.save("设计图.png", new byte[] {1});
        StoredFile second = storage.save("设计图.png", new byte[] {2});

        assertThat(first.getStorageKey()).isNotEqualTo(second.getStorageKey());
        assertThat(storage.load(first.getStorageKey())).containsExactly(1);
        assertThat(storage.load(second.getStorageKey())).containsExactly(2);
    }

    /**
     * 验证允许的后缀不区分大小写，并返回对应文件类型。
     */
    @Test
    void shouldAcceptAllowedFileTypes() {
        assertContentType("设计图.png", "image/png");
        assertContentType("设计图.JPG", "image/jpeg");
        assertContentType("设计图.jpeg", "image/jpeg");
        assertContentType("设计图.gif", "image/gif");
        assertContentType("设计图.webp", "image/webp");
        assertContentType("说明.PDF", "application/pdf");
        assertContentType("记录.txt", "text/plain");
        assertContentType("记录.md", "text/markdown");
        assertContentType("打包.zip", "application/zip");
    }

    /**
     * 验证空文件名和空白文件名会被拒绝。
     */
    @Test
    void shouldRejectBlankFileName() {
        assertValidationError(() -> storage.save(null, new byte[] {1}), "文件名不能为空");
        assertValidationError(() -> storage.save("   ", new byte[] {1}), "文件名不能为空");
        assertThat(tempDir).isEmptyDirectory();
    }

    /**
     * 验证文件名里的路径片段会被拒绝，并且不会写出文件。
     */
    @Test
    void shouldRejectFileNameWithPath() {
        assertValidationError(() -> storage.save("../secret.png", new byte[] {1}), "文件名不能包含路径");
        assertValidationError(() -> storage.save("..\\secret.png", new byte[] {1}), "文件名不能包含路径");
        assertValidationError(() -> storage.save("dir/a.png", new byte[] {1}), "文件名不能包含路径");
        assertValidationError(() -> storage.save("dir\\a.png", new byte[] {1}), "文件名不能包含路径");
        assertValidationError(() -> storage.save("设计..图.png", new byte[] {1}), "文件名不能包含路径");
        assertThat(tempDir).isEmptyDirectory();
    }

    /**
     * 验证没有允许后缀的文件会被拒绝。
     */
    @Test
    void shouldRejectUnsupportedFileType() {
        assertValidationError(() -> storage.save("脚本.exe", new byte[] {1}), "不支持的文件类型");
        assertValidationError(() -> storage.save("没有后缀", new byte[] {1}), "不支持的文件类型");
        assertValidationError(() -> storage.save(".png", new byte[] {1}), "不支持的文件类型");
        assertValidationError(() -> storage.save("伪装.txt.exe", new byte[] {1}), "不支持的文件类型");
        assertThat(tempDir).isEmptyDirectory();
    }

    /**
     * 验证空内容和超过上限的内容会被拒绝。
     */
    @Test
    void shouldRejectEmptyOrOversizedContent() {
        assertValidationError(() -> storage.save("设计图.png", null), "文件不能为空");
        assertValidationError(() -> storage.save("设计图.png", new byte[0]), "文件不能为空");
        assertValidationError(() -> storage.save("设计图.png", new byte[1025]), "文件大小不能超过1024字节");

        StoredFile stored = storage.save("设计图.png", new byte[1024]);
        assertThat(stored.getSizeBytes()).isEqualTo(1024);
    }

    /**
     * 验证整兆字节上限会显示成 MB。
     */
    @Test
    void shouldDescribeMegabyteLimit() {
        StorageService megabyteStorage = newStorage(1024L * 1024);
        assertValidationError(() -> megabyteStorage.save("设计图.png", new byte[1024 * 1024 + 1]),
                "文件大小不能超过1MB");
    }

    /**
     * 验证读取和删除时拒绝带路径的存储编号。
     */
    @Test
    void shouldRejectStorageKeyWithPath() {
        String key = UUID.randomUUID().toString();
        assertValidationError(() -> storage.load(null), "存储编号不合法");
        assertValidationError(() -> storage.load("../" + key), "存储编号不合法");
        assertValidationError(() -> storage.load(key + "/secret"), "存储编号不合法");
        assertValidationError(() -> storage.delete("..\\secret"), "存储编号不合法");
        assertValidationError(() -> storage.load("不是编号"), "存储编号不合法");
    }

    /**
     * 验证删除一个本来就不存在的文件不会报错。
     */
    @Test
    void shouldIgnoreDeleteWhenFileIsAlreadyMissing() {
        String key = UUID.randomUUID().toString();
        storage.delete(key);
        storage.delete(key);
        assertBusinessError(() -> storage.load(key), ErrorCode.BUSINESS_ERROR, "文件不存在");
    }

    /**
     * 验证目录或大小上限配置不合法时会立刻失败。
     */
    @Test
    void shouldRejectInvalidStorageSetup() {
        DevCompassProperties blankDir = new DevCompassProperties();
        blankDir.getStorage().setLocalDir("  ");
        assertThatThrownBy(() -> new StorageService(blankDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("附件目录不能为空");

        DevCompassProperties missingStorage = new DevCompassProperties();
        missingStorage.setStorage(null);
        assertThatThrownBy(() -> new StorageService(missingStorage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("附件目录不能为空");

        DevCompassProperties invalidSize = new DevCompassProperties();
        invalidSize.getStorage().setLocalDir(tempDir.toString());
        invalidSize.getStorage().setMaxSizeBytes(0);
        assertThatThrownBy(() -> new StorageService(invalidSize))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("文件大小上限必须大于0");
    }

    /**
     * 验证默认和显式的 local 都可以保存文件。
     */
    @Test
    void shouldUseLocalStorageByDefault() {
        DevCompassProperties defaults = new DevCompassProperties();
        assertThat(defaults.getStorage().getType()).isEqualTo("local");

        DevCompassProperties properties = new DevCompassProperties();
        properties.getStorage().setType(" LOCAL ");
        properties.getStorage().setLocalDir(tempDir.toString());
        properties.getStorage().setMaxSizeBytes(8);
        StorageService localStorage = new StorageService(properties);

        StoredFile stored = localStorage.save("记录.md", new byte[] {9});
        assertThat(localStorage.load(stored.getStorageKey())).containsExactly(9);
    }

    /**
     * 验证选择 MinIO 或其他未知类型时会立刻失败。
     */
    @Test
    void shouldRejectMinioUntilItIsImplemented() {
        DevCompassProperties minio = new DevCompassProperties();
        minio.getStorage().setType("minio");
        minio.getStorage().setLocalDir(tempDir.toString());
        assertThatThrownBy(() -> new StorageService(minio))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MinIO 还没实现，请继续使用 local");

        DevCompassProperties blankType = new DevCompassProperties();
        blankType.getStorage().setType("  ");
        blankType.getStorage().setLocalDir(tempDir.toString());
        assertThatThrownBy(() -> new StorageService(blankType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("存储类型不能为空");

        DevCompassProperties unknown = new DevCompassProperties();
        unknown.getStorage().setType("s3");
        unknown.getStorage().setLocalDir(tempDir.toString());
        assertThatThrownBy(() -> new StorageService(unknown))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("不支持的存储类型，请使用 local");
    }

    private StorageService newStorage(long maxSizeBytes) {
        DevCompassProperties properties = new DevCompassProperties();
        properties.getStorage().setLocalDir(tempDir.toString());
        properties.getStorage().setMaxSizeBytes(maxSizeBytes);
        return new StorageService(properties);
    }

    private void assertContentType(String fileName, String contentType) {
        StoredFile stored = storage.save(fileName, new byte[] {1});
        assertThat(stored.getContentType()).isEqualTo(contentType);
        assertThat(stored.getOriginalFileName()).isEqualTo(fileName);
    }

    private void assertValidationError(ThrowableAction action, String message) {
        assertBusinessError(action, ErrorCode.VALIDATION_ERROR, message);
    }

    private void assertBusinessError(ThrowableAction action, ErrorCode errorCode, String message) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .hasMessage(message)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(errorCode));
    }

    @FunctionalInterface
    private interface ThrowableAction {
        void run() throws Exception;
    }
}
