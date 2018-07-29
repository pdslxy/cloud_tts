package com.enerbos.cloud.tts.microservice.controller;

import com.enerbos.cloud.tts.microservice.service.TimeTaskService;
import com.enerbos.cloud.tts.microservice.task.EamInventoryTaskTimeJob;
import com.enerbos.cloud.tts.microservice.task.EamMaintenancePlanTaskTimeJob;
import com.enerbos.cloud.tts.microservice.task.EamPatrolPlanTaskTimeJob;
import com.enerbos.cloud.tts.microservice.task.EamRoutineWorksheetTaskTimeJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * All rights Reserved, Designed By 翼虎能源
 * Copyright:    Copyright(C) 2015-2017
 * Company   北京翼虎能源科技有限公司
 *
 * @author 庞慧东
 * @version 1.0.0
 * @date 2017年9月1日
 * @Description eam定时任务执行
 */
@RestController
public class EamTimerTaskController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private TimeTaskService timeTaskService;
    @Autowired
    private EamRoutineWorksheetTaskTimeJob job;
    @Autowired
    private DiscoveryClient client;

    @Autowired
    private EamMaintenancePlanTaskTimeJob eamMaintenancePlanTaskTimeJob;

    @Autowired
    private EamPatrolPlanTaskTimeJob eamPatrolPlanTaskTimeJob;


    @Autowired
    private EamInventoryTaskTimeJob  eamInventoryTaskTimeJob;
    /**
     * startEamMaintenancePlanTask:启动预防性维护计划生成维保工单定时任务
     * startTask: 启动一个时间任务
     *
     * @param cron                   时间表达式
     * @param shardingTotalCount     分片总数
     * @param shardingItemParameters 分片参数
     * @param jobParameters          任务的参数
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/tts/micro/task/startEamMaintenancePlanTask")
    public boolean startEamMaintenancePlanTask(
            @RequestParam("jobName") String jobName,
            @RequestParam("cron") String cron,
            @RequestParam(value = "shardingTotalCount", required = false) int shardingTotalCount,
            @RequestParam(value = "shardingItemParameters", required = false) String shardingItemParameters,
            @RequestParam("jobParameters") String jobParameters) {
        try {
            timeTaskService.deleteTask(jobName);
            boolean result = timeTaskService.startTask(jobName, eamMaintenancePlanTaskTimeJob, cron, shardingTotalCount, shardingItemParameters, jobParameters);
            return true;
        } catch (Exception e) {
            logger.error("-------startEamMaintenancePlanTask-----", e);
        }
        return false;
    }


    /**
     * startEamMaintenancePlanTask:启动重订购
     * startTask: 启动一个时间任务
     *
     * @param cron                   时间表达式
     * @param shardingTotalCount     分片总数
     * @param shardingItemParameters 分片参数
     * @param jobParameters          任务的参数
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/tts/micro/task/startEamInventoryTask")
    public boolean startEamInventoryTask(
            @RequestParam("jobName") String jobName,
            @RequestParam("cron") String cron,
            @RequestParam(value = "shardingTotalCount", required = false) int shardingTotalCount,
            @RequestParam(value = "shardingItemParameters", required = false) String shardingItemParameters,
            @RequestParam("jobParameters") String jobParameters) {
        try {
            logger.info("----param:{},{},{},{},{}",jobName,cron,shardingTotalCount,shardingItemParameters,jobParameters);
            timeTaskService.deleteTask(jobName);
            boolean result = timeTaskService.startTask(jobName, eamInventoryTaskTimeJob, cron, shardingTotalCount, shardingItemParameters, jobParameters);
            return true;
        } catch (Exception e) {
            logger.error("-------startEamInventoryTask-----", e);
        }
        return false;
    }

    /**
     * startEamRoutineWorksheet:启动例行工作定时生成例行工作单
     * startTask: 启动一个时间任务
     *
     * @param cron                   时间表达式
     * @param shardingTotalCount     分片总数
     * @param shardingItemParameters 分片参数
     * @param jobParameters          任务的参数
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/tts/micro/task/startEamRoutineWorksheet")
    public boolean startEamRoutineWorksheet(@RequestParam(value = "jobName", required = true) String jobName,
                                            @RequestParam(value = "cron", required = true) String cron,
                                            @RequestParam(value = "shardingTotalCount", required = false) int shardingTotalCount,
                                            @RequestParam(value = "shardingItemParameters", required = false) String shardingItemParameters,
                                            @RequestParam(value = "jobParameters", required = true) String jobParameters) {
        try {
            ServiceInstance instance = client.getLocalServiceInstance();
            logger.info("/tts/micro/task/startEamRoutineWorksheet, host: [{}:{}], service_id: {}, jobName: {}, cron: {}, shardingTotalCount: {}, shardingItemParameters: {}, jobParameters: {}",instance.getHost(), instance.getPort(), instance.getServiceId(), jobName, shardingTotalCount,shardingItemParameters,jobParameters);
            //	EamRoutineWorksheetTaskTimeJob job=new EamRoutineWorksheetTaskTimeJob();
            timeTaskService.deleteTask(jobName);
            boolean result = timeTaskService.startTask(jobName, job, cron, shardingTotalCount, shardingItemParameters, jobParameters);
            logger.info("startEamRoutineWorksheet-2-启动返回值：" + result);
            return true;
        } catch (Exception e) {
            logger.error("-------startEamRoutineWorksheet-----", e);
        }
        return false;
    }

    /**
     * startEamMaintenancePlanTask:启动巡检计划生成巡检工单定时任务
     * startTask: 启动一个时间任务
     *
     * @param cron                   时间表达式
     * @param shardingTotalCount     分片总数
     * @param shardingItemParameters 分片参数
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/tts/micro/task/startEamPatrolPlanTask")
    public boolean startEamPatrolPlanTask(
            @RequestParam(value = "jobName") String jobName,
            @RequestParam("cron") String cron,
            @RequestParam("shardingTotalCount") int shardingTotalCount,
            @RequestParam(value = "shardingItemParameters", required = false) String shardingItemParameters,
            @RequestParam("jobParameters") String jobParameters,
            @RequestParam(value = "oldFrequencyIds", required = false) List<String> oldFrequencyIds,
            @RequestParam(value = "newFrequencyIds", required = false) List<String> newFrequencyIds) {
        try {
            if (oldFrequencyIds != null) {
                for (String frequencyId : oldFrequencyIds) {
                    try {
                        timeTaskService.deleteTask(jobName + "#" + frequencyId);
                    } catch (Exception e) {
                        logger.error("-------删除定时任务出错-----定时任务Id", jobName + "#" + frequencyId);
                    }
                }
            }
            if (newFrequencyIds != null) {
                for (String frequencyId : newFrequencyIds) {
                    try {
                        timeTaskService.startTask(jobName + "#" + frequencyId, eamPatrolPlanTaskTimeJob, cron, shardingTotalCount, shardingItemParameters, jobName + "#" + frequencyId);
                    } catch (Exception e) {
                        logger.error("-------生成定时任务出错-----定时任务Id", jobName + "#" + frequencyId);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("-------startEamPatrolPlanTask-----", e);
        }
        return false;
    }

    /**
     * deleteEamTask:删除指定EAM定时任务
     * startTask: 启动一个时间任务
     *
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/tts/micro/task/deleteEamTask")
    public boolean deleteEamTask(@RequestParam(value = "jobName", required = true) String jobName) {
        try {
            timeTaskService.deleteTask(jobName);
            return true;
        } catch (Exception e) {
            logger.error("-------deleteEamTask-----", e);
        }
        return false;
    }
}
