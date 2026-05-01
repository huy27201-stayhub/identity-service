package com.stayhub.identity.service;

public interface JwtService {
    String generateToken(String email);
}
