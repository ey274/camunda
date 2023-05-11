package com.camunda.training.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;

@Component
public class Workers {
  
  
  private static final Logger LOG = LoggerFactory.getLogger(Workers.class);


  @JobWorker
  public void logging(ActivatedJob job) {
    LOG.info("Works for process instance {}", job.getProcessInstanceKey());
  }
}
