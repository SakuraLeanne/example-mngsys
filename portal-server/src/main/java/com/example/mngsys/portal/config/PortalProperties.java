package com.example.mngsys.portal.config;

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

        public List<String> getAllowedHosts() {
            return allowedHosts;
        }

        public void setAllowedHosts(List<String> allowedHosts) {
            this.allowedHosts = allowedHosts;
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
