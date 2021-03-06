package com.enerbos.cloud.tts.microservice.task;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.enerbos.cloud.eis.client.EisTaskClient;

/**
 * All rights Reserved, Designed By 翼虎能源
 * Copyright:    Copyright(C) 2015-2017
 * Company   北京翼虎能源科技有限公司
 * @author 李伟龙
 * @version 1.0
 * @date 2017年11月1日 下午4:42:49
 * @Description 任务生成定时任务
 */
@Component
public class EisCreateTaskTimeJob implements SimpleJob {

	private Logger logger = LoggerFactory.getLogger(EisCreateTaskTimeJob.class);

	@Autowired
	private EisTaskClient eisTaskClient;

	@Override
	public void execute(ShardingContext shardingContext) {
		try {
			String id = shardingContext.getJobParameter();
			logger.debug("--------定时任务--------当前时间:{},id:{}", new Date(), id);
			eisTaskClient.saveEisTaskByTemplateId(id);
		} catch (Exception e) {
			logger.error("--------定时任务--------{}", e);
		}
	}

}
