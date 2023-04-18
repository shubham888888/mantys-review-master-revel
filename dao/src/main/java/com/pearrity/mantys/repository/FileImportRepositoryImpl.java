package com.pearrity.mantys.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.pearrity.mantys.domain.FileImport;
import com.pearrity.mantys.domain.FileImportTemplate;
import com.pearrity.mantys.domain.Page;
import com.pearrity.mantys.domain.enums.Frequency;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Repository
public class FileImportRepositoryImpl implements FileImportRepository {

  @Autowired JdbcTemplate jdbcTemplate;

  private FileImportTemplate getFileImportTemplateFromResultSet(ResultSet rs, boolean extraDetails)
      throws SQLException, IOException {
    FileImportTemplate fileImportTemplate =
        FileImportTemplate.builder()
            .schema(new ObjectMapper().readValue(rs.getString("schema").getBytes(), HashMap.class))
            .id(rs.getLong("id"))
            .label(rs.getString("label"))
            .buffer(rs.getInt("buffer"))
            .frequency(Frequency.valueOf(rs.getString("frequency")))
            .build();
    if (extraDetails) {
      fileImportTemplate.setInsertQuery(rs.getString("insert_query"));
      fileImportTemplate.setInsertTypes(rs.getString("insert_types"));
      fileImportTemplate.setInsertPlaceholders(rs.getString("insert_placeholders"));
      fileImportTemplate.setTableName(rs.getString("table_name"));
    }
    return fileImportTemplate;
  }

