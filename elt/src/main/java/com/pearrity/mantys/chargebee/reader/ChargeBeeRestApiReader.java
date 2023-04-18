package com.pearrity.mantys.chargebee.reader;

import com.pearrity.mantys.chargebee.ChargeBeeAuthUtil;
import com.pearrity.mantys.chargebee.reader.adapter.ChargeBeeEntityReaderAdapter;
import com.pearrity.mantys.chargebee.reader.loader.ChargeBeeEntityLoader;
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

public class ChargeBeeRestApiReader implements ItemReader<JSONObject> {
  private final Logger log = Logger.getLogger(ChargeBeeRestApiReader.class.getName());

  private final List<String> list;
  private final ChargeBeeAuthUtil chargeBeeAuthUtil;
  private final EtlReaderAdapter ChargeBeeReaderAdapter;

  public ChargeBeeRestApiReader(ChargeBeeAuthUtil ChargeBeeAuthUtil, String type) throws Exception {
    this.chargeBeeAuthUtil = ChargeBeeAuthUtil;
    EtlLoader loader = getLoaderInstance(type);
    final String chargebee = "chargebee";
    Timestamp getLastSyncTime =
        getLastSyncByDomainAndPlatform(type, ChargeBeeAuthUtil.getTenant(), chargebee, null);
    Set<String> idsToSync = new HashSet<>(loader.load(getLastSyncTime));
    idsToSync.addAll(
        addAllPreviousUnsuccessfulSyncIds(ChargeBeeAuthUtil.getTenant(), type, chargebee, null));
    this.list = new ArrayList<>(idsToSync.stream().toList());
    this.ChargeBeeReaderAdapter = getReaderAdapter(type);
  }

  @Override
  public JSONObject read() throws IOException {
    synchronized (list) {
      if (!list.isEmpty()) {
        return ChargeBeeReaderAdapter.getResource(list.remove(0));
      }
      return null;
    }
  }

  private EtlReaderAdapter getReaderAdapter(String type) {
    return new ChargeBeeEntityReaderAdapter(chargeBeeAuthUtil, type);
  }

  private EtlLoader getLoaderInstance(String type) {
    return new ChargeBeeEntityLoader(chargeBeeAuthUtil, type);
  }
}
