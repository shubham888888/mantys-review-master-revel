package com.pearrity.mantys.repository;

import com.pearrity.mantys.domain.FileImport;
import com.pearrity.mantys.domain.FileImportTemplate;
import com.pearrity.mantys.domain.Page;

import java.util.List;

public interface FileImportRepository {

  List<FileImportTemplate> getAllFileImportTemplates();

  Boolean save(FileImport fileImport, Boolean forced);

  List<FileImport> getAllImports(Page page);

  FileImport getImportById(Long id);

  FileImport getActiveImportByName(String name);

  Boolean isFileNameImported(String name);
}
