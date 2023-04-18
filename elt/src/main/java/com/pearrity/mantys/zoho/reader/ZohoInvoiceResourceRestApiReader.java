package com.pearrity.mantys.zoho.reader;

import com.pearrity.mantys.zoho.ZohoInvoiceAuthUtil;
import com.pearrity.mantys.zoho.ZohoInvoiceResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ZohoInvoiceResourceRestApiReader<T> {

  private final ZohoInvoiceAuthUtil zohoInvoiceAuthUtil;

  private static final String zoho = ".zoho.";

  private static final Logger log = LoggerFactory.getLogger(ZohoInvoiceResourceRestApiReader.class);
  private final Class<?> type;

  private final String[] organizationIds;

  public ZohoInvoiceResourceRestApiReader(Class<?> t, String domain, String[] organizationIds)
      throws IOException, InterruptedException {
    this.organizationIds = organizationIds;
    this.zohoInvoiceAuthUtil = ZohoInvoiceResourceUtil.getAuthUtilBYDomain(domain);
    this.type = t;
  }

  public List<T> loadAllResources() throws IOException, InterruptedException {
    String endpoint = "/api/v3/" + ZohoInvoiceResourceUtil.getEndPoint(type);
    String url = String.format("%s%s", zohoInvoiceAuthUtil.resourceBaseUrl, endpoint);
    List<T> zohoInvoiceResources = new ArrayList<>();
    for (String organizationId : this.organizationIds) {
      zohoInvoiceResources.addAll(
          new ZohoInvoiceResourceUtil<T>()
              .loadResourcesIntoList(
                  url,
                  zohoInvoiceAuthUtil,
                  ZohoInvoiceResourceUtil.getEndPoint(type),
                  type,
                  organizationId));
      log.info("Data fetched for orgId: " + organizationId);
    }
    return zohoInvoiceResources;
  }
}
