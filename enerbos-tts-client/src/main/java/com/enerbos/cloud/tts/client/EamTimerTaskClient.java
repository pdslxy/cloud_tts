package com.enerbos.cloud.tts.client;

import feign.hystrix.FallbackFactory;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * All rights Reserved, Designed By 翼虎能源
 * Copyright:    Copyright(C) 2015-2017
 * Company   北京翼虎能源科技有限公司
 *
 * @author 庞慧东
 * @version 1.0
 * @date 2017年09月01日
 * @Description eam定时任务执行client
 */
@FeignClient(name = "enerbos-tts-microservice", fallbackFactory = EamTimerTaskClientFallback.class)
public interface EamTimerTaskClient {

    /**
     * startEamMaintenancePlanTask:启动预防性维护计划生成维保工单定时任务
     * @param cron 时间表达式
     * @param shardingTotalCount 分片总数
     * @param shardingItemParameters 分片参数
     * @param jobParameters 任务的参数
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/tts/micro/task/startEamMaintenancePlanTask")
    public boolean startEamMaintenancePlanTask(
            @RequestParam("jobName") String jobName,
            @RequestParam("cron") String cron,
            @RequestParam(value = "shardingTotalCount",required = false) int shardingTotalCount,
            @RequestParam(value = "shardingItemParameters",required = false) String shardingItemParameters,
            @RequestParam("jobParameters") String jobParameters);


    /**
     * startEamMaintenancePlanTask:启动重订购
     * @param cron 时间表达式
     * @param shardingTotalCount 分片总数
     * @param shardingItemParameters 分片参数
     * @param jobParameters 任务的参数
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/tts/micro/task/startEamInventoryTask")
    public boolean startEamInventoryTask(
            @RequestParam("jobName") String jobName,
            @RequestParam("cron") String cron,
            @RequestParam(value = "shardingTotalCount",required = false) int shardingTotalCount,
            @RequestParam(value = "shardingItemParameters",required = false) String shardingItemParameters,
            @RequestParam("jobParameters") String jobParameters);
    /**
     * startEamMaintenancePlanTask:启动预防性维护计划生成维保工单定时任务
     * startTask: 启动一个时间任务
     * @param cron 时间表达式
     * @param shardingTotalCount 分片总数
     * @param shardingItemParameters 分片参数
     * @param jobParameters 任务的参数
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/tts/micro/task/startEamRoutineWorksheet")
    public boolean startEamRoutineWorksheet(
            @RequestParam(value = "jobName", required = true) String jobName,
            @RequestParam(value = "cron", required = true) String cron,
            @RequestParam(value = "shardingTotalCount", required = false) int shardingTotalCount,
            @RequestParam(value = "shardingItemParameters", required = false) String shardingItemParameters,
            @RequestParam(value = "jobParameters", required = true) String jobParameters);

    /**
     * startEamMaintenancePlanTask:启动巡检计划生成巡检工单定时任务
     * startTask: 启动一个时间任务
     * @param cron 时间表达式
     * @param shardingTotalCount 分片总数
     * @param shardingItemParameters 分片参数
     * @param jobParameters 任务的参数
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/tts/micro/task/startEamPatrolPlanTask")
    public boolean startEamPatrolPlanTask(@RequestParam("jobName") String jobName,
                                          @RequestParam("cron") String cron,
                                          @RequestParam("shardingTotalCount") int shardingTotalCount,
                                          @RequestParam("shardingItemParameters") String shardingItemParameters,
                                          @RequestParam("jobParameters") String jobParameters,
                                          @RequestParam("oldFrequencyIds") List oldFrequencyIds,
                                          @RequestParam("newFrequencyIds") List newFrequencyIds);

    /**
     * deleteEamTask:删除指定EAM定时任务
     * startTask: 启动一个时间任务
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/tts/micro/task/deleteEamTask")
    public boolean deleteEamTask(@RequestParam(value = "jobName", required = true) String jobName);
}

@Component
class EamTimerTaskClientFallback implements FallbackFactory<EamTimerTaskClient> {
    @Override
    public EamTimerTaskClient create(Throwable throwable) {

        return new EamTimerTaskClient() {
            @Override
            public boolean startEamMaintenancePlanTask(String jobName, String cron, int shardingTotalCount, String shardingItemParameters, String jobParameters) {
                throw new RuntimeException(throwable.getMessage());
            }

            @Override
            public boolean startEamInventoryTask(String jobName, String cron, int shardingTotalCount, String shardingItemParameters, String jobParameters) {
                throw new RuntimeException(throwable.getMessage());
            }

            @Override
            public boolean startEamRoutineWorksheet(
                    @RequestParam("jobName") String jobName,
                    @RequestParam("cron") String cron,
                    @RequestParam("shardingTotalCount") int shardingTotalCount,
                    @RequestParam("shardingItemParameters") String shardingItemParameters,
                    @RequestParam("jobParameters") String jobParameters) {
                throw  new RuntimeException(throwable.getMessage());
            }

            @Override
            public boolean startEamPatrolPlanTask(@RequestParam("jobName") String jobName, @RequestParam("cron") String cron, @RequestParam("shardingTotalCount") int shardingTotalCount, @RequestParam("shardingItemParameters") String shardingItemParameters, @RequestParam("jobParameters") String jobParameters, @RequestParam("oldFrequencyList") List oldFrequencyIds, @RequestParam("newFrequencyList") List newFrequencyIds) {
                throw new RuntimeException(throwable.getMessage());
            }

            @Override
            public boolean deleteEamTask(String jobName) {
                throw new RuntimeException(throwable.getMessage());
            }
        };



    }
}