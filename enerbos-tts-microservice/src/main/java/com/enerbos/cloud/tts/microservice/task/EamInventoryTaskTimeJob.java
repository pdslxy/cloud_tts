package com.enerbos.cloud.tts.microservice.task;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.enerbos.cloud.eam.client.CodeGeneratorClient;
import com.enerbos.cloud.eam.client.MaterialGoodsReceiveClient;
import com.enerbos.cloud.eam.client.MaterialInventoryClient;
import com.enerbos.cloud.eam.vo.MaterialGoodsReceiveDetailVo;
import com.enerbos.cloud.eam.vo.MaterialGoodsReceiveVo;
import com.enerbos.cloud.eam.vo.MaterialInventoryVo;
import com.enerbos.cloud.uas.client.SiteClient;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * All rights Reserved, Designed By 翼虎能源
 * Copyright:    Copyright(C) 2015-2017
 * Company   北京翼虎能源科技有限公司
 *
 * @author liuxiupeng
 * @version 1.0
 * @date 2017年10月18日 19:53:25
 * @Description 重订购
 */
@Component
public class EamInventoryTaskTimeJob implements SimpleJob {

    private Logger logger = LoggerFactory.getLogger(EamInventoryTaskTimeJob.class);

    @Resource
    private MaterialInventoryClient materialInventoryClient;

//    @Autowired
//    private PatrolFrequencyClient patrolFrequencyClient;

    @Autowired
    private MaterialGoodsReceiveClient materialGoodsReceiveClient;

    @Autowired
    private CodeGeneratorClient codeGeneratorClient;

    @Autowired
    private SiteClient siteClient;

//    @Resource
//    private WorkflowClient workflowClient;

//    @Autowired
//    private ProcessTaskClient processTaskClient;

//    @Autowired
//    private UgroupClient ugroupClient;

//    @Autowired
//    private UserGroupDomainClient userGroupDomainClient;

//    @Autowired
//    private PersonAndUserClient personAndUserClient;

    @Override
    public void execute(ShardingContext shardingContext) {

        String id = shardingContext.getJobParameter();
        logger.info("--------定时任务--------当前时间:{},id:{}", new Date(), id);
        MaterialInventoryVo materialInventoryVo = materialInventoryClient.findInventoryDetail(id);
        if (materialInventoryVo == null) {
            logger.error("-------无数据-----------id:{},materialInventoryVo:{}", id, materialInventoryVo);
            return;
        }
        if (isAutoCreateGoodReceive(materialInventoryVo)) {
            createGoodReceive(materialInventoryVo);
        }

    }

    private void createGoodReceive(MaterialInventoryVo materialInventoryVo) {
        MaterialGoodsReceiveVo materialGoodsReceiveVo = new MaterialGoodsReceiveVo();

        String code = codeGeneratorClient.getCodegenerator(materialInventoryVo.getOrgId(),materialInventoryVo.getSiteId(), "item");
        if (StringUtils.isNotEmpty(code)) {
            materialGoodsReceiveVo.setGoodsReceiveNum(code);
            materialGoodsReceiveVo.setCreateUser(materialInventoryVo.getCreateUser());
            materialGoodsReceiveVo.setChangeUser(materialInventoryVo.getCreateUser());
            materialGoodsReceiveVo.setDescription("重订购自动生成接收单");
            materialGoodsReceiveVo.setStoreroomId(materialInventoryVo.getStoreroomId());
            materialGoodsReceiveVo.setSiteId(materialInventoryVo.getSiteId());
            materialGoodsReceiveVo.setOrgId(materialInventoryVo.getOrgId());
            materialGoodsReceiveVo.setReceiveDate(new Date());
            materialGoodsReceiveVo.setReceivePerson(materialInventoryVo.getCreateUser());
            materialGoodsReceiveVo.setReceiveType("CGJS");
            materialGoodsReceiveVo.setStatus("CG");

            MaterialGoodsReceiveVo materialGoodsReceiveVoRsult = materialGoodsReceiveClient.saveGoodsReceive(materialGoodsReceiveVo);

            MaterialGoodsReceiveDetailVo materialGoodsReceiveDetailVo = new MaterialGoodsReceiveDetailVo();
            materialGoodsReceiveDetailVo.setCreateUser(materialGoodsReceiveVoRsult.getCreateUser());
            materialGoodsReceiveDetailVo.setGoodsReceiveId(materialGoodsReceiveVoRsult.getId());
            materialGoodsReceiveDetailVo.setSiteId(materialGoodsReceiveVoRsult.getSiteId());
            materialGoodsReceiveDetailVo.setOrgId(materialGoodsReceiveVoRsult.getOrgId());

            materialGoodsReceiveClient.saveGoodsReceiveDetail(materialGoodsReceiveDetailVo);


        } else {

            logger.error("-------读取自动编码失败-----------");
        }
    }

