package com.pearrity.mantys.repository;

import com.pearrity.mantys.domain.User;
import com.pearrity.mantys.domain.enums.Role;
import com.pearrity.mantys.domain.utils.UtilFunctions;
import com.pearrity.mantys.repository.config.DbConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class SudoRepositoryImpl implements SudoRepository {

  @Autowired private DbConfiguration dbConfiguration;

  @Autowired private UserRepository userRepository;

  @Override
  public Object createUpdateDevAccountsForEachClient(String password) {
    Set<Object> jdbcTemplateMapKeys = dbConfiguration.getClientJdbcTemplateMapKeys();
    int count = 0;
    for (Object domain : jdbcTemplateMapKeys) {
      String mail = "dev_mantys@" + domain;
      int rows = 0;
      try {
        rows =
            userRepository.save(
                User.builder()
                    .email(mail)
                    .role(Role.USER)
                    .name("Mantys Dev")
                    .password(password)
                    .build(),
                UtilFunctions.getDomainFromEmail(mail));
      } catch (Exception e) {
        log.error("Error occurred while inserting dev records: " + e.getMessage(), e);
      }
      if (rows == 0) {
        User user =
            userRepository.findByEmail(mail, UtilFunctions.getDomainFromEmail(mail)).orElseThrow();
        user.setPassword(password);
        rows = userRepository.save(user, UtilFunctions.getDomainFromEmail(user.getEmail()));
      }
      count += rows;
    }
    return count + " accounts updated";
  }

  @Override
  public Object createAccountForUser(Map<String, String> map) {
    int rows =
        userRepository.save(
            User.builder().email(map.get("email")).role(Role.USER).name(map.get("name")).build(),
            map.get("domain"));
    return rows + "account created";
  }

  @Override
  public Object updatePrivilegesForDevAccount() {
    List<JdbcTemplate> jdbcTemplates = dbConfiguration.getAllClientJdbcTemplates();
    int count = 0;
    for (JdbcTemplate jdbcTemplate : jdbcTemplates) {
      count +=
          jdbcTemplate.update(
              """
                          insert
                          	into
                          	public.privileges( user_id ,
                          	action,
                          	object_id )
                          select
                          	u.id as
                            user_id ,
                          	'READ' as action ,
                          	o.id as object_id
                          from
                          	users u,
                          	object o
                          where
                          	u.email like 'dev_mantys%' on
                          	conflict on
                          	constraint
                            privileges_user_id_object_id_key do nothing
                          """);
    }
    return count + "rows inserted";
  }
}
