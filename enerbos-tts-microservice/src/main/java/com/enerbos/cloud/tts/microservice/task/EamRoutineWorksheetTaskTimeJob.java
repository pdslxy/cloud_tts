package com.enerbos.cloud.tts.microservice.task;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.enerbos.cloud.common.EnerbosException;
import com.enerbos.cloud.common.EnerbosMessage;
import com.enerbos.cloud.eam.client.*;
import com.enerbos.cloud.eam.contants.Common;
import com.enerbos.cloud.eam.contants.DispatchWorkOrderCommon;
import com.enerbos.cloud.eam.contants.HeadquartersDailyTaskCommon;
import com.enerbos.cloud.eam.contants.WorkOrderCommon;
import com.enerbos.cloud.eam.vo.*;
import com.enerbos.cloud.tts.microservice.service.TimeTaskService;
import com.enerbos.cloud.uas.client.PersonAndUserClient;
import com.enerbos.cloud.uas.client.SiteClient;
import com.enerbos.cloud.uas.client.UgroupClient;
import com.enerbos.cloud.uas.vo.personanduser.PersonAndUserVoForDetail;
import com.enerbos.cloud.uas.vo.site.SiteVoForDetail;
import com.enerbos.cloud.uas.vo.ugroup.UgroupVoForDetail;
import com.enerbos.cloud.util.PrincipalUserUtils;
import com.enerbos.cloud.wfs.client.ProcessTaskClient;
import com.enerbos.cloud.wfs.client.WorkflowClient;
import com.enerbos.cloud.wfs.vo.ProcessVo;
import io.swagger.util.Json;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * All rights Reserved, Designed By 翼虎能源
 * Copyright:    Copyright(C) 2015-2017
 * Company   北京翼虎能源科技有限公司
 *
 * @author 张鹏伟
 * @version 1.0
 * @date 2017年09月05日
 * @Description 例行工作生成例行工作单
 */
@Component
public class EamRoutineWorksheetTaskTimeJob implements SimpleJob {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private CodeGeneratorClient codeGeneratorClient;
	@Autowired
	private SiteClient siteClient;
	@Resource
	private WorkflowClient workflowClient;
	@Autowired
	private ProcessTaskClient processTaskClient;
	@Autowired
	private UgroupClient ugroupClient;
	@Autowired
	private UserGroupDomainClient userGroupDomainClient;
	@Autowired
	private PersonAndUserClient personAndUserClient;
	@Autowired
    private  HeadquartersDailyClient   headquartersDailyClient;
	@Autowired
	private  HeadquartersDaliyTaskClient headquartersDaliyTaskClient;

	@Autowired
	private TimeTaskService timeTaskService;
	/**
	 * 例行工作生成例行工作单定时任务
	 */
	@Override
	public void execute(ShardingContext shardingContext) {
		String id=shardingContext.getJobParameter();
		logger.info("--------例行工作生成例行工作单定时任务--------当前时间:{},id:{}",new Date(),id);
		HeadquartersDailyVo headquartersDailyVo=headquartersDailyClient.findDetail(id);
		if (headquartersDailyVo == null||!Common.MAINTENANCE_PLAN_STATUS_ACTIVITY.equals(headquartersDailyVo.getStatus())) {
			logger.error("-------无数据或例行工作单状态不是活动-----------id:{},MaintenanceMaintenancePlanVo:{}",id,headquartersDailyVo);
			return;
		}
		if(isAutoCreateHeadquartersDaily( headquartersDailyVo)){
			createHeadquartersDaily(headquartersDailyVo);
		}


	}


