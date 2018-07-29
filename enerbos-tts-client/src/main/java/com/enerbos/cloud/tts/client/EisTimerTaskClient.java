package com.enerbos.cloud.tts.client;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import feign.hystrix.FallbackFactory;

/**
 * 
 * All rights Reserved, Designed By 翼虎能源
 * Copyright:    Copyright(C) 2015-2017
 * Company   北京翼虎能源科技有限公司
 *
 * @author 李伟龙
 * @version 1.0
 * @date 2017年11月1日 下午3:51:51
 * @Description TODO 方法描述
 */
@FeignClient(name = "enerbos-tts-microservice", fallbackFactory = EisTimerTaskClientFallback.class)
public interface EisTimerTaskClient {

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
			@RequestParam("jobParameters") String jobParameters);

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
			@RequestParam("jobParameters") String jobParameters);

}

@Component
class EisTimerTaskClientFallback implements FallbackFactory<EisTimerTaskClient> {
	@Override
	public EisTimerTaskClient create(Throwable cause) {

		return new EisTimerTaskClient() {
			@Override
			public boolean startCreateTask(String jobName, String cron, int shardingTotalCount,
					String shardingItemParameters, String jobParameters) {
				return false;
			}

			@Override
			public boolean startCreateReport(String jobName, String cron, int shardingTotalCount,
					String shardingItemParameters, String jobParameters) {
				// TODO Auto-generated method stub
				return false;
			}

		};
	}
}