package com.pearrity.mantys.interfaces;

import java.sql.Timestamp;
import java.util.List;

public interface EtlLoader {
  List<String> load(Timestamp lastSyncTime) throws Exception;
}
