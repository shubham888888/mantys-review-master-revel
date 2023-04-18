package com.pearrity.mantys.auth.util;

import com.pearrity.mantys.domain.auth.AuthPrincipal;
import com.pearrity.mantys.domain.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

import static java.util.Objects.isNull;

@Component
public class AuthUserUtil {

  public static String getUrl(HttpServletRequest req) {
    String reqUrl = req.getRequestURL().toString();
    String queryString = req.getQueryString();
    if (queryString != null) {
      reqUrl += "?" + queryString;
    }
    return reqUrl;
  }

  public AuthPrincipal getAuthPrincipalFromSecurityContext() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (isNull(auth)) {
      throw new IllegalStateException("user not found, try logging in.");
    }
    return ((AuthPrincipal) auth.getPrincipal());
  }

  public String getUserDomainFromSecurityContext() {
    AuthPrincipal principal = getAuthPrincipalFromSecurityContext();
    return principal != null ? principal.getDomain() : null;
  }

  public String getUserEmailFromSecurityContext() {
    AuthPrincipal principal = getAuthPrincipalFromSecurityContext();
    return principal != null ? principal.getEmail() : null;
  }

  public String getUserNameFromSecurityContext() {
    AuthPrincipal principal = getAuthPrincipalFromSecurityContext();
    return principal != null ? principal.getName() : null;
  }

  public Role getRoleFromSecurityContext() {
    AuthPrincipal principal = getAuthPrincipalFromSecurityContext();
    return principal != null ? principal.getRole() : null;
  }

  public Long getUserIdFromSecurityContext() {
    AuthPrincipal principal = getAuthPrincipalFromSecurityContext();
    return principal != null ? principal.getUserId() : null;
  }
}
