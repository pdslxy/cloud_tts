package com.enerbos.cloud.tts.microservice.task;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.enerbos.cloud.common.EnerbosException;
import com.enerbos.cloud.eam.client.*;
import com.enerbos.cloud.eam.contants.Common;
import com.enerbos.cloud.eam.contants.PatrolCommon;
import com.enerbos.cloud.eam.contants.PatrolOrderCommon;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * All rights Reserved, Designed By 翼虎能源
 * Copyright:    Copyright(C) 2015-2017
 * Company   北京翼虎能源科技有限公司
 *
 * @author 李晓阳
 * @version 1.0
 * @date 2017年08月18日
 * @Description 预防性巡检计划生成巡检工单定时任务
 */
@Component
public class EamPatrolPlanTaskTimeJob implements SimpleJob {

    private Logger logger = LoggerFactory.getLogger(EamPatrolPlanTaskTimeJob.class);

    @Resource
    private PatrolPlanClient patrolPlanClient;

    @Autowired
    private PatrolFrequencyClient patrolFrequencyClient;

    @Autowired
    private PatrolOrderClient patrolOrderClient;

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

    /**
     * 巡检计划定时生成巡检工单
     */
    @Override
    public void execute(ShardingContext shardingContext) {
        String taskid = shardingContext.getJobParameter();
        logger.info("--------巡检计划生成巡检工单定时任务--------当前时间:{},id:{}", new Date(), taskid);
        int index = taskid.indexOf("#");
        String planid = taskid.substring(0, index);
        String frequencyId = taskid.substring(index + 1);
        PatrolPlanFrequencyVoForFilter planFrequencyVoForFilter = new PatrolPlanFrequencyVoForFilter();
        planFrequencyVoForFilter.setPatrolPlanId(planid);
        planFrequencyVoForFilter.setId(frequencyId);
        PatrolPlanVo patrolPlanVo = null;
        try {
            patrolPlanVo = patrolPlanClient.findPatrolPlanVoById(planFrequencyVoForFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (patrolPlanVo == null || !PatrolCommon.PATROL_PLAN_STATUS_Y.equals(patrolPlanVo.getStatus())) {
            logger.error("-------无数据或巡检计划状态不是活动-----------id:{},PatrolPlanVo:{}", planid, patrolPlanVo);
            return;
        }
        if (isAutoCreatePatrolPlanOrder(patrolPlanVo)) {
            createPatrolPlanOrder(patrolPlanVo);
        }

    }

    /**
     * isAutoCreatePMOrder:判断当前时间是否生成巡检工单
     *
     * @param patrolPlanVo 巡检计划
     * @return boolean 是否执行
     */
    private boolean isAutoCreatePatrolPlanOrder(PatrolPlanVo patrolPlanVo) {
        boolean flag = false;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        PatrolPlanFrequencyVo patrolPlanFrequencyVo = patrolPlanVo.getPatrolPlanFrequencyVoList().get(0);
        Date nextDate = patrolPlanFrequencyVo.getNextdate();
        // 日期判断
        if (null != nextDate && (DateFormatUtils.format(calendar.getTime(), "yyyy-MM-dd HH").equals(DateFormatUtils.format(nextDate, "yyyy-MM-dd HH")))) {
            flag = true;
        }
        return flag;
    }

    /**
     * createOrder:根据巡检计划创建巡检工单
     *
     * @param patrolPlan 巡检计划
     * @return boolean 是否执行
     */
    private void createPatrolPlanOrder(PatrolPlanVo patrolPlan) {
        Calendar calendar = Calendar.getInstance();
        PatrolPlanFrequencyVo patrolPlanFrequencyVo = patrolPlan.getPatrolPlanFrequencyVoList().get(0);
        if (patrolPlanFrequencyVo.getNextdate() != null) {
            calendar.setTime(patrolPlanFrequencyVo.getNextdate());
        } else {
            logger.error("自动生成工单异常，巡检计划编号为：" + patrolPlan.getPatrolPlanNum());
        }
        //顺延日期，以期下次生成
        if ("天".equals(patrolPlanFrequencyVo.getUnit())) {
            calendar.add(Calendar.DAY_OF_MONTH, Integer.valueOf(patrolPlanFrequencyVo.getFrequency()));
        }
        if ("周".equals(patrolPlanFrequencyVo.getUnit())) {
            calendar.add(Calendar.DAY_OF_MONTH, Integer.valueOf(patrolPlanFrequencyVo.getFrequency()) * 7);
        }
        if ("月".equals(patrolPlanFrequencyVo.getUnit())) {
            calendar.add(Calendar.MONTH, Integer.valueOf(patrolPlanFrequencyVo.getFrequency()));
        }
        if ("年".equals(patrolPlanFrequencyVo.getUnit())) {
            calendar.add(Calendar.YEAR, Integer.valueOf(patrolPlanFrequencyVo.getFrequency()));
        }
        patrolPlanFrequencyVo.setNextdate(calendar.getTime());
        patrolPlanFrequencyVo.setUpdatetime(new Date());
        patrolFrequencyClient.saveOrUpdate(patrolPlanFrequencyVo);

        String siteId = patrolPlan.getSiteId();
        String result = "";
        SiteVoForDetail site = new SiteVoForDetail();
        if (StringUtils.isNotBlank(siteId)) {
            site = siteClient.findById(siteId);
            if (site == null) {
                logger.error("站点编码无效！siteId:{}", siteId);
                return;
            }
            result = codeGeneratorClient.getCodegenerator(site.getOrgId(), siteId, PatrolCommon.PATROL_ORDER_MODEL_KEY);
            if (null == result || "".equals(result)) {
                logger.error("编码生成规则内容读取失败！siteId:{},modelKey:{}", siteId, PatrolCommon.PATROL_ORDER_MODEL_KEY);
                return;
            }
        } else {
            logger.error("站点为空！siteId:{}", siteId);
            return;
        }
        PatrolOrderForSaveVo patrolOrderForSaveVo = new PatrolOrderForSaveVo();
        patrolOrderForSaveVo.setPatrolOrderNum(result);
        patrolOrderForSaveVo.setPatrolPlanId(patrolPlan.getId());
        patrolOrderForSaveVo.setCreatetime(new Date());
        patrolOrderForSaveVo.setUpdatetime(new Date());
        patrolOrderForSaveVo.setStatusdate(new Date());
        patrolOrderForSaveVo.setDescription(patrolPlan.getDescription() + "生成的工单");
        patrolOrderForSaveVo.setType(patrolPlan.getType());
        patrolOrderForSaveVo.setStatus(PatrolOrderCommon.STATUS_DTB);
        patrolOrderForSaveVo.setSiteId(patrolPlan.getSiteId());
        patrolOrderForSaveVo.setOrgId(patrolPlan.getOrgId());
        patrolOrderForSaveVo.setCreatePersonId(Common.SYSTEM_USER);
        PatrolOrderVo patrolOrderVo = patrolOrderClient.saveOrUpdate(patrolOrderForSaveVo);

        if (patrolOrderVo == null || StringUtils.isBlank(patrolOrderVo.getId())) {
            logger.error("保存工单提报失败！, PatrolOrderVo: {}", patrolOrderVo);
        }

        //工单提报
        //启动流程
        //设置流程变量
        Map<String, Object> variables = new HashMap<String, Object>();
        //业务主键
        String businessKey = patrolOrderVo.getId();
        //流程key,key为维保固定前缀+站点code
        String code = site.getCode();
        String processKey = PatrolOrderCommon.WFS_PROCESS_KEY + code;
        ProcessVo processVo = new ProcessVo();
        processVo.setBusinessKey(businessKey);
        processVo.setProcessKey(processKey);
        processVo.setUserId(Common.SYSTEM_USER);
        variables.put(PatrolOrderCommon.SUBMIT_USER, Common.SYSTEM_USER);
        variables.put("userId", Common.SYSTEM_USER);
        variables.put(PatrolOrderCommon.ORDER_NUM, patrolOrderVo.getPatrolOrderNum());
        variables.put(PatrolOrderCommon.ORDER_DESCRIPTION, patrolOrderVo.getDescription());
        logger.debug("/eam/open/patrolOrder/commit, processKey: {}", processKey);
        processVo = workflowClient.startProcess(variables, processVo);

        if (null == processVo || "".equals(processVo.getProcessInstanceId())) {
            logger.error("流程启动失败");
            return;
        }
        //提报，修改基本字段保存
        patrolOrderVo.setProcessInstanceId(processVo.getProcessInstanceId());
        PatrolOrderForWorkFlowVo patrolOrder = new PatrolOrderForWorkFlowVo();
        BeanUtils.copyProperties(patrolOrderVo, patrolOrder);
        patrolOrder = patrolOrderClient.savePatrolOrderFlow(patrolOrder);
        //查询分派组签收人员
        variables = new HashMap<String, Object>();
        List<String> userList = new ArrayList<>();
        UserGroupDomainVo vo = userGroupDomainClient.
                findUserGroupDomainByDomainValueAndDomainNum(patrolOrder.getType(),
                        PatrolOrderCommon.PATROL_TYPE, patrolOrder.getOrgId(), siteId, Common.USERGROUP_ASSOCIATION_TYPE_ALL);
        UgroupVoForDetail voForDetail = ugroupClient.findById(vo.getUserGroupId());
        voForDetail.getUsers().stream().forEach(u -> {
                    PersonAndUserVoForDetail person = personAndUserClient.findByPersonId(u.getPersonId());
                    if (person != null) {//清空无关数据
                        userList.add(person.getPersonId());
                    }
                }
        );
        if (null == userList || userList.size() <= 0) {
            logger.error("巡检工单分派组下没有人员,请联系管理员添加!!userGroup{}", voForDetail.getCode());
        }
        variables.put(PatrolOrderCommon.ASSIGN_USER, org.apache.commons.lang3.StringUtils.join(userList, ","));
        variables.put("description", patrolOrder.getDescription());
        variables.put("status", PatrolOrderCommon.STATUS_DFP);
        variables.put(PatrolOrderCommon.ACTIVITY_REJECT_ASSIGN_PASS, true);
        PersonAndUserVoForDetail createPerson = personAndUserClient.findByLoginName(Common.SYSTEM_USER_LOGINNAME);
        if(createPerson!=null)variables.put("userId", createPerson.getPersonId());
        Boolean processMessage = processTaskClient.completeByProcessInstanceId(patrolOrder.getProcessInstanceId(), variables);
        if (Objects.isNull(processMessage) || !processMessage) {
            throw new EnerbosException("500", "流程操作异常。");
        }
        //提报，修改基本字段保存
        patrolOrder.setStatus(PatrolOrderCommon.STATUS_DFP);
        patrolOrder.setStatusdate(new Date());//状态日期
        patrolOrderClient.savePatrolOrderFlow(patrolOrder);
        logger.info("--------巡检计划定时生成巡检工单结束--------当前时间:{},PatrolPlanVo:{},patrolOrderVo:{}", new Date(), patrolPlan, patrolOrderVo);
    }
}
