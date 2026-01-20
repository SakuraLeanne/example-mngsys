package com.dhgx.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "portal")
/**
 * PortalProperties。
 */
public class PortalProperties {

    private Security security = new Security();
    private Sso sso = new Sso();
    private Ptk ptk = new Ptk();
    private UserAuth userAuth = new UserAuth();
    private Events events = new Events();

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Sso getSso() {
        return sso;
    }

    public void setSso(Sso sso) {
        this.sso = sso;
    }

    public Ptk getPtk() {
        return ptk;
    }

    public void setPtk(Ptk ptk) {
        this.ptk = ptk;
    }

    public UserAuth getUserAuth() {
        return userAuth;
    }

    public void setUserAuth(UserAuth userAuth) {
        this.userAuth = userAuth;
    }

    public Events getEvents() {
        return events;
    }

    public void setEvents(Events events) {
        this.events = events;
    }

    public static class Security {
        private List<String> allowedHosts = new ArrayList<>();
        private Captcha captcha = new Captcha();

        public List<String> getAllowedHosts() {
            return allowedHosts;
        }

        public void setAllowedHosts(List<String> allowedHosts) {
            this.allowedHosts = allowedHosts;
        }

        public Captcha getCaptcha() {
            return captcha;
        }

        public void setCaptcha(Captcha captcha) {
            this.captcha = captcha;
        }

        public static class Captcha {
            private boolean enabled = true;
            private long ttlSeconds = 180;
            private int length = 4;
            private long failThreshold = 3;
            private long failWindowSeconds = 900;
            private String hashSecret;
            private boolean caseInsensitive = true;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public long getTtlSeconds() {
                return ttlSeconds;
            }

            public void setTtlSeconds(long ttlSeconds) {
                this.ttlSeconds = ttlSeconds;
            }

            public int getLength() {
                return length;
            }

            public void setLength(int length) {
                this.length = length;
            }

            public long getFailThreshold() {
                return failThreshold;
            }

            public void setFailThreshold(long failThreshold) {
                this.failThreshold = failThreshold;
            }

            public long getFailWindowSeconds() {
                return failWindowSeconds;
            }

            public void setFailWindowSeconds(long failWindowSeconds) {
                this.failWindowSeconds = failWindowSeconds;
            }

            public String getHashSecret() {
                return hashSecret;
            }

            public void setHashSecret(String hashSecret) {
                this.hashSecret = hashSecret;
            }

            public boolean isCaseInsensitive() {
                return caseInsensitive;
            }

            public void setCaseInsensitive(boolean caseInsensitive) {
                this.caseInsensitive = caseInsensitive;
            }
        }
    }

    public static class Sso {
        private long ticketTtlSeconds = 60;

        public long getTicketTtlSeconds() {
            return ticketTtlSeconds;
        }

        public void setTicketTtlSeconds(long ticketTtlSeconds) {
            this.ticketTtlSeconds = ticketTtlSeconds;
        }
    }

    public static class Ptk {
        private long ttlSeconds = 600;

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    public static class UserAuth {
        private long cacheTtlSeconds = 600;

        public long getCacheTtlSeconds() {
            return cacheTtlSeconds;
        }

        public void setCacheTtlSeconds(long cacheTtlSeconds) {
            this.cacheTtlSeconds = cacheTtlSeconds;
        }
    }

    public static class Events {
        private String streamKey = "portal:events";

        public String getStreamKey() {
            return streamKey;
        }

        public void setStreamKey(String streamKey) {
            this.streamKey = streamKey;
        }
    }
}
