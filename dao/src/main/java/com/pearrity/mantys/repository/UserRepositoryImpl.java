package com.pearrity.mantys.repository;

import com.pearrity.mantys.domain.Privileges;
import com.pearrity.mantys.domain.User;
import com.pearrity.mantys.domain.enums.Action;
import com.pearrity.mantys.domain.enums.Role;
import com.pearrity.mantys.repository.config.DbConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Transactional
@Repository
@Slf4j
public class UserRepositoryImpl implements UserRepository {

  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired DbConfiguration dbConfiguration;

  public User getUserFromResultSet(ResultSet rs) throws SQLException {
    Long id = rs.getLong("id");
    return User.builder()
        .id(id)
        .deletionTime((Timestamp) rs.getObject("deletion_time"))
        .email(rs.getString("email"))
        .name(rs.getString("name"))
        .role(Role.valueOf((String) rs.getObject("role")))
        .password(rs.getString("password"))
        .primaryResetToken(rs.getString("primary_reset_token"))
        .secondaryResetToken(rs.getString("secondary_reset_token"))
        .resetTokenExpiryTime((Timestamp) rs.getObject("reset_token_expiry_time"))
        .resetTokenCreationTime((Timestamp) rs.getObject("reset_token_creation_time"))
        .refreshToken(rs.getString("refresh_token"))
        .refreshTokenCreationTime((Timestamp) rs.getObject("refresh_token_creation_time"))
        .build();
  }

  public Privileges getPrivilegeFromResultSet(ResultSet rs) throws SQLException {

    return Privileges.builder()
        .id(rs.getLong("id"))
        .userId(rs.getLong("user_id"))
        .action(Action.valueOf(rs.getString("action")))
        .objectId(rs.getLong("object_id"))
        .build();
  }

  @Override
  public Optional<User> findByEmail(String email, String domain) {
    if (domain != null) jdbcTemplate = dbConfiguration.getJdbcTemplateByDomain(domain);
    return Optional.ofNullable(
        jdbcTemplate.query(
            """
                        select
                        	*
                        from
                        	users
                        where
                        	email = ?
                        """,
            (rs) -> rs.next() ? getUserFromResultSet(rs) : null,
            email));
  }

  @Override
  public Set<Privileges> getUserPrivilegesById(Long id) {
    return jdbcTemplate.query(
        """
                    select
                    	up.*
                    from
                    	privileges up
                    where
                    	up.user_id = ?
                    """,
        (rs) -> {
          Set<Privileges> privileges = new HashSet<>();
          while (rs.next()) {
            privileges.add(getPrivilegeFromResultSet(rs));
          }
          return privileges;
        },
        id);
  }

  @Override
  public Optional<User> findByRefreshToken(String token) {
    return Optional.ofNullable(
        jdbcTemplate.query(
            """
                        select
                        	*
                        from
                        	users u
                        where
                        	refresh_token = ?
                        """,
            (rs) -> rs.next() ? getUserFromResultSet(rs) : null,
            token));
  }

  @Override
  public boolean findIfUniquePrimaryTokenString(String generatedString) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            """
                        select
                        	count(*) > 0
                        from
                        	users u
                        where
                        	u. primary_reset_token = ?
                        """,
            Boolean.class,
            generatedString));
  }

  @Override
  public boolean findIfUniqueSecondaryTokenString(String generatedString) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            """
                        select
                        	count(*) > 0
                        from
                        	users u
                        where
                        	u. secondary_reset_token = ?
                        """,
            Boolean.class,
            generatedString));
  }

  @Override
  public Optional<User> findByUserIdAndPrimaryAndSecondaryTokenAndNotExpired(
      String email, String primaryToken, String secondaryToken, Timestamp now) {
    return Optional.ofNullable(
        jdbcTemplate.query(
            """
                        select
                        	*
                        from
                        	users u
                        where
                        	u.email = ?
                        	and u. primary_reset_token = ?
                        	and u.secondary_reset_token = ?
                        	and u. reset_token_expiry_time > ?
                        """,
            (rs) -> rs.next() ? getUserFromResultSet(rs) : null,
            email,
            primaryToken,
            secondaryToken,
            now));
  }

  @Override
  public int save(User user) {
    return save(user, null);
  }

  @Override
  public int save(User user, String domain) {
    JdbcTemplate jdbcTemplate = this.jdbcTemplate;
    if (domain != null) jdbcTemplate = dbConfiguration.getJdbcTemplateByDomain(domain);
    if (user.getId() == null) {
      return jdbcTemplate.update(
          """
                      insert
                      	into
                      	public.users( deletion_time ,
                      	email,
                      	name,
                      	password,
                      	primary_reset_token ,
                      	reset_token_creation_time ,
                      	reset_token_expiry_time ,
                      	refresh_token_creation_time ,
                      	refresh_token ,
                      	secondary_reset_token ,
                      	role)
                      values ( ?,?,?,?,?,?,?,?,?,?,?)
                      """,
          user.getDeletionTime(),
          user.getEmail(),
          user.getName(),
          user.getPassword(),
          user.getPrimaryResetToken(),
          user.getResetTokenCreationTime(),
          user.getResetTokenExpiryTime(),
          user.getRefreshTokenCreationTime(),
          user.getRefreshToken(),
          user.getSecondaryResetToken(),
          user.getRole().name());
    } else {
      return jdbcTemplate.update(
          """
                      update
                      	public.users
                      set
                      	deletion_time =?,
                      	email =?,
                      	name =?,
                      	password =?,
                      	primary_reset_token =?,
                      	reset_token_creation_time =?,
                      	reset_token_expiry_time =?,
                      	refresh_token_creation_time =?,
                      	refresh_token =?,
                      	secondary_reset_token =?,
                      	role =?
                      where
                      	id = ?
                      """,
          user.getDeletionTime(),
          user.getEmail(),
          user.getName(),
          user.getPassword(),
          user.getPrimaryResetToken(),
          user.getResetTokenCreationTime(),
          user.getResetTokenExpiryTime(),
          user.getRefreshTokenCreationTime(),
          user.getRefreshToken(),
          user.getSecondaryResetToken(),
          user.getRole().name(),
          user.getId());
    }
  }
}
