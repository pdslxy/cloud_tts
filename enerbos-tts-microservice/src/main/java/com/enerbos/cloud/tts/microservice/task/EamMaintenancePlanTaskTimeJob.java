package com.enerbos.cloud.tts.microservice.task;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.enerbos.cloud.eam.client.*;
import com.enerbos.cloud.eam.contants.Common;
import com.enerbos.cloud.eam.contants.WorkOrderCommon;
import com.enerbos.cloud.eam.vo.*;
import com.enerbos.cloud.uas.client.PersonAndUserClient;
import com.enerbos.cloud.uas.client.SiteClient;
import com.enerbos.cloud.uas.client.UgroupClient;
import com.enerbos.cloud.uas.vo.personanduser.PersonAndUserVoForDetail;
import com.enerbos.cloud.uas.vo.site.SiteVoForDetail;
import com.enerbos.cloud.uas.vo.ugroup.UgroupVoForDetail;
import com.enerbos.cloud.wfs.client.ProcessTaskClient;
import com.enerbos.cloud.wfs.client.WorkflowClient;
import com.enerbos.cloud.wfs.vo.ProcessVo;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * All rights Reserved, Designed By 翼虎能源
 * Copyright:    Copyright(C) 2015-2017
 * Company   北京翼虎能源科技有限公司
 *
 * @author 庞慧东
 * @version 1.0
 * @date 2017年08月18日
 * @Description 预防性维护计划生成维保工单定时任务
 */
@Component
public class EamMaintenancePlanTaskTimeJob implements SimpleJob {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private MaintenanceMaintenancePlanClient maintenanceMaintenancePlanClient;

	@Autowired
	private MaintenanceMaintenancePlanActiveTimeClient maintenanceMaintenancePlanActiveTimeClient;

	@Autowired
	private MaintenanceWorkOrderClient maintenanceWorkOrderClient;

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
	private MaintenanceMaintenancePlanAssetClient maintenanceMaintenancePlanAssetClient;

	@Autowired
	private MaintenanceJobStandardTaskClient maintenanceJobStandardTaskClient;

	@Resource
	private MaintenanceWorkOrderAssetClient maintenanceWorkOrderAssetClient;

	@Resource
	private MaintenanceWorkOrderStepClient maintenanceWorkOrderStepClient;

	@Autowired
	private UserGroupDomainClient userGroupDomainClient;

	@Autowired
	private PersonAndUserClient personAndUserClient;

	/**
	 * 预防性维护计划定时生成维保工单
	 */
	@Override
	public void execute(ShardingContext shardingContext) {
		String id=shardingContext.getJobParameter();
		logger.info("--------预防性维护计划生成维保工单定时任务--------当前时间:{},id:{}",new Date(),id);
		MaintenanceMaintenancePlanVo maintenancePlanVo=maintenanceMaintenancePlanClient.findMaintenancePlanById(id);
		if (maintenancePlanVo == null||!Common.MAINTENANCE_PLAN_STATUS_ACTIVITY.equals(maintenancePlanVo.getStatus())) {
			logger.error("-------无数据或预防性维护计划状态不是活动-----------id:{},MaintenanceMaintenancePlanVo:{}",id,maintenancePlanVo);
			return;
		}
		if (isAutoCreateMaintenancePlanOrder(maintenancePlanVo)) {
			createMaintenancePlanOrder(maintenancePlanVo);
		}
	}

