package com.dhgx.portal.service;

/**
 * UserAuthCacheService。
 */
public interface UserAuthCacheService {
    void updateUserAuthCache(String userId, Integer status, Long authVersion, Long profileVersion);

    UserAuthCache getUserAuthCache(String userId);

    class UserAuthCache {
        private final Integer status;
        private final Long authVersion;
        private final Long profileVersion;

        public UserAuthCache(Integer status, Long authVersion, Long profileVersion) {
            this.status = status;
            this.authVersion = authVersion;
            this.profileVersion = profileVersion;
        }

        public Integer getStatus() {
            return status;
        }

        public Long getAuthVersion() {
            return authVersion;
        }

        public Long getProfileVersion() {
            return profileVersion;
        }
    }
}