	/**
	 * isAutoCreateHeadquartersDaily:判断当前时间是否生成例行工作单
	 * @param headquartersDailyVo 例行工作
	 * @return boolean 是否执行
	 */
	private boolean isAutoCreateHeadquartersDaily(HeadquartersDailyVo headquartersDailyVo){
		boolean flag = false;
		Date nowDate=new Date();//当前时间
		Date nextDate = headquartersDailyVo.getStartDate();//下一个开始时间需大于开始时间，小于结束时间
		Date startDate = headquartersDailyVo.getValidStartDate();//开始时间大于等于当前时间
		Date endDate = headquartersDailyVo.getValidEndDate();//结束时间小于当前时间

		Calendar calendar = Calendar.getInstance();
		if(headquartersDailyVo.getStartDate()!=null){
			calendar.setTime(headquartersDailyVo.getStartDate());
		}else {
			logger.error("自动生成工单异常，例行工作编号为："+headquartersDailyVo.getPlanNum());
		}
		if ("day".equals(headquartersDailyVo.getFrequency())) {
			calendar.add(Calendar.DAY_OF_MONTH, headquartersDailyVo.getTimes().intValue());
		}
		if ("month".equals(headquartersDailyVo.getFrequency())) {
			calendar.add(Calendar.MONTH, headquartersDailyVo.getTimes().intValue());
		}
		if ("week".equals(headquartersDailyVo.getFrequency())) {
			calendar.add(Calendar.DAY_OF_MONTH, headquartersDailyVo.getTimes().intValue() * 7);
		}
		if ("year".equals(headquartersDailyVo.getFrequency())) {
			calendar.add(Calendar.YEAR, headquartersDailyVo.getTimes().intValue());
		}
		List<HeadquartersDailyTaskVo>  taskVos=	headquartersDaliyTaskClient.
				findDailyTaskByplanId(headquartersDailyVo.getId(),headquartersDailyVo.getOrgId(),headquartersDailyVo.getSiteId());
		Date maxDate;
		boolean sign=true;
		if(headquartersDailyVo.getStatus()!=Common.MAINTENANCE_PLAN_STATUS_ACTIVITY){
			timeTaskService.deleteTask(headquartersDailyVo.getId());
			return  false;
		}
		if(taskVos.size()>0){
			 maxDate=taskVos.stream().map(HeadquartersDailyTaskVo::getCreateDate).max(Date::compareTo).get();
			maxDate.setMinutes(maxDate.getMinutes()+calendar.getTime().getMinutes());
			sign=maxDate.getTime()<=nowDate.getTime()?true:false;
		}
		if(startDate==null||nextDate==null||endDate==null||endDate.getTime()<new Date().getTime()){//超出有效期删除任务
			timeTaskService.deleteTask(headquartersDailyVo.getId());
			return flag;
		}
        if(startDate!=null&&nextDate!=null&&endDate!=null&&sign){
        	if(startDate.getTime()<=nextDate.getTime()&&nextDate.getTime()<=endDate.getTime()){//验证下一执行时间是否在有效的执行时间范围内
				if(startDate.getTime()<nowDate.getTime()&&endDate.getTime()>=nextDate.getTime()){//开始时间大于当前时间，结束时间小于当前时间
					flag=true;
				}else{
					logger.error("例行工作生成例行工作单 开始时间和结束时间不在有效的执行范围内!headquartersDailyVo:{} ",headquartersDailyVo);
				}
			}else{
        		logger.error("例行工作生成例行工作单 下一执行时间不在有效的执行范围内!headquartersDailyVo:{} ",headquartersDailyVo);
			}
		}else{
			logger.error("例行工作生成例行工作单 时间为null或者该数据不在有效生成规则内 !headquartersDailyVo:{} ",headquartersDailyVo);
		}
		return flag;
	}




