# Example Management System（示例管理系统）

## 项目简介
Example Management System 是一个基于 Spring Boot + Maven 的多模块后端工程，提供认证、网关、门户等核心能力，适用于企业级管理系统的快速搭建与二次开发。

## 目录结构
- auth-server：认证服务（登录、鉴权、用户相关能力）
- gateway-server：网关服务（统一入口、路由转发）
- portal-server：门户服务（业务聚合与对外接口）
- common-utils：公共工具模块
- event-notify-api：事件通知 API 模块

## 环境要求
- JDK 17（或与项目 pom.xml 声明版本一致）
- Maven 3.8+
- 可选：MySQL / Redis / Nacos（按业务配置启用）

## 快速启动
1. 安装依赖并编译：
   mvn clean install
2. 启动认证服务：
   ./auth-server.sh
3. 启动网关服务：
   ./gateway-server.sh
4. 启动门户服务：
   ./portal-server.sh

## 开发说明
- 所有模块统一通过根目录 `pom.xml` 管理。
- 建议按模块独立调试，再进行联调。
- 提交代码前请执行单元测试与基础静态检查。

## 常见命令
- 全量测试：mvn test
- 指定模块测试：mvn -pl auth-server test
- 打包构建：mvn clean package -DskipTests

## 维护建议
- 将环境配置与敏感信息放入外部配置中心或环境变量。
- 生产环境开启日志归档与监控告警。
- 定期升级依赖，修复安全漏洞。
