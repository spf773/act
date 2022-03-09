package com.springboot.act.util;

import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.springframework.util.StreamUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作流工具类
 */
@Slf4j
public class ActivitiUtil {

    /**
     * 获取走过的流程线
     * @param bpmnModel 流程对象模型
     * @param processDefinitionEntity 流程定义对象
     * @param historicActivityInstances 历史流程已经执行的节点，并已经按执行的先后顺序排序
     * @return List<String> 流程走过的线
     */
    public static List<String> getHighlightedFlows(BpmnModel bpmnModel, ProcessDefinitionEntity processDefinitionEntity,
                                                   List<HistoricActivityInstance> historicActivityInstances) {
        //用以保存高亮的线flowId
        List<String> highFlows = new ArrayList<>();
        if(historicActivityInstances == null || historicActivityInstances.size()  == 0) {
            return highFlows;
        }

        //遍历历史节点
        for (int i = 0; i < historicActivityInstances.size() - 1; i++) {
            //取出已执行的节点
            HistoricActivityInstance activityImpl_  = historicActivityInstances.get(i);

            //用以保存后续开始时间相同的节点
            List<FlowNode> sameStartTimeNodes = new ArrayList<>();

            //获取下一个节点（用于连线）
            FlowNode sameActivityImpl = getNextFlowNode(bpmnModel, historicActivityInstances, i, activityImpl_);
//            FlowNode sameActivityImpl = (FlowNode) bpmnModel.getMainProcess().getFlowElement(historicActivityInstances.get(i + 1).getActivityId());

            //将后面第一个节点放在时间相同的节点的集合里
            if(sameActivityImpl != null) {
                sameStartTimeNodes.add(sameActivityImpl);
            }

            //循环后面节点，看是否有与此后继节点开始时间相同的节点，有则添加到后继节点集合
            for (int j = 0; j < historicActivityInstances.size() - 1; j++) {
                HistoricActivityInstance activityImpl1  = historicActivityInstances.get(j);
                HistoricActivityInstance activityImpl2  = historicActivityInstances.get(j + 1);

                if(activityImpl1.getStartTime().getTime() != activityImpl2.getStartTime().getTime()) break;

                //如果第一个节点和第二个节点开始时间相同保存
                FlowNode sameActivityImpl2 = (FlowNode) bpmnModel.getMainProcess().getFlowElement(activityImpl2.getActivityId());
                sameStartTimeNodes.add(sameActivityImpl2);
            }

            //得到节点定义的详细信息
            FlowNode activityImpl = (FlowNode) bpmnModel.getMainProcess().getFlowElement(historicActivityInstances.get(i).getActivityId());
            //取出节点的所有出去的线，对所有节点的线进行遍历
            List<SequenceFlow> outgoingFlows = activityImpl.getOutgoingFlows();
            for (SequenceFlow outgoingFlow : outgoingFlows) {
                //获取节点
                FlowNode flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(outgoingFlow.getTargetRef());

                //不是后继节点
                if(!sameStartTimeNodes.contains(flowNode)) continue;

                //如果取出的线的目标节点在时间相同的节点里，保存该线的id,进行高亮显示
                highFlows.add(outgoingFlow.getId());
            }
        }
        //返回高亮的线
        return highFlows;
    }

    /**
     * 获取下一个节点信息
     * @param bpmnModel 流程模型
     * @param historicActivityInstances 历史节点
     * @param i 当前已经遍历到的历史节点索引（找下一个节点从此节点开始）
     * @param activityImpl_ 当前遍历到的历史节点实例
     * @return FlowNode 下一个节点的信息
     */
    public static FlowNode getNextFlowNode(BpmnModel bpmnModel, List<HistoricActivityInstance> historicActivityInstances,
                                           int i, HistoricActivityInstance activityImpl_) {
        //保存后一个节点
        FlowNode sameActivityImpl = null;

        //如果当前任务节点不是用户任务节点，则取排序的下一个节点为后续节点
        if(!"userTask".equals(activityImpl_.getActivityType())) {
            // 是最后一个节点，没有下一个节点
            if(i == historicActivityInstances.size()) {
                return sameActivityImpl;
            }
            //不是最后一个节点，取下一个节点为后继节点
            sameActivityImpl = (FlowNode) bpmnModel.getMainProcess().getFlowElement(historicActivityInstances.get(i + 1).getActivityId());
            // 返回
            return sameActivityImpl;
        }

        //遍历后续节点，获取当前节点后续节点
        for (int j = 0; j < historicActivityInstances.size() - 1; j++) {
            //后续节点
            HistoricActivityInstance activityImp2_ = historicActivityInstances.get(j);

            //都是userTask, 且主节点与后续节点的开始时间相同，说明不是真实的后继节点
            if("userTask".equals(activityImp2_.getActivityType())
                    && activityImpl_.getStartTime().getTime() == activityImp2_.getStartTime().getTime()) {
                continue;
            }
            //找到紧跟在后面的一个节点
            sameActivityImpl = (FlowNode) bpmnModel.getMainProcess().getFlowElement(activityImp2_.getActivityId());
            break;
        }
        return sameActivityImpl;

    }


    /**
     * 输出图像
     * @param response 响应实体
     * @param bpmnModel 图形对象
     * @param flowIds 已执行的线集合
     * @param executedActivityIdList 已执行的节点Id集合
     */
    public static void outputImg(HttpServletResponse response, BpmnModel bpmnModel, List<String> flowIds,
                          List<String> executedActivityIdList) {
        InputStream inputStream = null;
        try {
            ProcessDiagramGenerator processDiagramGenerator = new DefaultProcessDiagramGenerator();
            inputStream = processDiagramGenerator.generateDiagram(bpmnModel, executedActivityIdList, flowIds, "宋体", "微软雅黑", "黑体", true, "png");
            //输出资源内容到相应的对象
            byte[] b = new byte[1024];
            int len;
            while ((len = inputStream.read(b, 0, 1024)) != -1) {
                response.getOutputStream().write(b, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("流程图输入异常！", e);
        }finally {
            if(inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
