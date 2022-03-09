package com.springboot.act;

import org.activiti.bpmn.model.FormProperty;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricActivityInstanceQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
class ActApplicationTests {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;



    @Test
    void contextLoads() {
    }

    //0.流程部署，单个文件部署方式
    @Test
    public void testDeployment() {
        //使用RepositoryService进行部署
        DeploymentBuilder builder = repositoryService.createDeployment();

        builder.addClasspathResource("process/leave-process.bpmn20.xml");
//        builder.addClasspathResource("process/leave-process.png");
        builder.name("leave-process-name");
        Deployment deploy = builder.deploy();

        //输出部署信息
        System.out.println("流程部署id: " + deploy.getId());//2d8f1a3b-78fb-11ec-9ea0-3c7c3f7e27d2
        System.out.println("流程部署名称： " + deploy.getName());
        //部署信息存在表 ：act_re_deployment、 act_re_procdef、 act_ge_bytearray
    }

    //1.流程实例启动
    @Test
    public void testStartProcess() {
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery().deploymentId("2d8f1a3b-78fb-11ec-9ea0-3c7c3f7e27d2");
        ProcessDefinition processDefinition = processDefinitionQuery.singleResult();
        //根据流程定义id启动流程
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());

        //输出实例信息
        System.out.println("流程定义id: " + processInstance.getProcessDefinitionId());
        System.out.println("流程实例id: " + processInstance.getId());
        System.out.println("当前活动id: " + processInstance.getActivityId());

        //流程定义id: myProcess_1:2:2d9e838e-78fb-11ec-9ea0-3c7c3f7e27d2
        //流程实例id: bcc2d346-7903-11ec-961f-3c7c3f7e27d2
        //当前活动id: null

        //流程实例启动，将操作以下几个数据库表

        //act_hi_actinst 流程实例执行历史
        //act_hi_identitylink 流程的参与用户历史信息
        //act_hi_procinst 流程实例历史信息
        //act_hi_taskinst 流程任务历史信息
        //act_ru_execution 流程执行信息
        //act_ru_identitylink 流程的参与用户信息
        //act_ru_task 任务信息

    }

    //2.任务查询
    @Test
    public void testFindPersonalTaskList() {
        //任务负责人
        String assignee = "liuky";

        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery().deploymentId("2d8f1a3b-78fb-11ec-9ea0-3c7c3f7e27d2");
        ProcessDefinition processDefinition = processDefinitionQuery.singleResult();
        System.out.println("id: " + processDefinition.getId());

        //根据流程key 和任务负责人 查询任务
        List<Task> list = taskService.createTaskQuery().processDefinitionKey("myProcess_1").taskAssignee(assignee).list();

        for (Task task : list) {
            System.out.println("流程实例id: " + task.getProcessInstanceId());
            System.out.println("任务id: " + task.getId());
            System.out.println("任务负责人： " + task.getAssignee());
            System.out.println("任务名称： " + task.getName());

            //流程实例id: bcc2d346-7903-11ec-961f-3c7c3f7e27d2
            //任务id: bcc67cca-7903-11ec-961f-3c7c3f7e27d2
            //任务负责人： liuky
            //任务名称： 主管
        }
    }

    //完成任务
    @Test
    public void completeTask() {
        //根据流程key 和任务的负责人查询任务 并 选择其中的一个任务处理，
        // 这里用的是singleResult返回一条，真实环境是查询出所有的任务，然后在页面上选择一个任务进行处理
        List<Task> list = taskService.createTaskQuery().processDefinitionKey("myProcess_1").taskAssignee("hefy").list();
        Task task = taskService.createTaskQuery().processDefinitionKey("myProcess_1").taskAssignee("hefy").singleResult();
        //完成任务，参数：任务id
        taskService.complete(task.getId());
    }

    //流程结束，或流程流转过程中的历史信息查询
    @Test
    public void findHistoryInfo() {
        //获取 actinst 表的查询对象
        HistoricActivityInstanceQuery instanceQuery = historyService.createHistoricActivityInstanceQuery();

        // 查询 表 actinst , 条件：根据 InstanceId 查询
        instanceQuery.processInstanceId("bcc2d346-7903-11ec-961f-3c7c3f7e27d2");
        //增加排序操作，orderByHistoricActivityInstanceStartTime 根据开始时间排序 asc 升序
        instanceQuery.orderByHistoricActivityInstanceStartTime().asc();
        //查询所有内容
        List<HistoricActivityInstance> list = instanceQuery.list();

        //输出结果
        for (HistoricActivityInstance instance : list) {
            System.out.println("");
            System.out.println("===================-===============");
            System.out.println(instance.getStartTime());
            System.out.println(instance.getAssignee());
            System.out.println(instance.getActivityId());
            System.out.println(instance.getActivityName());
            System.out.println(instance.getProcessDefinitionId());
            System.out.println(instance.getProcessInstanceId());
            System.out.println("===================-===============");
            System.out.println("");

        }


    }

    //查询流程相关信息，包含流程定义，流程部署，流程定义版本
    @Test
    public void queryProcessDefinition() {
        //得到 ProcessDefinitionQuery 对象
        ProcessDefinitionQuery definitionQuery = repositoryService.createProcessDefinitionQuery();

        //查询出当前所有的流程定义
        List<ProcessDefinition> definitions = definitionQuery.processDefinitionKey("myProcess_1").orderByProcessDefinitionVersion().desc().list();
        //打印结果
        for (ProcessDefinition processDefinition : definitions) {
            System.out.println("流程定义 id="+processDefinition.getId());
            System.out.println("流程定义 name="+processDefinition.getName());
            System.out.println("流程定义 key="+processDefinition.getKey());
            System.out.println("流程定义 Version="+processDefinition.getVersion());
            System.out.println("流程部署ID ="+processDefinition.getDeploymentId());
            System.out.println("===================-===============");
        }

    }



    //删除流程
    @Test
    public void deleteDeployment(){

        String deploymentId = "29f592c7-87db-11ec-b22d-3c7c3f7e27d2";

        //删除流程定义，如果该流程定义已有流程实例启动则删除时出错
//        repositoryService.deleteDeployment(deploymentId);

        //设置true 级联删除流程定义，即使该流程有流程实例启动也可以删除，设置为false非级别删除方式，如果流程
        repositoryService.deleteDeployment(deploymentId, true);

    }


}
