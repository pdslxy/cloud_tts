
package com.enerbos.cloud.tts.microservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.enerbos.cloud.tts.microservice.service.TimeTaskService;
import com.enerbos.cloud.tts.microservice.task.EisCreateReportTimeJob;
import com.enerbos.cloud.tts.microservice.task.EisCreateTaskTimeJob;

/**
 * 
 * All rights Reserved, Designed By 翼虎能源
 * Copyright:    Copyright(C) 2015-2017
 * Company   北京翼虎能源科技有限公司
 *
 * @author 李伟龙
 * @version 1.0
 * @date 2017年11月1日 下午4:21:22
 * @Description TODO 方法描述
 */
@RestController
public class EisTimerTaskController {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private TimeTaskService timeTaskService;

	@Autowired
	private EisCreateTaskTimeJob eisCreateTaskTimeJob;
	@Autowired
	private EisCreateReportTimeJob eisCreateReportTimeJob;

	/**
	 * startCreateTask: 生成任务
	 * @param jobName 定时任务名称
	 * @param cron 时间表达式
	 * @param shardingTotalCount 分片总数
	 * @param shardingItemParameters 分片参数
	 * @param jobParameters 任务的参数
	 * @return boolean 是否成功
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/tts/micro/task/startCreateTask")
	public boolean startCreateTask(@RequestParam("jobName") String jobName, @RequestParam("cron") String cron,
			@RequestParam("shardingTotalCount") int shardingTotalCount,
			@RequestParam("shardingItemParameters") String shardingItemParameters,
			@RequestParam("jobParameters") String jobParameters) {
		try {
			timeTaskService.startTask(jobName, eisCreateTaskTimeJob, cron, shardingTotalCount, shardingItemParameters,
					jobParameters);
			return true;
		} catch (Exception e) {
			logger.error("-------startCreateTask-----{}", e);
		}
		return false;
	}

	/**
	 * startCreateReport: 生成报告
	 * @param jobName 定时任务名称
	 * @param cron 时间表达式
	 * @param shardingTotalCount 分片总数
	 * @param shardingItemParameters 分片参数
	 * @param jobParameters 任务的参数
	 * @return boolean 是否成功
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/tts/micro/task/startCreateReport")
	public boolean startCreateReport(@RequestParam("jobName") String jobName, @RequestParam("cron") String cron,
			@RequestParam("shardingTotalCount") int shardingTotalCount,
			@RequestParam("shardingItemParameters") String shardingItemParameters,
			@RequestParam("jobParameters") String jobParameters) {
		try {
			timeTaskService.startTask(jobName, eisCreateReportTimeJob, cron, shardingTotalCount, shardingItemParameters,
					jobParameters);
			return true;
		} catch (Exception e) {
			logger.error("-------startCreateReport-----{}", e);
		}
		return false;
	}

}
