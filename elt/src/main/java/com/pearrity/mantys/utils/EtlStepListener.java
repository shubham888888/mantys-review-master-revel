package com.pearrity.mantys.utils;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;

import java.util.Date;
import java.util.logging.Logger;

import static com.pearrity.mantys.utils.UtilFunctions.*;

public class EtlStepListener implements StepExecutionListener {

  private final Logger log = Logger.getLogger(EtlStepListener.class.getName());
  private final String tenant;
  private final String entity;
  private final String platform;
  private final String org;
  private final Date from;

  public EtlStepListener(String entity, String tenant, String platform, Date from, String org) {
    this.tenant = tenant;
    this.entity = entity;
    this.platform = platform;
    this.from = from;
    this.org = org;
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {
    ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
    executionContext.putString("stepName", stepExecution.getStepName());
    executionContext.putString("entity", entity);
    log.info("before step : " + stepExecution.getStepName());
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
    executionContext.remove("stepName");
    executionContext.remove("entity");
    log.info("after step : " + stepExecution.getStepName());
    log.info("status of step : " + stepExecution.getExitStatus());
    updateEtlMileStone(stepExecution, tenant, platform, entity, from, org);
    stepExecution.setExitStatus(ExitStatus.COMPLETED);
    return ExitStatus.COMPLETED;
  }
}
