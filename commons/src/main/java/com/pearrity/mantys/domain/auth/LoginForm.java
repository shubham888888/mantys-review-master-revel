package com.pearrity.mantys.domain.auth;

import javax.validation.constraints.NotNull;

public record LoginForm(@NotNull String email, @NotNull String password) {}
