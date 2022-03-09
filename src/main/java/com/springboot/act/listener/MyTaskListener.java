package com.springboot.act.listener;

import lombok.extern.slf4j.Slf4j;
import org.activiti.api.process.runtime.events.ProcessStartedEvent;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.TaskListener;

@Slf4j
public class MyTaskListener implements TaskListener, ExecutionListener {


    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        log.info("{} TaskListener 进入监听：{}", eventName, delegateTask.toString());

    }

    @Override
    public void notify(DelegateExecution delegateExecution) {
        String eventName = delegateExecution.getEventName();
        log.info("{} ExecutionListener 进入监听：{}", eventName, delegateExecution.toString());
    }
}
