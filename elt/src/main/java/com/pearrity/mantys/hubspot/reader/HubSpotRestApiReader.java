package com.pearrity.mantys.hubspot.reader;

import com.pearrity.mantys.hubspot.HubSpotAuthUtil;
import com.pearrity.mantys.hubspot.reader.adapter.HubspotEntityReaderAdapter;
import com.pearrity.mantys.hubspot.reader.loader.HubspotEntityLoader;
import com.pearrity.mantys.interfaces.EtlLoader;
import com.pearrity.mantys.interfaces.EtlReaderAdapter;
import org.json.JSONObject;
import org.springframework.batch.item.ItemReader;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static com.pearrity.mantys.utils.UtilFunctions.addAllPreviousUnsuccessfulSyncIds;
import static com.pearrity.mantys.utils.UtilFunctions.getLastSyncByDomainAndPlatform;

public class HubSpotRestApiReader implements ItemReader<JSONObject> {
  private final Logger log = Logger.getLogger(HubSpotRestApiReader.class.getName());

  private final List<String> list;
  private final HubSpotAuthUtil hubSpotAuthUtil;

  private final EtlReaderAdapter hubSpotReaderAdapter;

  public HubSpotRestApiReader(HubSpotAuthUtil hubSpotAuthUtil, String type) throws Exception {
    this.hubSpotAuthUtil = hubSpotAuthUtil;
    EtlLoader loader = getLoaderInstance(type);
    String hubspot = "hubspot";
    Timestamp getLastSyncTime =
        getLastSyncByDomainAndPlatform(type, hubSpotAuthUtil.getTenant(), hubspot, null);
    Set<String> idsToSync = new HashSet<>(loader.load(getLastSyncTime));
    idsToSync.addAll(
        addAllPreviousUnsuccessfulSyncIds(hubSpotAuthUtil.getTenant(), type, hubspot, null));
    this.list = new ArrayList<>(idsToSync.stream().toList());
    this.hubSpotReaderAdapter = getReaderAdapter(type);
  }

  @Override
  public JSONObject read() throws IOException {
    synchronized (list) {
      if (!list.isEmpty()) {
        return hubSpotReaderAdapter.getResource(list.remove(0));
      }
      return null;
    }
  }

  private EtlReaderAdapter getReaderAdapter(String type) {
    return new HubspotEntityReaderAdapter(hubSpotAuthUtil, type);
  }

  private EtlLoader getLoaderInstance(String type) {
    return new HubspotEntityLoader(hubSpotAuthUtil, type);
  }
}
