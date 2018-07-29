package com.enerbos.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;

/**
 * All rights Reserved, Designed By 翼虎能源
 * Copyright:    Copyright(C) 2015-2017
 * Company   北京翼虎能源科技有限公司
 *
 * @author 刘广路
 * @version 1.0.0
 * @date 2017/04/10 10:41
 * @Description TTS的微服务
 */
@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@EnableDiscoveryClient
@EnableFeignClients({ "com.enerbos.cloud.eam", "com.enerbos.cloud.uas", "com.enerbos.cloud.ams",
		"com.enerbos.cloud.wfs", "com.enerbos.cloud.eis" })
@EnableCaching
public class EnerbosTtsMicroserviceApplication extends RepositoryRestConfigurerAdapter {

	public static void main(String[] args) {
		SpringApplication.run(EnerbosTtsMicroserviceApplication.class, args);
	}
}
