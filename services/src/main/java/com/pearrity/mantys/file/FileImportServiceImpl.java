package com.pearrity.mantys.file;

import com.pearrity.mantys.auth.util.AuthUserUtil;
import com.pearrity.mantys.domain.FileImport;
import com.pearrity.mantys.domain.Page;
import com.pearrity.mantys.repository.FileImportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.ZonedDateTime;

@Service
public class FileImportServiceImpl implements FileImportService {

  @Autowired FileImportRepository fileImportRepository;

  @Autowired AuthUserUtil authUserUtil;

  @Override
  public ResponseEntity<Object> importFile(FileImport fileImport, Boolean forced) {
    fileImport.setTtBegin(Timestamp.from(ZonedDateTime.now().toInstant()));
    fileImport.setImportedByUserId(authUserUtil.getUserIdFromSecurityContext());
    if (forced == null) forced = false;
    return ResponseEntity.ok(fileImportRepository.save(fileImport, forced));
  }

  @Override
  public ResponseEntity<Object> getActiveImportByName(String name) {
    return ResponseEntity.ok(fileImportRepository.getActiveImportByName(name));
  }

  @Override
  public ResponseEntity<Object> isFileNameImported(String name) {
    return ResponseEntity.ok(fileImportRepository.isFileNameImported(name));
  }

  @Override
  public ResponseEntity<Object> getImportById(Long id) {
    return ResponseEntity.ok(fileImportRepository.getImportById(id));
  }

  @Override
  public ResponseEntity<Object> getAllImports(Page page) {
    return ResponseEntity.ok(fileImportRepository.getAllImports(page));
  }

  @Override
  public ResponseEntity<Object> getFileImportTemplates() {
    return ResponseEntity.ok(fileImportRepository.getAllFileImportTemplates());
  }
}
