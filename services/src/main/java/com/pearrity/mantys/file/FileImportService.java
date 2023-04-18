package com.pearrity.mantys.file;

import com.pearrity.mantys.domain.FileImport;
import com.pearrity.mantys.domain.Page;
import org.springframework.http.ResponseEntity;

public interface FileImportService {
  ResponseEntity<Object> importFile(FileImport fileImport, Boolean forced);

  ResponseEntity<Object> getActiveImportByName(String name);

  ResponseEntity<Object> isFileNameImported(String name);

  ResponseEntity<Object> getImportById(Long id);

  ResponseEntity<Object> getAllImports(Page page);

  ResponseEntity<Object> getFileImportTemplates();
}