	/**
	 * isAutoCreatePMOrder:判断当前时间是否生成维保工单
	 * @param maintenanceMaintenancePlanVo 预防性维护计划
	 * @return boolean 是否执行
	 */
	private boolean isAutoCreateMaintenancePlanOrder(MaintenanceMaintenancePlanVo maintenanceMaintenancePlanVo){
		boolean flag = false;
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		Date nextDate = maintenanceMaintenancePlanVo.getNextDate();
		// 日期判断
		if (null != nextDate && (DateFormatUtils.format(calendar.getTime(), "yyyy-MM-dd").equals(DateFormatUtils.format(nextDate, "yyyy-MM-dd")))) {
			flag = true;
			//是否延迟生成工单
			if (null != maintenanceMaintenancePlanVo.getExtDate() && !DateFormatUtils.format(calendar.getTime(), "yyyy-MM-dd").equals(DateFormatUtils.format(maintenanceMaintenancePlanVo.getExtDate(), "yyyy-MM-dd"))) {
				flag = false;
			}
		} else if (null != maintenanceMaintenancePlanVo.getExtDate() && (DateFormatUtils.format(calendar.getTime(), "yyyy-MM-dd").equals(DateFormatUtils.format(maintenanceMaintenancePlanVo.getExtDate(),"yyyy-MM-dd")))) {
			flag = true;
			maintenanceMaintenancePlanVo.setNextDate(maintenanceMaintenancePlanVo.getExtDate());
			maintenanceMaintenancePlanVo.setExtDate(null);
		}
		if(flag){
			flag = checkPmAuth(maintenanceMaintenancePlanVo, calendar, flag);
		}
		maintenanceMaintenancePlanClient.saveMaintenancePlan(maintenanceMaintenancePlanVo);
		return flag;
	}

	private Boolean checkPmAuth(MaintenanceMaintenancePlanVo maintenanceMaintenancePlanVo, Calendar calendar, Boolean flag) {
		//星期判断
		String weekFrequency=maintenanceMaintenancePlanVo.getWeekFrequency();

		String dayOfWeek=String.valueOf(calendar.get(Calendar.DAY_OF_WEEK)-1);
		dayOfWeek=dayOfWeek.equals("0")?"7":dayOfWeek;
		//如果字段里没有当前星期，置为FALSE
		if (StringUtils.isNotBlank(weekFrequency)&&weekFrequency.indexOf(dayOfWeek)<0) {
			flag = false;
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			maintenanceMaintenancePlanVo.setNextDate(calendar.getTime());
		}
		if(flag){
			// 季节时间判断
			List<MaintenanceMaintenancePlanActiveTimeVo> mps = maintenanceMaintenancePlanActiveTimeClient.findAllMaintenancePlanActiveTime(maintenanceMaintenancePlanVo.getId());
			if (mps.size() > 0) {
				flag = false;
				for (MaintenanceMaintenancePlanActiveTimeVo mp : mps) {
					Date start=mp.getStartDate();
					Date end=mp.getEndDate();
					if (mp.getEndDate().getTime()>=mp.getStartDate().getTime()) {
						start.setYear(calendar.get(Calendar.YEAR));
						end.setYear(calendar.get(Calendar.YEAR));
					}else {
						start.setYear(calendar.get(Calendar.YEAR)-1);
						end.setYear(calendar.get(Calendar.YEAR));
					}
					if (calendar.getTimeInMillis()>=start.getTime()&&calendar.getTimeInMillis()<=end.getTime()+1000*60*60*24) {
						flag = true;
						break;
					}
				}
				if(!flag){//顺延日期，以期下次生成
					calendar.setTime(maintenanceMaintenancePlanVo.getNextDate());
					if ("day".equals(maintenanceMaintenancePlanVo.getFrequencyUnit())) {
						calendar.add(Calendar.DAY_OF_MONTH, maintenanceMaintenancePlanVo.getFrequency());
					}
					if ("month".equals(maintenanceMaintenancePlanVo.getFrequencyUnit())) {
						calendar.add(Calendar.MONTH, maintenanceMaintenancePlanVo.getFrequency());
					}
					if ("week".equals(maintenanceMaintenancePlanVo.getFrequencyUnit())) {
						calendar.add(Calendar.DAY_OF_MONTH, maintenanceMaintenancePlanVo.getFrequency() * 7);
					}
					if ("year".equals(maintenanceMaintenancePlanVo.getFrequencyUnit())) {
						calendar.add(Calendar.YEAR, maintenanceMaintenancePlanVo.getFrequency());
					}
					//如果字段里没有当前星期，置为FALSE
					if (StringUtils.isNotBlank(weekFrequency)&&weekFrequency.indexOf(calendar.get(Calendar.DAY_OF_WEEK))<0) {
						calendar.add(Calendar.DAY_OF_MONTH, 1);
						maintenanceMaintenancePlanVo.setNextDate(calendar.getTime());
					}
					maintenanceMaintenancePlanVo.setNextDate(calendar.getTime());
				}
			}
		}
		return flag;
	}

