package com.xuesinuo.pignoo.test;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试上下文锚点：仅为 {@code @SpringBootTest} 提供 Spring Boot 配置入口
 * （组件扫描 + 自动装配 DataSource，数据源配置见 src/test/resources/application.yml）。
 * <p>
 * Test context anchor: provides the Spring Boot configuration entry point for
 * {@code @SpringBootTest} (component scan + DataSource auto-configuration; the
 * datasource config lives in src/test/resources/application.yml).
 * <p>
 * 纯测试模块，不需要独立启动，故无 main 方法。
 * <p>
 * This is a test-only module that never runs standalone, so there is no main method.
 */
@SpringBootApplication
public class PignooTestApplication {
}
