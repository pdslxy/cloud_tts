package com.enerbos.cloud.tts.microservice.service.impl;

import javax.annotation.Resource;

import com.dangdang.ddframe.job.lite.lifecycle.api.JobOperateAPI;
import com.dangdang.ddframe.job.lite.lifecycle.api.JobSettingsAPI;
import com.enerbos.cloud.tts.microservice.service.TimeTaskService;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;

/**
 * All rights Reserved, Designed By 翼虎能源
 * Copyright:    Copyright(C) 2015-2017
 * Company   北京翼虎能源科技有限公司
 *
 * @author 刘广路
 * @version 1.0.0
 * @date 2017/4/24 11:29
 * @Description 时间任务的服务
 */
@Service
public class TimeTaskServiceImpl implements TimeTaskService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ZookeeperRegistryCenter regCenter;

    @Resource
    private JobEventConfiguration jobEventConfiguration;

    @Resource
    private JobOperateAPI jobOperateAPI;

    @Resource
    private JobSettingsAPI jobSettingsAPI;

    private LiteJobConfiguration getLiteJobConfiguration(final String jobName,final Class<? extends SimpleJob> jobClass, final String cron,
                                                         final int shardingTotalCount, final String shardingItemParameters, final String jobParameter) {
        return LiteJobConfiguration.newBuilder(new SimpleJobConfiguration(
                JobCoreConfiguration.newBuilder(jobName, cron, shardingTotalCount)
                        .shardingItemParameters(shardingItemParameters).jobParameter(jobParameter).build(),
                jobClass.getCanonicalName())).overwrite(true).build();
    }

    @Override
    public boolean startTask(final String jobName,final SimpleJob simpleJob, final String cron, final int shardingTotalCount,
                             final String shardingItemParameters, final String jobParameter) {
        try {
            JobScheduler jobScheduler = new SpringJobScheduler(simpleJob, regCenter,
                    getLiteJobConfiguration(jobName,simpleJob.getClass(), cron, shardingTotalCount, shardingItemParameters,
                            jobParameter),
                    jobEventConfiguration);
            jobScheduler.init();
            return true;
        } catch (Exception e) {
            logger.error("--------startTask-------", e);
            System.out.println(e);
        }
        return false;
    }

    @Override
    public boolean deleteTask(String jobName) {
        try {
            jobOperateAPI.shutdown(Optional.of(jobName), Optional.<String>absent());
            jobSettingsAPI.removeJobSettings(jobName);
        } catch (Exception ex) {
            logger.error("---------deleteTask------", ex);
        }
        return false;
    }
}
