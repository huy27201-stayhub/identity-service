package com.stayhub.identity.service;

import com.stayhub.identity.dto.request.LoginRequest;
import com.stayhub.identity.dto.request.RegisterRequest;
import com.stayhub.identity.dto.response.LoginResponse;
import com.stayhub.identity.exception.BadRequestException;

public interface AuthService {
  String register(RegisterRequest request) throws BadRequestException;

  LoginResponse login(LoginRequest request) throws BadRequestException;
}