	/**
	 * createHeadquartersDaily:根据例行工作生成例行工作单
	 * @param headquartersDailyVo 例行工作
	 * @return boolean 是否执行
	 */
	private void createHeadquartersDaily(HeadquartersDailyVo headquartersDailyVo){

		String siteId = headquartersDailyVo.getSiteId();
		String result="";
		SiteVoForDetail site=new SiteVoForDetail();
		if (StringUtils.isNotBlank(siteId)) {
			site=siteClient.findById(headquartersDailyVo.getSiteId());
			if (site == null) {
				logger.error("站点编码无效！siteId:{}",siteId);
				return;
			}
			result=codeGeneratorClient.getCodegenerator(site.getOrgId(),siteId, Common.DAILY_TASK_WFS_PROCESS_KEY);
			if (null==result||"".equals(result)) {
				logger.error("编码生成规则内容读取失败！siteId:{},modelKey:{}",siteId,Common.DAILY_TASK_WFS_PROCESS_KEY);
				return;
			}
		}else {
			logger.error("站点为空！siteId:{}",siteId);
			return;
		}

		HeadquartersDailyTaskVo vo=new HeadquartersDailyTaskVo();

		vo.setTaskName(headquartersDailyVo.getPlanName());
		vo.setTaskNum(result);
        vo.setPlanNum(headquartersDailyVo.getPlanNum());
		vo.setPlanId(headquartersDailyVo.getId());
		vo.setCheckItem(headquartersDailyVo.getCheckItem());
		vo.setWorkType(headquartersDailyVo.getWorkType());
		vo.setTaskProperty(headquartersDailyVo.getNature());
		vo.setSiteId(headquartersDailyVo.getSiteId());
		vo.setOrgId(headquartersDailyVo.getOrgId());
		vo.setCreateUser(Common.SYSTEM_USER);
		Calendar estimateCalendar = Calendar.getInstance();
		estimateCalendar.add(Calendar.DAY_OF_MONTH, headquartersDailyVo.getDeadline().intValue());
        vo.setEstimateDate(estimateCalendar.getTime());
		HeadquartersDailyTaskVo dailyTaskVo =headquartersDaliyTaskClient.save(vo);
		if (dailyTaskVo == null||StringUtils.isBlank(dailyTaskVo.getId())) {
			logger.error("保存工单保存失败！, headquartersDailyTaskVo: {}",dailyTaskVo);
		}
		//启动流程
		Map<String, Object> variables = new HashMap<>();
		String processKey = String.format("%s%s",HeadquartersDailyTaskCommon.DAILY_TASK_WFS_PROCESS_KEY,site.getCode());
		ProcessVo processVo = new ProcessVo();
		processVo.setBusinessKey(dailyTaskVo.getId());
		processVo.setProcessKey(processKey);
		processVo.setUserId(Common.SYSTEM_USER);
		variables.put("startUserId", Common.SYSTEM_USER);
		variables.put(HeadquartersDailyTaskCommon.DAILY_TASK_ACTIVITY_ASSIGNEE_SUBMIT_USER, Common.SYSTEM_USER);
		variables.put(Common.ORDER_NUM, dailyTaskVo.getTaskNum());
		variables.put("userId", Common.SYSTEM_USER);
		variables.put("description", "流程启动");
		processVo = workflowClient.startProcess(variables, processVo);
		//提报
        if(processVo==null){
			logger.info("--------例行工作生成例行工作单流程启动失败--------当前时间:{},dailyTaskVo:{},processVo:{}，variables：{},当前时间：{}",dailyTaskVo,processVo,variables,new Date());
		}else{//更新流程Id和状态
			//更新流程ID
			dailyTaskVo.setProcessInstanceId(processVo.getProcessInstanceId());
			dailyTaskVo.setReportPersonId(Common.SYSTEM_USER);
			headquartersDaliyTaskClient.save(dailyTaskVo);
		}
		if(processVo!=null){
			UserGroupDomainVo userGroupDomainVo=userGroupDomainClient.findUserGroupDomainByDomainValueAndDomainNum(dailyTaskVo.getWorkType(),Common.WORK_TYPE_DOMAIN,dailyTaskVo.getOrgId(),dailyTaskVo.getSiteId(),Common.USERGROUP_ASSOCIATION_TYPE_ALL);
			UgroupVoForDetail voForDetail=  ugroupClient.findById(userGroupDomainVo.getUserGroupId());
			List<PersonAndUserVoForDetail> detailList=new ArrayList<>();
			voForDetail.getUsers().stream().forEach(
					u->{
						PersonAndUserVoForDetail person= personAndUserClient.findByPersonId(u.getPersonId());
						if(person!=null){//清空无关数据
							person.setOrgs(null);
							person.setSites(null);
							person.setProducts(null);
							person.setLevels(null);
							person.setRoles(null);
							detailList.add(person);
						}
					}
			);
			String  personIdJoin=  detailList.stream().map(PersonAndUserVoForDetail::getPersonId).collect(Collectors.toList()).stream().reduce((a,b) -> a +"," +b).get();
			// //工单提报
			variables.clear();
			variables.put(DispatchWorkOrderCommon.DISPATCH_ORDER_ACTIVITY_ASSIGNEE_SUBMIT_USER, dailyTaskVo.getReportPersonId());
			variables.put(DispatchWorkOrderCommon.DISPATCH_ORDER_ACTIVITY_ASSIGNEE_ASSIGN_USER, personIdJoin);
			variables.put(DispatchWorkOrderCommon.DISPATCH_ORDER_PROCESS_STATUS_PASS, Boolean.TRUE);
			variables.put(DispatchWorkOrderCommon.DISPATCH_ORDER_PROCESS_STATUS_REJECT, Boolean.FALSE);
			variables.put(DispatchWorkOrderCommon.DISPATCH_ORDER_PROCESS_STATUS_CANCEL, Boolean.FALSE);
			variables.put("description", dailyTaskVo.getProcessDescription());
			variables.put("status", dailyTaskVo.getStatus());
			variables.put("userId", Common.SYSTEM_USER);
			//更新流程进度
			Boolean processMessage = processTaskClient.completeByProcessInstanceId(dailyTaskVo.getProcessInstanceId(), variables);
			if (Objects.isNull(processMessage) || !processMessage) {
				logger.info("--------例行工作生成例行工作单流程提报失败--------当前时间:{},dailyTaskVo:{},processVo:{}，variables：{}",new Date(),dailyTaskVo,processVo,variables);
			}else{
				dailyTaskVo.setStatus(HeadquartersDailyTaskCommon.DAILY_TASK_STATUS_DFP);
			}
			headquartersDaliyTaskClient.save(dailyTaskVo);
		}
		logger.info("--------例行工作生成例行工作单--------当前时间:{}",new Date());
	}
}
