package com.pearrity.mantys.repository;

import com.pearrity.mantys.domain.Privileges;
import com.pearrity.mantys.domain.User;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.Set;

public interface UserRepository {

  Optional<User> findByEmail(String email, String domainFromEmail);

  Set<Privileges> getUserPrivilegesById(Long id);

  Optional<User> findByRefreshToken(String token);

  boolean findIfUniquePrimaryTokenString(String generatedString);

  boolean findIfUniqueSecondaryTokenString(String generatedString);

  Optional<User> findByUserIdAndPrimaryAndSecondaryTokenAndNotExpired(
      String mail, String primaryToken, String secondaryToken, Timestamp now);

  int save(User user);

  int save(User user, String domain);
}
