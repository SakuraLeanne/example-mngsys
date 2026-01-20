package com.dhgx.portal;

import com.dhgx.common.gateway.GatewaySecurityProperties;
import com.dhgx.portal.config.AuthClientProperties;
import com.dhgx.portal.config.PortalProperties;
import org.springframework.boot.SpringApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@MapperScan("com.dhgx.portal.mapper")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.dhgx"})
@EnableConfigurationProperties({PortalProperties.class, AuthClientProperties.class, GatewaySecurityProperties.class})
/**
 * PortalServerApplication。
 */
public class PortalServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PortalServerApplication.class, args);
    }
}