  @Override
  public List<FileImportTemplate> getAllFileImportTemplates() {
    String query =
        """
                    select
                    	*
                    from
                    	public. file_import_templates
                    where
                    	tt_end is null
                    order by
                    	id asc
                    """;
    //    query = addPaginationToQuery(query, page);
    return jdbcTemplate.query(
        query,
        rs -> {
          List<FileImportTemplate> importTemplates = new ArrayList<>();
          while (rs.next()) {
            try {
              importTemplates.add(getFileImportTemplateFromResultSet(rs, false));
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
          return importTemplates;
        });
  }

  @Override
  @Transactional
  public Boolean save(FileImport fileImport, Boolean forced) {
    if (Boolean.FALSE.equals(forced)
        && Boolean.FALSE.equals(
            jdbcTemplate.queryForObject(
                """
                            select
                            	count(*) = 0
                            from
                            	file_imports
                            where
                            	name = ?
                            	and tt_end is null
                            """,
                Boolean.class,
                fileImport.getName()))) {
      throw new Error("name not unique ...");
    }
    FileImportTemplate fileImportTemplate =
        getFileImportTemplateById(fileImport.getImportTemplateId());
    int result =
        jdbcTemplate.update(
            """
                        update
                        	public.file_imports
                        set
                        	tt_end = ?
                        where
                        	name = ?
                        	and tt_end is null
                        """,
            fileImport.getTtBegin(),
            fileImport.getName());
    if (result == 1) {
      updateCorrespondingTemplateTablesData(
          fileImport.getName(), fileImportTemplate.getTableName(), fileImport.getTtBegin());
    }
    if (result == 0 || forced) {
      this.saveDataIntoCorrespondingTemplateTable(fileImport, fileImportTemplate);
      result =
          jdbcTemplate.update(
              """
                          insert
                          	into
                          	public.file_imports ( name ,
                          	data,
                          	imported_by_user_id ,
                          	tt_begin ,
                          	import_template_id ,
                          	file_extensions,
                          	vt_end ,
                          	vt_begin,
                          	version )
                          values
                           ( ?,?,?,?,? ,?,? ,?,?)
                          """,
              new Object[] {
                fileImport.getName(),
                new Gson().toJson(fileImport.getData()),
                fileImport.getImportedByUserId(),
                fileImport.getTtBegin(),
                fileImport.getImportTemplateId(),
                fileImport.getFileExtensions(),
                fileImport.getVtEnd(),
                fileImport.getVtBegin(),
                fileImport.getVersion()
              },
              new int[] {
                Types.VARCHAR,
                Types.OTHER,
                Types.INTEGER,
                Types.TIMESTAMP,
                Types.INTEGER,
                Types.VARCHAR,
                Types.TIMESTAMP,
                Types.TIMESTAMP,
                Types.VARCHAR
              });
    } else throw new Error("cannot replace already existing record implicitly");
    return result > 0;
  }

  private void updateCorrespondingTemplateTablesData(
      String name, String tableName, Timestamp ttBegin) {
    jdbcTemplate.update(
        String.format(
            """
                    update
                        %s
                    set
                        tt_end = ?
                    where
                        tt_end is null
                        and file_import_name = ?
                        """,
            tableName),
        ttBegin,
        name);
  }

  private FileImportTemplate getFileImportTemplateById(Long id) {
    return jdbcTemplate.query(
        """
                    select
                    	*
                    from
                    	file_import_templates
                    where
                    	id = ?
                    	and tt_end is null
                    """,
        rs -> {
          try {
            return rs.next() ? getFileImportTemplateFromResultSet(rs, true) : null;
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        },
        id);
  }

  @Override
  public List<FileImport> getAllImports(Page page) {
    String query =
        """
            select
            	*
            from
            	public. file_imports
            where
            	tt_end is null
            """;
    query = addPaginationToQuery(query, page);
    return jdbcTemplate.query(
        query,
        rs -> {
          List<FileImport> importTemplates = new ArrayList<>();
          while (rs.next()) {
            importTemplates.add(getFileImportFromResultSet(rs, false));
          }
          return importTemplates;
        });
  }

  private String addPaginationToQuery(String query, Page page) {
    if (page.getLimit() == null) page.setLimit(10L);
    if (page.getOffset() == null) page.setOffset(0L);
    //    else page.setOffset(page.getLimit() * page.getOffset());
    if (page.getSortBy() == null || page.getSortBy().contains(" ")) page.setSortBy("id");
    if (page.getDir() == null) page.setDir(Sort.Direction.DESC);
    return query
        + " order by "
        + page.getSortBy()
        + " "
        + page.getDir()
        + " limit "
        + page.getLimit()
        + " offset "
        + page.getOffset();
  }

  @Override
  public FileImport getImportById(Long id) {
    return jdbcTemplate.query(
        """
                    select
                    	*
                    from
                    	public. file_imports
                    where
                    	id = ?
                    	and tt_end is null
                    """,
        rs -> rs.next() ? getFileImportFromResultSet(rs, true) : null,
        id);
  }

  @Override
  public FileImport getActiveImportByName(String name) {
    return jdbcTemplate.query(
        """
                    select
                    	*
                    from
                    	public. file_imports
                    where
                    	name = ?
                    	and tt_end is null
                    """,
        rs -> rs.next() ? getFileImportFromResultSet(rs, true) : null,
        name);
  }

  @Override
  public Boolean isFileNameImported(String name) {
    return jdbcTemplate.queryForObject(
        """
                    select
                    	count(*) > 0
                    from
                    	public. file_imports
                    where
                    	name = ?
                    	and tt_end is null
                    """,
        Boolean.class,
        name);
  }

  @Transactional
  private void saveDataIntoCorrespondingTemplateTable(
      FileImport fileImport, FileImportTemplate fileImportTemplate) {
    if (fileImportTemplate != null && fileImportTemplate.getTableName() != null) {
      List<Object[]> objects = new ArrayList<>();
      String[] keys = fileImportTemplate.getInsertPlaceholders().split(",");
      for (HashMap ob : fileImport.getData()) {
        JSONObject object = new JSONObject(ob);
        Object[] arr = new Object[keys.length + 6];
        for (int i = 0; i < keys.length; i++) {
          String key = keys[i];
          try {
            Object obj = object.get(key.trim());
            arr[i] = obj;
          } catch (JSONException e) {
            arr[i] = null;
          }
        }
        arr[keys.length] = fileImport.getVtBegin();
        arr[keys.length + 1] = fileImport.getVtEnd();
        arr[keys.length + 2] = fileImport.getTtBegin();
        arr[keys.length + 3] = fileImport.getName();
        arr[keys.length + 4] = fileImport.getVersion();
        arr[keys.length + 5] = object.toString();
        objects.add(arr);
      }
      objects.forEach(a -> System.out.println(Arrays.toString(a)));
      int[] types = new int[keys.length + 6];
      int k = 0;
      for (String s : fileImportTemplate.getInsertTypes().split(","))
        types[k++] = Integer.parseInt(s.trim());
      jdbcTemplate.batchUpdate(fileImportTemplate.getInsertQuery(), objects, types);
    }
  }

  private FileImport getFileImportFromResultSet(ResultSet rs, boolean data) throws SQLException {
    FileImport fileImport =
        FileImport.builder()
            .importedByUserId(rs.getLong("imported_by_user_id"))
            .name(rs.getString("name"))
            .ttBegin(rs.getTimestamp("tt_begin"))
            .ttEnd(rs.getTimestamp("tt_end"))
            .vtBegin(rs.getTimestamp("vt_begin"))
            .vtEnd(rs.getTimestamp("vt_end"))
            .fileExtensions(rs.getString("file_extensions"))
            .version(rs.getString("version"))
            .importTemplateId(rs.getLong("import_template_id"))
            .id(rs.getLong("id"))
            .build();
    if (data) fileImport.setData(new Gson().fromJson(rs.getString("data"), List.class));
    return fileImport;
  }
}
