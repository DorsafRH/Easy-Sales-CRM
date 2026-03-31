package com.crm.modules.auth.service;

import com.crm.modules.auth.dto.AuthResponse;
import com.crm.modules.auth.dto.LoginRequest;

public interface IAuthService {

    AuthResponse login(LoginRequest request);
}