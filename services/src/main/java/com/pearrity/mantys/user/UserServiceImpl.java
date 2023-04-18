package com.pearrity.mantys.user;

import com.pearrity.mantys.auth.util.AuthUserUtil;
import com.pearrity.mantys.domain.User;
import com.pearrity.mantys.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

  @Autowired private UserRepository userRepository;

  @Autowired private AuthUserUtil authUserUtil;

  @Override
  public User findById(Long userId) {
    return null;
  }

  @Override
  public Map<String, Object> getUserDetails() {
    return Map.of(
        "role",
        authUserUtil.getRoleFromSecurityContext(),
        "email",
        authUserUtil.getUserEmailFromSecurityContext(),
        "name",
        authUserUtil.getUserNameFromSecurityContext(),
        "id",
        authUserUtil.getUserIdFromSecurityContext());
  }

  @Override
  public Object getUserPrivileges() {
    return userRepository.getUserPrivilegesById(authUserUtil.getUserIdFromSecurityContext());
  }
}
