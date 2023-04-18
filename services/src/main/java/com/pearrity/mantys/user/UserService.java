package com.pearrity.mantys.user;

import com.pearrity.mantys.domain.User;

import java.util.Map;

public interface UserService {

  User findById(Long userId);

  Map<String, Object> getUserDetails();

  Object getUserPrivileges();
}
