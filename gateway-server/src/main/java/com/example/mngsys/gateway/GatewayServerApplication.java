package com.example.mngsys.gateway;

import com.example.mngsys.gateway.config.GatewaySecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * GatewayServerApplication。
 * <p>
 * 网关服务启动入口，启用配置属性与 Feign 客户端。
 * </p>
 */
@SpringBootApplication
@EnableConfigurationProperties(GatewaySecurityProperties.class)
@EnableFeignClients
public class GatewayServerApplication {

    /**
     * 应用主入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayServerApplication.class, args);
    }
}
