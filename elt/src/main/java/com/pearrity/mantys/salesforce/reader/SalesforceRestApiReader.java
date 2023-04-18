package com.pearrity.mantys.salesforce.reader;

import com.pearrity.mantys.domain.utils.Constants;
import com.pearrity.mantys.interfaces.RequestType;
import com.pearrity.mantys.salesforce.SalesforceAuthUtil;
import com.pearrity.mantys.salesforce.SalesforceResourceUtil;
import com.pearrity.mantys.utils.UtilFunctions;
import com.squareup.okhttp.Request;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.batch.item.ItemReader;

import java.time.ZonedDateTime;
import java.util.*;

import static com.pearrity.mantys.salesforce.SalesforceResourceUtil.*;
import static com.pearrity.mantys.utils.UtilFunctions.addAllPreviousUnsuccessfulSyncIds;

@Slf4j
public class SalesforceRestApiReader implements ItemReader<String> {

  private final List<String> resources;
  private final String type;
  private final SalesforceAuthUtil salesforceAuthUtil;

  public SalesforceRestApiReader(String type, String tenant, SalesforceAuthUtil salesforceAuthUtil)
      throws Exception {
    this.salesforceAuthUtil = salesforceAuthUtil;
    this.type = type;
    createNextJobsForSalesforce(salesforceAuthUtil, type);
    Set<String> resources = new HashSet<>(loadResources(type, tenant));
    resources.addAll(addAllPreviousUnsuccessfulSyncIds(tenant, platform, type, null));
    this.resources = new ArrayList<>(resources.stream().toList());
  }

  private List<String> loadResources(String type, String tenant) throws Exception {
    List<String> resourceList = new ArrayList<>();
    String url = salesforceAuthUtil.getBulkJobQueryUrl();
    boolean controlVar;
    List<Map<String, Object>> ourJobs =
        SalesforceResourceUtil.getJobsFromDBByType(type, salesforceAuthUtil.getTenant());
    do {
      Request request = UtilFunctions.createGetRequest(url, salesforceAuthUtil.getAccessToken());
      JSONObject object =
          UtilFunctions.makeWebRequest(
              null,
              request,
              tenant,
              salesforceAuthUtil,
              true,
              RequestType.Loader,
              type,
              platform,
              null);
      controlVar = Boolean.FALSE.equals(object.getBoolean("done"));
      resourceList.addAll(processJobInfo(object, ourJobs));
      if (controlVar) {
        url = salesforceAuthUtil.getMainBaseUrl() + object.getString("nextRecordsUrl");
      }
    } while (controlVar);
    return resourceList;
  }

  private List<String> processJobInfo(JSONObject object, List<Map<String, Object>> ourJobs)
      throws Exception {
    List<String> jobIds = new ArrayList<>();
    for (Object ob : object.getJSONArray("records")) {
      JSONObject resource = (JSONObject) ob;
      Map<String, Object> jobDetail = null;
      for (Map<String, Object> map : ourJobs) {
        if (((String) map.get("id")).equalsIgnoreCase(resource.getString("id"))) {
          jobDetail = map;
          break;
        }
      }
      if (jobDetail == null) continue;
      log.info("Job details present: " + jobDetail.toString());
      if (resource.getString("state").equalsIgnoreCase("JobComplete")
          && resource.getString("object").equalsIgnoreCase(this.type))
        jobIds.add(resource.getString("id"));
      else if (resource.getString("state").equalsIgnoreCase("Failed")) {
        log.error("the job with id : {} has failed ...", resource.getString("id"));
        log.error("information about job {}", resource.toMap());
        updateJobStatusInDB(salesforceAuthUtil, (String) jobDetail.get("id"), Constants.fail);
        createJobAdapter(
            (String) jobDetail.get("entity"),
            salesforceAuthUtil.getBulkJobQueryUrl(),
            (String) jobDetail.get("query"),
            (Long) jobDetail.get("milestone_id"),
            salesforceAuthUtil);
      } else {
        log.info(
            "the job with id : {} has status {} at {} ...",
            resource.getString("id"),
            resource.getString("state"),
            ZonedDateTime.now());
        log.info("information about job {}", resource.toMap());
      }
    }
    return jobIds;
  }

  @Override
  public String read() {
    synchronized (resources) {
      if (!resources.isEmpty()) {
        return resources.remove(0);
      }
      return null;
    }
  }
}
