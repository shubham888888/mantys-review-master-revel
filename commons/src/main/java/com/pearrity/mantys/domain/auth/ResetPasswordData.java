package com.pearrity.mantys.domain.auth;

import javax.validation.constraints.NotNull;

public record ResetPasswordData(
    @NotNull String email,
    @NotNull String password,
    @NotNull String primaryToken,
    @NotNull String secondaryToken) {}
