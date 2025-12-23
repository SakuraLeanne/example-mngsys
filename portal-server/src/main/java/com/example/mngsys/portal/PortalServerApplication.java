package com.example.mngsys.portal;

import com.example.mngsys.portal.config.AuthClientProperties;
import com.example.mngsys.portal.config.PortalProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties({PortalProperties.class, AuthClientProperties.class})
public class PortalServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PortalServerApplication.class, args);
    }
}
