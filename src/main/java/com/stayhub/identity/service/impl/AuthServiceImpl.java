package com.stayhub.identity.service.impl;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.stayhub.identity.dto.request.LoginRequest;
import com.stayhub.identity.dto.request.RegisterRequest;
import com.stayhub.identity.dto.response.LoginResponse;
import com.stayhub.identity.exception.BadRequestException;
import com.stayhub.identity.exception.UnauthorizedException;
import com.stayhub.identity.model.User;
import com.stayhub.identity.repository.UserRepository;
import com.stayhub.identity.service.AuthService;
import com.stayhub.identity.service.JwtService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  @Override
  public String register(RegisterRequest request) throws BadRequestException {
    validatePasswordConfirmation(request);

    String normalizedPhoneNumber =
        normalizePhoneNumber(request.getPhoneNumber(), request.getRegionCode());

    String normalizedEmail = request.getEmail().trim().toLowerCase();

    validateEmailAndPhoneUniqueness(normalizedEmail, normalizedPhoneNumber);

    User user =
        User.builder()
            .email(normalizedEmail)
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(normalizedPhoneNumber)
            .build();

    handleSaveUser(user);

    return "success";
  }

  private void handleSaveUser(User user) {
    try {
      userRepository.save(user);
    } catch (DataIntegrityViolationException e) {
      String message = e.getMostSpecificCause().getMessage();
      Map<String, String> errors = new HashMap<>();

      if (message.contains("email")) {
        errors.put("email", "Email is already taken");
      } else if (message.contains("phone_number")) {
        errors.put("phoneNumber", "Phone number is already taken");
      }

      throw new BadRequestException("Validation failed", errors);
    }
  }

  @Override
  public LoginResponse login(LoginRequest request) throws BadRequestException {
    User user =
        userRepository
            .findUserByEmail(request.getEmail().trim().toLowerCase())
            .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new UnauthorizedException("Invalid credentials");
    }

    String accessToken = jwtService.generateToken(user.getEmail());
    return LoginResponse.builder().accessToken(accessToken).build();
  }

  private void validateEmailAndPhoneUniqueness(String email, String phoneNumber) {
    Map<String, String> errors = new HashMap<>();

    if (userRepository.existsByEmail(email)) {
      errors.put("email", "Email is already taken");
    }

    if (userRepository.existsByPhoneNumber(phoneNumber)) {
      errors.put("phoneNumber", "Phone number is already taken");
    }

    if (!errors.isEmpty()) {
      throw new BadRequestException("Validation failed", errors);
    }
  }

  private void validatePasswordConfirmation(RegisterRequest request) throws BadRequestException {
    if (!request.getPassword().equals(request.getConfirmPassword())) {
      throw new BadRequestException(
          "Validation failed", Map.of("confirmPassword", "Password does not match"));
    }
  }

  private String normalizePhoneNumber(String rawPhoneNumber, String regionCode)
      throws BadRequestException {
    try {
      Phonenumber.PhoneNumber parsedPhoneNumber = phoneNumberUtil.parse(rawPhoneNumber, regionCode);

      if (!phoneNumberUtil.isValidNumber(parsedPhoneNumber)) {
        throw new BadRequestException(
            "Validation failed", Map.of("phoneNumber", "Invalid phone number"));
      }

      return phoneNumberUtil.format(parsedPhoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
    } catch (NumberParseException e) {
      throw new BadRequestException(
          "Validation failed", Map.of("phoneNumber", "Invalid phone number"));
    }
  }
}