    private boolean isAutoCreateGoodReceive(MaterialInventoryVo materialInventoryVo) {

        Boolean reorder = materialInventoryVo.getReorder();
        if (reorder != null && reorder.booleanValue()) {

            long currentBalance = materialInventoryVo.getCurrentBalance();
            long reorderPoint = Long.valueOf(materialInventoryVo.getReorderPoint());
            if (currentBalance < reorderPoint) {
                return true;
            }
        }

        return false;
    }


    /**
     * createOrder:根据巡检计划创建巡检工单
     *
     * @param patrolPlan 巡检计划
     * @return boolean 是否执行
     */
//    private void createPatrolPlanOrder(PatrolPlanVo patrolPlan) {
//        Calendar calendar = Calendar.getInstance();
//        PatrolPlanFrequencyVo patrolPlanFrequencyVo = patrolPlan.getPatrolPlanFrequencyVoList().get(0);
//        if (patrolPlanFrequencyVo.getNextdate() != null) {
//            calendar.setTime(patrolPlanFrequencyVo.getNextdate());
//        } else {
//            logger.error("自动生成工单异常，巡检计划编号为：" + patrolPlan.getPatrolPlanNum());
//        }
//        //顺延日期，以期下次生成
//        if ("天".equals(patrolPlanFrequencyVo.getUnit())) {
//            calendar.add(Calendar.DAY_OF_MONTH, Integer.valueOf(patrolPlanFrequencyVo.getFrequency()));
//        }
//        if ("周".equals(patrolPlanFrequencyVo.getUnit())) {
//            calendar.add(Calendar.MONTH, Integer.valueOf(patrolPlanFrequencyVo.getFrequency()));
//        }
//        if ("月".equals(patrolPlanFrequencyVo.getUnit())) {
//            calendar.add(Calendar.DAY_OF_MONTH, Integer.valueOf(patrolPlanFrequencyVo.getFrequency()) * 7);
//        }
//        if ("年".equals(patrolPlanFrequencyVo.getUnit())) {
//            calendar.add(Calendar.YEAR, Integer.valueOf(patrolPlanFrequencyVo.getFrequency()));
//        }
//        patrolPlanFrequencyVo.setNextdate(calendar.getTime());
//        patrolPlanFrequencyVo.setUpdatetime(new Date());
//        patrolFrequencyClient.saveOrUpdate(patrolPlanFrequencyVo);
//
//        String siteId = patrolPlan.getSiteId();
//        String result = "";
//        SiteVoForDetail site = new SiteVoForDetail();
//        if (StringUtils.isNotBlank(siteId)) {
//            site = siteClient.findById(siteId);
//            if (site == null) {
//                logger.error("站点编码无效！siteId:{}", siteId);
//                return;
//            }
//            result = codeGeneratorClient.getCodegenerator(siteId, PatrolCommon.PATROL_ORDER_MODEL_KEY);
//            if (null == result || "".equals(result)) {
//                logger.error("编码生成规则内容读取失败！siteId:{},modelKey:{}", siteId, PatrolCommon.PATROL_ORDER_MODEL_KEY);
//                return;
//            }
//        } else {
//            logger.error("站点为空！siteId:{}", siteId);
//            return;
//        }
//        PatrolOrderForSaveVo patrolOrderForSaveVo = new PatrolOrderForSaveVo();
//        patrolOrderForSaveVo.setPatrolOrderNum(result);
//        patrolOrderForSaveVo.setPatrolPlanId(patrolPlan.getId());
//        patrolOrderForSaveVo.setCreatetime(new Date());
//        patrolOrderForSaveVo.setUpdatetime(new Date());
//        patrolOrderForSaveVo.setStatusdate(new Date());
//        patrolOrderForSaveVo.setDescription(patrolPlan.getDescription() + "生成的工单");
//        patrolOrderForSaveVo.setType(patrolPlan.getType());
//        patrolOrderForSaveVo.setStatus(PatrolOrderCommon.STATUS_DTB);
//        patrolOrderForSaveVo.setSiteId(patrolPlan.getSiteId());
//        patrolOrderForSaveVo.setOrgId(patrolPlan.getOrgId());
//        patrolOrderForSaveVo.setCreatePersonId(Common.SYSTEM_USER);
//        PatrolOrderVo patrolOrderVo = patrolOrderClient.saveOrUpdate(patrolOrderForSaveVo);
//
//        if (patrolOrderVo == null || StringUtils.isBlank(patrolOrderVo.getId())) {
//            logger.error("保存工单提报失败！, PatrolOrderVo: {}", patrolOrderVo);
//        }
//
//        //工单提报
//        //启动流程
//        //设置流程变量
//        Map<String, Object> variables = new HashMap<String, Object>();
//        //业务主键
//        String businessKey = patrolOrderVo.getId();
//        //流程key,key为维保固定前缀+站点code
//        String code = site.getCode();
//        String processKey = PatrolOrderCommon.WFS_PROCESS_KEY + code;
//        ProcessVo processVo = new ProcessVo();
//        processVo.setBusinessKey(businessKey);
//        processVo.setProcessKey(processKey);
//        processVo.setUserId(Common.SYSTEM_USER);
//        variables.put(PatrolOrderCommon.SUBMIT_USER, Common.SYSTEM_USER);
//        variables.put("userId", Common.SYSTEM_USER);
//        variables.put(PatrolOrderCommon.ORDER_NUM, patrolOrderVo.getPatrolOrderNum());
//        variables.put(PatrolOrderCommon.ORDER_DESCRIPTION, patrolOrderVo.getDescription());
//        logger.debug("/eam/open/patrolOrder/commit, processKey: {}", processKey);
//        processVo = workflowClient.startProcess(variables, processVo);
//
//        if (null == processVo || "".equals(processVo.getProcessInstanceId())) {
//            logger.error("流程启动失败");
//            return;
//        }
//        //提报，修改基本字段保存
//        patrolOrderVo.setProcessInstanceId(processVo.getProcessInstanceId());
//        PatrolOrderForWorkFlowVo patrolOrder = new PatrolOrderForWorkFlowVo();
//        BeanUtils.copyProperties(patrolOrderVo, patrolOrder);
//        patrolOrder = patrolOrderClient.savePatrolOrderFlow(patrolOrder);
//        //查询分派组签收人员
//        variables = new HashMap<String, Object>();
//        List<String> userList = new ArrayList<>();
//        UserGroupDomainVo vo = userGroupDomainClient.
//                findUserGroupDomainByDomainValueAndDomainNum(patrolOrder.getType(),
//                        PatrolOrderCommon.PATROL_TYPE, patrolOrder.getOrgId(), siteId, Common.USERGROUP_ASSOCIATION_TYPE_ALL);
//        UgroupVoForDetail voForDetail = ugroupClient.findById(vo.getUserGroupId());
//        voForDetail.getUsers().stream().forEach(u -> {
//                    PersonAndUserVoForDetail person = personAndUserClient.findByPersonId(u.getPersonId());
//                    if (person != null) {//清空无关数据
//                        userList.add(person.getPersonId());
//                    }
//                }
//        );
//        if (null == userList || userList.size() <= 0) {
//            logger.error("巡检工单分派组下没有人员,请联系管理员添加!!userGroup{}", voForDetail.getCode());
//        }
//        variables.put(PatrolOrderCommon.ASSIGN_USER, org.apache.commons.lang3.StringUtils.join(userList, ","));
//        variables.put("description", patrolOrder.getDescription());
//        variables.put("status", PatrolOrderCommon.STATUS_DFP);
//        variables.put(PatrolOrderCommon.ACTIVITY_REJECT_ASSIGN_PASS, true);
//        variables.put("userId", Common.SYSTEM_USER);
//        Boolean processMessage = processTaskClient.completeByProcessInstanceId(patrolOrder.getProcessInstanceId(), variables);
//        if (Objects.isNull(processMessage) || !processMessage) {
//            throw new EnerbosException("500", "流程操作异常。");
//        }
//        //提报，修改基本字段保存
//        patrolOrder.setStatus(PatrolOrderCommon.STATUS_DFP);
//        patrolOrder.setStatusdate(new Date());//状态日期
//        patrolOrderClient.savePatrolOrderFlow(patrolOrder);
//        logger.info("--------巡检计划定时生成巡检工单结束--------当前时间:{},PatrolPlanVo:{},patrolOrderVo:{}", new Date(), patrolPlan, patrolOrderVo);
//    }
}