	/**
	 * createPMOrder:根据预防性维护计划创建维保工单
	 * @param maintenanceMaintenancePlan 预防性维护计划
	 * @return boolean 是否执行
	 */
	private void createMaintenancePlanOrder(MaintenanceMaintenancePlanVo maintenanceMaintenancePlan){
		Calendar calendar = Calendar.getInstance();
		if(maintenanceMaintenancePlan.getNextDate()!=null){
			calendar.setTime(maintenanceMaintenancePlan.getNextDate());
		}else if(maintenanceMaintenancePlan.getExtDate()!=null){
			calendar.setTime(maintenanceMaintenancePlan.getExtDate());
		}else {
			logger.error("自动生成工单异常，预防性维护编号为："+maintenanceMaintenancePlan.getMaintenancePlanNum());
			return;
		}
		if ("day".equals(maintenanceMaintenancePlan.getFrequencyUnit())) {
			calendar.add(Calendar.DAY_OF_MONTH, maintenanceMaintenancePlan.getFrequency());
		}
		if ("month".equals(maintenanceMaintenancePlan.getFrequencyUnit())) {
			calendar.add(Calendar.MONTH, maintenanceMaintenancePlan.getFrequency());
		}
		if ("week".equals(maintenanceMaintenancePlan.getFrequencyUnit())) {
			calendar.add(Calendar.DAY_OF_MONTH, maintenanceMaintenancePlan.getFrequency() * 7);
		}
		if ("year".equals(maintenanceMaintenancePlan.getFrequencyUnit())) {
			calendar.add(Calendar.YEAR, maintenanceMaintenancePlan.getFrequency());
		}
//		//星期判断
//		String weekFrequency=maintenanceMaintenancePlan.getWeekFrequency();
//		String dayOfWeek=String.valueOf(calendar.get(Calendar.DAY_OF_WEEK)-1);
//		dayOfWeek=dayOfWeek.equals("0")?"7":dayOfWeek;
//		//如果字段里没有当前星期
//		if (StringUtils.isNotBlank(weekFrequency)&&weekFrequency.indexOf(dayOfWeek)<0) {
//			calendar.add(Calendar.DAY_OF_MONTH, 1);
//			maintenanceMaintenancePlan.setNextDate(calendar.getTime());
//		}
		maintenanceMaintenancePlan.setLastStartDate(new Date());//上次开始时间
		maintenanceMaintenancePlan.setNextDate(calendar.getTime());
		//第一次进来没有延迟时间，是否根据上次工单信息计算下次到期频率是FALSE，之后设置为TRUE
		maintenanceMaintenancePlan.setUseTargetDate(true);
		//设置延长日期为空
		maintenanceMaintenancePlan.setExtDate(null);
		maintenanceMaintenancePlanClient.saveMaintenancePlan(maintenanceMaintenancePlan);

		String siteId = maintenanceMaintenancePlan.getSiteId();
		String result="";
		SiteVoForDetail site=new SiteVoForDetail();
		if (StringUtils.isNotBlank(siteId)) {
			site=siteClient.findById(maintenanceMaintenancePlan.getSiteId());
			if (site == null) {
				logger.error("站点编码无效！siteId:{}",siteId);
				return;
			}
			result=codeGeneratorClient.getCodegenerator(site.getOrgId(),siteId, Common.WORK_ORDER_MODEL_KEY);
			if (null==result||"".equals(result)) {
				logger.error("编码生成规则内容读取失败！siteId:{},modelKey:{}",siteId,Common.WORK_ORDER_MODEL_KEY);
				return;
			}
		}else {
			logger.error("站点为空！siteId:{}",siteId);
			return;
		}
		PersonAndUserVoForDetail system=personAndUserClient.findByLoginName(Common.SYSTEM_USER_LOGINNAME);
		String systemId="";
		if (system != null) {
			systemId=system.getPersonId();
		}
		MaintenanceWorkOrderForCommitVo maintenanceWorkOrderForCommitVo=new MaintenanceWorkOrderForCommitVo();
		maintenanceWorkOrderForCommitVo.setWorkOrderNum(result);
		maintenanceWorkOrderForCommitVo.setDescription(maintenanceMaintenancePlan.getDescription());
		maintenanceWorkOrderForCommitVo.setWorkType(maintenanceMaintenancePlan.getWorkOrderType());
		maintenanceWorkOrderForCommitVo.setProjectType(maintenanceMaintenancePlan.getProjectType());
		maintenanceWorkOrderForCommitVo.setStatus(maintenanceMaintenancePlan.getWorkOrderStatus());
		maintenanceWorkOrderForCommitVo.setLocationId(maintenanceMaintenancePlan.getLocationId());
		maintenanceWorkOrderForCommitVo.setIncidentLevel(maintenanceMaintenancePlan.getIncidentLevel());
		maintenanceWorkOrderForCommitVo.setSiteId(maintenanceMaintenancePlan.getSiteId());
		maintenanceWorkOrderForCommitVo.setUdisww(maintenanceMaintenancePlan.getUdisww());
		maintenanceWorkOrderForCommitVo.setMaintenancePlanNum(maintenanceMaintenancePlan.getMaintenancePlanNum());
		maintenanceWorkOrderForCommitVo.setReportId(systemId);
		maintenanceWorkOrderForCommitVo.setReportDate(new Date());
		maintenanceWorkOrderForCommitVo.setOrgId(maintenanceMaintenancePlan.getOrgId());
		maintenanceWorkOrderForCommitVo.setContractId(maintenanceMaintenancePlan.getContractId());
		maintenanceWorkOrderForCommitVo.setCreateUser(systemId);
		maintenanceWorkOrderForCommitVo=maintenanceWorkOrderClient.saveWorkOrderCommit(maintenanceWorkOrderForCommitVo);
		if (maintenanceWorkOrderForCommitVo == null||StringUtils.isBlank(maintenanceWorkOrderForCommitVo.getId())) {
			logger.error("保存工单提报失败！, maintenanceWorkOrderForCommitVo: {}",maintenanceWorkOrderForCommitVo);
			return;
		}
		//将预防性维护计划关联设备插入到生成的维保工单关联设备中
		List<String> assets=maintenanceMaintenancePlanAssetClient.findAllMaintenancePlanAsset(maintenanceMaintenancePlan.getId());
		List<MaintenanceWorkOrderAssetVo> eamWorkOrderAssetVoList=new ArrayList<>();
		MaintenanceWorkOrderAssetVo maintenanceWorkOrderAssetVo;
		if (assets != null&&assets.size()>0) {
			for (String asset : assets) {
				maintenanceWorkOrderAssetVo=new MaintenanceWorkOrderAssetVo();
				maintenanceWorkOrderAssetVo.setAssetId(asset);
				maintenanceWorkOrderAssetVo.setWorkOrderId(maintenanceWorkOrderForCommitVo.getId());
				eamWorkOrderAssetVoList.add(maintenanceWorkOrderAssetVo);
			}
		}
		maintenanceWorkOrderAssetClient.saveWorkOrderAsset(eamWorkOrderAssetVoList);

		//工单提报
		//启动流程
		//设置流程变量
		Map<String, Object> variables = new HashMap<String, Object>();
		//业务主键
		String businessKey = maintenanceWorkOrderForCommitVo.getId();
		//流程key,key为维保固定前缀+站点code
		String code=site.getCode();
		String processKey = Common.WORK_ORDER_WFS_PROCESS_KEY + code;
		ProcessVo processVo=new ProcessVo();
		processVo.setBusinessKey(businessKey);
		processVo.setProcessKey(processKey);
		processVo.setUserId(systemId);
		variables.put(WorkOrderCommon.WORK_ORDER_SUBMIT_USER, systemId);
		variables.put(Common.ORDER_NUM,maintenanceWorkOrderForCommitVo.getWorkOrderNum());
		variables.put(Common.ORDER_DESCRIPTION,maintenanceWorkOrderForCommitVo.getDescription());
		logger.debug("/eam/open/workorder/commit, processKey: {}", processKey);
		processVo=workflowClient.startProcess(variables, processVo);

		if (null==processVo || "".equals(processVo.getProcessInstanceId())) {
			logger.error("流程启动失败");
			return;
		}
		//提报，修改基本字段保存
		maintenanceWorkOrderForCommitVo.setProcessInstanceId(processVo.getProcessInstanceId());
		maintenanceWorkOrderForCommitVo = maintenanceWorkOrderClient.saveWorkOrderCommit(maintenanceWorkOrderForCommitVo);
		//查询分派组签收人员
		variables = new HashMap<String, Object>();
		List<String> userList = new ArrayList<>();
		UserGroupDomainVo vo=userGroupDomainClient.
				findUserGroupDomainByDomainValueAndDomainNum(maintenanceMaintenancePlan.getProjectType(),
				WorkOrderCommon.WORK_ORDER_PROJECT_TYPE, maintenanceMaintenancePlan.getOrgId(), siteId,Common.USERGROUP_ASSOCIATION_TYPE_ALL);
		UgroupVoForDetail voForDetail=  ugroupClient.findById(vo.getUserGroupId());
		voForDetail.getUsers().stream().forEach(u->{
					PersonAndUserVoForDetail person= personAndUserClient.findByPersonId(u.getPersonId());
					if(person!=null){//清空无关数据
						userList.add(person.getPersonId());
					}
				}
		);
		if(null==userList||userList.size() <= 0){
			logger.error("维保工单分派组下没有人员,请联系管理员添加!!userGroup{}", voForDetail.getCode());
			return;
		}
		variables.put(WorkOrderCommon.WORK_ORDER_ASSIGN_USER, StringUtils.join(userList, ","));
		variables.put("description", "");
		variables.put("status", WorkOrderCommon.WORK_ORDER_STATUS_DFP);
		Boolean processMessage = processTaskClient.completeByProcessInstanceId(maintenanceWorkOrderForCommitVo.getProcessInstanceId(), variables);
		if (Objects.isNull(processMessage) || !processMessage) {
			logger.error("流程操作异常。maintenanceWorkOrderForCommitVo{},variables{}",maintenanceWorkOrderForCommitVo,variables);
			return;
		}
		//提报，修改基本字段保存
		maintenanceWorkOrderForCommitVo.setStatus(WorkOrderCommon.WORK_ORDER_STATUS_DFP);
		maintenanceWorkOrderForCommitVo.setStatusDate(new Date());//状态日期
		maintenanceWorkOrderForCommitVo=maintenanceWorkOrderClient.saveWorkOrderCommit(maintenanceWorkOrderForCommitVo);
		MaintenanceWorkOrderForAssignVo maintenanceWorkOrderForAssignVo=maintenanceWorkOrderClient.findWorkOrderAssignById(maintenanceWorkOrderForCommitVo.getId());
		maintenanceWorkOrderForAssignVo.setJobStandardId(maintenanceMaintenancePlan.getJobStandardId());
		maintenanceWorkOrderForAssignVo=maintenanceWorkOrderClient.saveWorkOrderAssign(maintenanceWorkOrderForAssignVo);

		//拉取关联作业标准的任务步骤
		if (StringUtils.isNotBlank(maintenanceMaintenancePlan.getJobStandardId())) {
			List<MaintenanceJobStandardTaskVo> list=maintenanceJobStandardTaskClient.findJobStandardTaskByJobStandardId(maintenanceMaintenancePlan.getJobStandardId());
			if (!list.isEmpty()) {
				MaintenanceWorkOrderStepVo maintenanceWorkOrderStepVo;
				for (MaintenanceJobStandardTaskVo maintenanceJobStandardTaskVo:list) {
					maintenanceWorkOrderStepVo=new MaintenanceWorkOrderStepVo();
					maintenanceWorkOrderStepVo.setWorkOrderId(maintenanceWorkOrderForAssignVo.getId());
					maintenanceWorkOrderStepVo.setDescription(maintenanceJobStandardTaskVo.getDescription());
					maintenanceWorkOrderStepVo.setDuration(maintenanceJobStandardTaskVo.getTaskDuration());
					maintenanceWorkOrderStepVo.setQualityStandard(maintenanceJobStandardTaskVo.getQualityStandard());
					maintenanceWorkOrderStepVo.setStep(maintenanceJobStandardTaskVo.getTaskSequence());
					maintenanceWorkOrderStepClient.saveOrderStep(maintenanceWorkOrderStepVo);
				}
			}
		}
		logger.info("--------预防性维护计划定时生成维保工单结束--------当前时间:{},MaintenanceMaintenancePlanVo:{},MaintenanceWorkOrderForCommitVo:{}",new Date(),maintenanceMaintenancePlan,maintenanceWorkOrderForCommitVo);
	}
}
