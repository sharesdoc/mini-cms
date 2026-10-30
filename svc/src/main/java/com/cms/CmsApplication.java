package com.cms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * CMS 客户管理系统后端服务启动入口。
 * 扫描 MyBatis mapper 接口（com.cms.**.mapper）与配置属性绑定。
 */
@SpringBootApplication
@ConfigurationPropertiesScan("com.cms.config")
@MapperScan("com.cms.**.mapper")
public class CmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CmsApplication.class, args);
    }
}
