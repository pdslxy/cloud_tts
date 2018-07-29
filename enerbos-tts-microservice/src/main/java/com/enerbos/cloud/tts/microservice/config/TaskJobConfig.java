/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.enerbos.cloud.tts.microservice.config;

import javax.annotation.Resource;

import com.dangdang.ddframe.job.lite.lifecycle.api.JobAPIFactory;
import com.dangdang.ddframe.job.lite.lifecycle.api.JobOperateAPI;
import com.dangdang.ddframe.job.lite.lifecycle.api.JobSettingsAPI;
import com.google.common.base.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.druid.pool.DruidDataSource;
import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.dangdang.ddframe.job.event.rdb.JobEventRdbConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;

/**
 * All rights Reserved, Designed By 翼虎能源
 * Copyright:    Copyright(C) 2015-2017
 * Company   北京翼虎能源科技有限公司
 *
 * @author 刘广路
 * @version 1.0.0
 * @date 2017/04/10 10:41
 * @Description elatic job 的事件数据配置
 */
@Configuration
public class TaskJobConfig {

	@Resource
	private DruidDataSource druidDataSource;

	@Bean(initMethod = "init")
	public ZookeeperRegistryCenter simpleJobScheduler(@Value("${regCenter.serverList}") final String serverList,
			@Value("${regCenter.namespace}") final String namespace) {
		ZookeeperConfiguration zookeeperConfiguration = new ZookeeperConfiguration(serverList, namespace);
		return new ZookeeperRegistryCenter(zookeeperConfiguration);
	}

	@Bean
	public JobEventConfiguration jobEventConfiguration() {
		JobEventConfiguration jobEventConfiguration = new JobEventRdbConfiguration(druidDataSource);
		return jobEventConfiguration;
	}

	/**
	 * 定时任务操作接口
	 * @param serverList zookeeper地址列表
	 * @param digest 登录凭证
	 * @param namespace 命名空间
	 * @return 实例
	 *
	 * .shutdown(Optional.of(jobName), Optional.<String>absent());
	 */
	@Bean
	public JobOperateAPI jobOperateAPI(@Value("${regCenter.serverList}") final String serverList,
									   @Value("${regCenter.digest}") final String digest,
									   @Value("${regCenter.namespace}") final String namespace) {
		return JobAPIFactory.createJobOperateAPI(serverList, namespace, Optional.fromNullable(digest));
	}

	/**
	 * 定时任务操作接口
	 * @param serverList zookeeper地址列表
	 * @param digest 登录凭证
	 * @param namespace 命名空间
	 * @return 实例
	 *
	 * .shutdown(Optional.of(jobName), Optional.<String>absent());
	 */
	@Bean
	public JobSettingsAPI jobSettingsAPI(@Value("${regCenter.serverList}") final String serverList,
										@Value("${regCenter.digest}") final String digest,
										@Value("${regCenter.namespace}") final String namespace) {
		return JobAPIFactory.createJobSettingsAPI(serverList, namespace, Optional.fromNullable(digest));
	}
}
