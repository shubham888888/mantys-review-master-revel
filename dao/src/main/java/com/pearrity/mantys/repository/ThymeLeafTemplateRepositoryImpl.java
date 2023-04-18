package com.pearrity.mantys.repository;

import com.pearrity.mantys.domain.ThymeleafTemplate;
import com.pearrity.mantys.domain.utils.Constants;
import com.pearrity.mantys.repository.config.DbConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.sql.ResultSet;
import java.sql.SQLException;

@Transactional
@Repository
public class ThymeLeafTemplateRepositoryImpl implements ThymeLeafTemplateRepository {

  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public ThymeLeafTemplateRepositoryImpl(DbConfiguration dbConfiguration) {
    this.jdbcTemplate = dbConfiguration.getJdbcTemplateByDomain(Constants.MANTYS_DOMAIN);
  }

  @Override
  public ThymeleafTemplate findByTemplateName(String name) {
    return jdbcTemplate.query(
        """
                    select
                    	*
                    from
                    	thymeleaf_template
                    where
                    	name = ?
                    """,
        (rs) -> rs.next() ? extractSingleTemplate(rs) : null,
        name);
  }

  private ThymeleafTemplate extractSingleTemplate(ResultSet rs) throws SQLException {
    return ThymeleafTemplate.builder()
        .html(rs.getString("html"))
        .name(rs.getString("name"))
        .id(rs.getLong("id"))
        .build();
  }
}
