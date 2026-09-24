package com.darkvoice1.devcompass.common.time;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用自定义配置。
 */
@ConfigurationProperties(prefix = "devcompass")
public class DevCompassProperties {

    /**
     * 计算日期边界时使用的时区。
     */
    private String timezone = "Asia/Shanghai";

    /**
     * 附件文件保存位置和大小上限。
     */
    private Storage storage = new Storage();

    /**
     * 认证与初始账号配置。
     */
    private Auth auth = new Auth();

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    /**
     * 附件存储配置，对应 devcompass.storage。
     */
    public static class Storage {

        /**
         * 存储类型。当前只实现 local。
         */
        private String type = "local";

        /**
         * 附件保存目录。相对路径相对于应用运行目录。
         */
        private String localDir = "data/attachments";

        /**
         * 单个文件最大字节数，默认 10MB。
         */
        private long maxSizeBytes = 10L * 1024 * 1024;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLocalDir() {
            return localDir;
        }

        public void setLocalDir(String localDir) {
            this.localDir = localDir;
        }

        public long getMaxSizeBytes() {
            return maxSizeBytes;
        }

        public void setMaxSizeBytes(long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
        }
    }

    /**
     * 认证配置，对应 devcompass.auth。
     */
    public static class Auth {

        /**
         * 用户表为空时创建的初始用户名。
         */
        private String initialUsername;

        /**
         * 初始用户的原始密码，只从运行环境读取，不写入数据库。
         */
        private String initialPassword;

        /**
         * JWT 签名密钥，HS256 至少需要 32 字节。
         */
        private String jwtSecret;

        /**
         * Access Token 有效期，默认 15 分钟。
         */
        private Duration accessTokenExpiration = Duration.ofMinutes(15);

        /**
         * Refresh Token 有效期，默认 7 天。
         */
        private Duration refreshTokenExpiration = Duration.ofDays(7);

        public String getInitialUsername() {
            return initialUsername;
        }

        public void setInitialUsername(String initialUsername) {
            this.initialUsername = initialUsername;
        }

        public String getInitialPassword() {
            return initialPassword;
        }

        public void setInitialPassword(String initialPassword) {
            this.initialPassword = initialPassword;
        }

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public Duration getAccessTokenExpiration() {
            return accessTokenExpiration;
        }

        public void setAccessTokenExpiration(Duration accessTokenExpiration) {
            this.accessTokenExpiration = accessTokenExpiration;
        }

        public Duration getRefreshTokenExpiration() {
            return refreshTokenExpiration;
        }

        public void setRefreshTokenExpiration(Duration refreshTokenExpiration) {
            this.refreshTokenExpiration = refreshTokenExpiration;
        }
    }
}
