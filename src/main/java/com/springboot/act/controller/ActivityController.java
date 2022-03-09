package com.springboot.act.controller;

import com.springboot.act.util.ActivitiUtil;
import lombok.extern.slf4j.Slf4j;


import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FormProperty;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.*;
import org.activiti.engine.history.*;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/activity")
public class ActivityController {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    TaskRuntime taskRuntime;




    @ResponseBody
    @RequestMapping("/import")
    public void importBpmn(@RequestParam MultipartFile file) {
        DeploymentBuilder deployment = repositoryService.createDeployment();
        String originalFilename = file.getOriginalFilename();
        try {
            deployment.name(originalFilename.split("\\.")[0]);
            Deployment deploy = deployment.addInputStream(originalFilename, file.getInputStream()).deploy();
            log.info("部署ID: " + deploy.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>启动请假流程（流程key即xml中定义的ID）</p>
     *  在业务中相当于保存表单
     * @return String 启动的流程ID
     */
    @ResponseBody
    @PostMapping("/start")
    public String start(@RequestBody Map<String, Object> param) {
        String processDefinitionKey = param.get("processDefinitionKey").toString();
        log.info("开启请假流程...");

        //设置流程参数，开启流程
        Map<String, Object> map = new HashMap<>();
        map.put("user", param.get("user"));
        map.put("variables", param.get("variables"));
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(processDefinitionKey, map);

        log.info("启动流程实例成功:{}", instance);
        log.info("流程实例ID:{}", instance.getId());
        log.info("流程定义ID:{}", instance.getProcessDefinitionId());

        //验证是否启动成功
        //通过查询正在运行的流程实例来判断
        ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery();
        //根据流程实例ID来查询
        List<ProcessInstance> list = processInstanceQuery.processInstanceId(instance.getId()).list();
        log.info("根据流程ID查询条数：{}", list.size());

        //返回流程id
        return instance.getId();

    }

    /**
     * 查询待审批任务
     * @param assignee
     * @return
     */
    @ResponseBody
    @GetMapping("/queryTaskList")
    public void queryTaskList(String assignee) {
        List<Task> list = taskService.createTaskQuery().taskAssignee(assignee).list();
        for (Task task : list) {
            System.out.println("任务ID:"+task.getId());
            System.out.println("任务名称:"+task.getName());
            System.out.println("任务的创建时间:"+task.getCreateTime());
            System.out.println("任务的办理人:"+task.getAssignee());
            System.out.println("流程实例ID："+task.getProcessInstanceId());
            System.out.println("执行对象ID:"+task.getExecutionId());
            System.out.println("流程定义ID:"+task.getProcessDefinitionId());


        }

    }

    /**
     * 完成流程
     */
    @ResponseBody
    @PostMapping("/completeTask")
    public void completeTask(@RequestBody Map<String, Object> param) {
        String assignee = param.get("assignee").toString();
        String taskId = param.get("taskId").toString();

        log.info("{} 开始审批，任务id: {}", assignee, taskId);
        //给变量重新赋值
        taskService.setVariable(taskId, "variables", param.get("variables"));

        Object audit = param.get("audit");
        if(audit != null) {
            taskService.setVariable(taskId, "audit", param.get("audit"));
        }
        taskService.complete(taskId);
        log.info("{} 完成了自己的审批，任务id: {}", assignee, taskId);
    }

    @ResponseBody
    @GetMapping("/completeTask2")
    public void completeTask2(String user,String taskId,int audit){
        System.out.println(user+"：提交自己的流程："+taskId+" ;是否通过："+audit);
        //任务ID
        HashMap<String, Object> variables=new HashMap<>();
        variables.put("audit", audit);//userKey在上文的流程变量中指定了
        taskService.complete(taskId,variables);

        System.out.println("完成任务：任务ID："+taskId);
        System.out.println("==================================================================");
    }

    @ResponseBody
    @GetMapping("/historyTask")
    public void historyTask(String instanceId) {
        List<HistoricTaskInstance> list=historyService // 历史相关Service
                .createHistoricTaskInstanceQuery() // 创建历史活动实例查询
                .processInstanceId(instanceId) // 执行流程实例id
                .orderByTaskCreateTime()
                .asc()
                .list();


        for(HistoricTaskInstance hai:list){
            System.out.println("活动ID:"+hai.getId());
            System.out.println("流程实例ID:"+hai.getProcessInstanceId());
            System.out.println("活动名称："+hai.getName());
            System.out.println("办理人："+hai.getAssignee());
            System.out.println("开始时间："+hai.getStartTime());
            System.out.println("结束时间："+hai.getEndTime());
            System.out.println(hai.getProcessVariables());
            System.out.println("==================================================================");
        }
    }

    @ResponseBody
    @GetMapping("/formDataShow")
    public Map<String, Object> formDataShow(String instanceId) {



        Task task = taskService.createTaskQuery().processInstanceId("a8dd998f-87e8-11ec-a612-3c7c3f7e27d2").singleResult();
        //获取task对应的表单内容
        UserTask userTask = (UserTask)repositoryService.getBpmnModel(task.getProcessDefinitionId())
                .getFlowElement(task.getTaskDefinitionKey());
        List<FormProperty> formProperties = userTask.getFormProperties();
        System.out.println(formProperties);
        return null;
    }

    /**
     * <p>查看当前流程图</p>
     * @param instanceId 流程实例
     * @param response 响应
     */
    @ResponseBody
    @RequestMapping("/showImg")
    public void showImg(String instanceId, HttpServletResponse response) {
        /**
         * 参数校验
         */
        log.info("查看完整流程图！流程实例ID:{}", instanceId);
        if(StringUtils.isBlank(instanceId)) return;

        /**
         * 获取流程实例
         */
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                                                                        .processInstanceId(instanceId)
                                                                        .singleResult();
        if(processInstance == null) {
            log.error("流程实例ID:{} 没查到流程实例！", instanceId);
            return;
        }

        //根据流程对象获取流程对象模型
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());

        /**
         * 查看已执行的节点集合
         * 获取流程历史中已执行的节点，并按照节点在流程中执行先后顺序排序
         */
        //构造历史流程查询
        HistoricActivityInstanceQuery instanceQuery = historyService.createHistoricActivityInstanceQuery().processInstanceId(instanceId);
        //查询历史节点
        List<HistoricActivityInstance> historyInstanceList = instanceQuery.orderByHistoricActivityInstanceStartTime().asc().list();
        if(historyInstanceList == null || historyInstanceList.size() == 0) {
            log.info("流程实例ID: {} 没有历史节点信息！", instanceId);
            ActivitiUtil.outputImg(response, bpmnModel, null, null);
            return;
        }
        //已执行的节点ID集合（将 historyInstanceList 中元素的 activityId 字段取出封装到 executedActivityIdList）
        List<String> executedActivityIdList = historyInstanceList.stream().map(item -> item.getActivityId()).collect(Collectors.toList());

        /**
         * 获取流程走过的线
         */
        //获取流程定义
        ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
                                                    .getDeployedProcessDefinition(processInstance.getProcessDefinitionId());
        List<String> flowIds = ActivitiUtil.getHighlightedFlows(bpmnModel, processDefinition, historyInstanceList);

        /*
         * 输出图像，并设置高亮
         */
        ActivitiUtil.outputImg(response, bpmnModel, flowIds, executedActivityIdList);
    }


}
