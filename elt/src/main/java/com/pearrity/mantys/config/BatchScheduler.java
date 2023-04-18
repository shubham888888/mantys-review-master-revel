package com.pearrity.mantys.config;

import com.google.gson.Gson;
import com.pearrity.mantys.chargebee.ChargeBeeJobConfig;
import com.pearrity.mantys.chargebee.ChargeBeeResourceUtil;
import com.pearrity.mantys.domain.OAuthSecret;
import com.pearrity.mantys.domain.utils.Constants;
import com.pearrity.mantys.hubspot.HubSpotJobConfig;
import com.pearrity.mantys.hubspot.HubSpotResourceUtil;
import com.pearrity.mantys.quickBooks.QuickBooksJobConfig;
import com.pearrity.mantys.quickBooks.QuickBooksResourceUtil;
import com.pearrity.mantys.repository.config.AwsSecretsService;
import com.pearrity.mantys.repository.config.DbConfiguration;
import com.pearrity.mantys.repository.config.SpringContext;
import com.pearrity.mantys.salesforce.SalesforceJobConfig;
import com.pearrity.mantys.salesforce.SalesforceResourceUtil;
import com.pearrity.mantys.zoho.ZohoInvoiceResourceUtil;
import com.pearrity.mantys.zohoV2.ZohoJobConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class BatchScheduler {

  final String query =
      """
          select distinct string_agg(domains, ',') as data
          from etl_platform_jobs
          where lower(platform) = lower(?)
            and tt_end is null
          """;
  private final JobLauncher jobLauncher;
  private final Gson gson = new Gson();
  private final String[] quickBooksDomains;
  private final String[] zohoDomains;
  private final String[] salesforceDomains;
  private final String[] hubSpotJobDomains;
  private final String[] chargebeeJobDomains;
  private final AtomicBoolean enabled = new AtomicBoolean(true);
  private final AtomicInteger batchRunCounter = new AtomicInteger(0);
  private final DbConfiguration dbConfiguration;

  public BatchScheduler(
      @Autowired JobLauncher jobLauncher, @Autowired DbConfiguration dbConfiguration) {
    log.info("bean initialized ... ");
    this.jobLauncher = jobLauncher;
    this.dbConfiguration = dbConfiguration;
    chargebeeJobDomains = getJobDomains(ChargeBeeResourceUtil.platform);
    hubSpotJobDomains = getJobDomains(HubSpotResourceUtil.platform);
    salesforceDomains = getJobDomains(SalesforceResourceUtil.platform);
    quickBooksDomains = getJobDomains(QuickBooksResourceUtil.platform);
    zohoDomains = getJobDomains(ZohoInvoiceResourceUtil.platform);
  }

  private String[] getJobDomains(String platform) {
    return getJdbcTemplateByTenant(Constants.MANTYS_DOMAIN)
        .query(
            query,
            rs ->
                rs.next()
                    ? rs.getString("data") == null
                        ? new String[] {}
                        : rs.getString("data").split(",")
                    : null,
            platform);
  }

  public JdbcTemplate getJdbcTemplateByTenant(String tenant) {
    return this.dbConfiguration.getJdbcTemplateByDomain(tenant);
  }

  @Scheduled(fixedRate = 4, timeUnit = TimeUnit.HOURS)
  private void launchZohoJobForClients()
      throws IOException,
          InterruptedException,
          JobInstanceAlreadyCompleteException,
          JobExecutionAlreadyRunningException,
          JobParametersInvalidException,
          JobRestartException {
    for (String domain : zohoDomains) {
      log.info("starting zoho job for {}", domain);
      if (enabled.get() && !domain.isBlank()) {
        final String key = domain + ".zoho";
        OAuthSecret oauthSecret =
            SpringContext.getBean(AwsSecretsService.class).getSecret(key, OAuthSecret.class);
        if (oauthSecret == null)
          throw new RuntimeException("problem getting oauthSecret for " + key);
        String[] organizationIds = oauthSecret.getOrganizationIds().split(",");
        for (String org : organizationIds) {
          JobExecution jobExecution =
              jobLauncher.run(
                  ZohoJobConfig.getZohoJob(domain, org),
                  getJobParameters(domain, ZohoInvoiceResourceUtil.platform));
          log.info(
              "{} : {}",
              jobExecution.getJobInstance().getJobName(),
              jobExecution.getExitStatus().getExitDescription());
          batchRunCounter.incrementAndGet();
        }
      }
    }
  }

  @Scheduled(fixedRate = 4, timeUnit = TimeUnit.HOURS)
  private void launchSalesforceJobForClients() throws Exception {
    for (String domain : salesforceDomains) {
      log.info("batch job started ...");
      if (enabled.get() && !domain.isBlank()) {
        JobExecution jobExecution =
            jobLauncher.run(
                SalesforceJobConfig.getSalesforceJob(
                    domain, Date.from(ZonedDateTime.now().toInstant())),
                getJobParameters(domain, SalesforceResourceUtil.platform));
        log.info(
            "{} : {}",
            jobExecution.getJobInstance().getJobName(),
            jobExecution.getExitStatus().getExitDescription());
        batchRunCounter.incrementAndGet();
      }
    }
  }

  @Scheduled(fixedRate = 4, timeUnit = TimeUnit.HOURS)
  private void launchQuickbooksJobs() throws Exception {
    for (String domain : quickBooksDomains) {
      if (enabled.get() && !domain.isBlank()) {
        final String key = domain + ".quickbooks";
        List oauthSecrets =
            SpringContext.getBean(AwsSecretsService.class).getSecret(key, List.class);
        if (oauthSecrets == null)
          throw new RuntimeException("problem getting oauthSecret for " + key);
        List<String> realmIds =
            oauthSecrets.stream()
                .map(a -> (gson.fromJson(gson.toJson(a), OAuthSecret.class)).getOrganizationIds())
                .toList();
        for (String ob : realmIds) {
          ob = ob.trim();
          JobExecution jobExecution =
              jobLauncher.run(
                  QuickBooksJobConfig.getQuickBooksJob(
                      domain, ob, Date.from(ZonedDateTime.now().toInstant())),
                  getJobParameters(domain, QuickBooksResourceUtil.platform));
          log.info(
              "{} : {}",
              jobExecution.getJobInstance().getJobName(),
              jobExecution.getExitStatus().getExitDescription());
          batchRunCounter.incrementAndGet();
        }
      }
    }
  }

  @Scheduled(fixedRate = 4, timeUnit = TimeUnit.HOURS)
  private void launchChargeBeeJobs() throws Exception {
    for (String domain : chargebeeJobDomains) {
      if (enabled.get() && !domain.isBlank()) {
        JobExecution jobExecution =
            jobLauncher.run(
                ChargeBeeJobConfig.getChargeBeeJob(
                    domain, Date.from(ZonedDateTime.now().toInstant())),
                getJobParameters(domain, ChargeBeeResourceUtil.platform));
        log.info(
            "{} : {}",
            jobExecution.getJobInstance().getJobName(),
            jobExecution.getExitStatus().getExitDescription());
        batchRunCounter.incrementAndGet();
      }
    }
  }

  @Scheduled(fixedRate = 4, timeUnit = TimeUnit.HOURS)
  private void launchHubSpotJobs() throws Exception {
    for (String domain : hubSpotJobDomains) {
      if (enabled.get() && !domain.isBlank()) {
        JobExecution jobExecution =
            jobLauncher.run(
                HubSpotJobConfig.getHubSpotJob(domain, Date.from(ZonedDateTime.now().toInstant())),
                getJobParameters(domain, HubSpotResourceUtil.platform));
        log.info(
            "{} : {}",
            jobExecution.getJobInstance().getJobName(),
            jobExecution.getExitStatus().getExitDescription());
        batchRunCounter.incrementAndGet();
      }
    }
  }

  @Scheduled(fixedRate = 4, timeUnit = TimeUnit.HOURS)
  private void launchQuickbooksReportJobs() throws Exception {
    for (String domain : quickBooksDomains) {
      if (enabled.get() && !domain.isBlank()) {
        final String key = domain + ".quickbooks";
        List oauthSecrets =
            SpringContext.getBean(AwsSecretsService.class).getSecret(key, List.class);
        if (oauthSecrets == null)
          throw new RuntimeException("problem getting oauthSecret for " + key);
        List<String> realmIds =
            oauthSecrets.stream()
                .map(a -> (gson.fromJson(gson.toJson(a), OAuthSecret.class)).getOrganizationIds())
                .toList();
        for (String ob : realmIds) {
          ob = ob.trim();
          JobExecution jobExecution =
              jobLauncher.run(
                  QuickBooksJobConfig.getQuickBooksReportJob(
                      domain, ob, Date.from(ZonedDateTime.now().toInstant())),
                  getJobParameters(domain, QuickBooksResourceUtil.platform));
          log.info(
              "{} : {}",
              jobExecution.getJobInstance().getJobName(),
              jobExecution.getExitStatus().getExitDescription());
          batchRunCounter.incrementAndGet();
        }
      }
    }
  }

  public JobParameters getJobParameters(String tenant, String platform) {
    JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
    jobParametersBuilder.addDate("startTime", Date.from(ZonedDateTime.now().toInstant()));
    jobParametersBuilder.addString("tenant", tenant);
    jobParametersBuilder.addString("platform", platform);
    return jobParametersBuilder.toJobParameters();
  }
}
