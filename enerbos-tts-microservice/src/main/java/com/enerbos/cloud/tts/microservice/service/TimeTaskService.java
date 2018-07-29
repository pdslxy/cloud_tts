package com.enerbos.cloud.tts.microservice.service;

import com.dangdang.ddframe.job.api.simple.SimpleJob;

/**
 * All rights Reserved, Designed By 翼虎能源
 * Copyright:    Copyright(C) 2015-2017
 * Company   北京翼虎能源科技有限公司
 *
 * @author 刘广路
 * @version 1.0.0
 * @date 2017/4/24 11:28
 * @Description 时间任务的接口
 */
public interface TimeTaskService {

	/**
	 * 启动一个任务
	 * @param jobName 实例任务的名称,规定:任务的ID
	 * @param simpleJob JOB类
	 * @param cron  时间
	 * @param shardingTotalCount 分片总数，同一个任务量大时使用。
	 * @param shardingItemParameters 分片的参数
	 * @param jobParameters 任务执行时需要传进去的数据。
	 * @return 任务启动是否成功
	 */
	public boolean startTask(final String jobName,final SimpleJob simpleJob, final String cron, final int shardingTotalCount,
			final String shardingItemParameters, final String jobParameters);

	/**
	 * 删除不需要的任务
	 * @param jobName 任务名称，规定:任务的ID
	 * @return 删除是否成功
	 */
	public boolean deleteTask(final  String jobName);

}
