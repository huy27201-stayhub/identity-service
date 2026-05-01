package com.stayhub.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

  @Email @NotBlank @Size(max = 255) private String email;

  @NotBlank @Size(min = 8, max = 64) private String password;

  @NotBlank @Size(min = 8, max = 64) private String confirmPassword;

  @NotBlank @Size(max = 50) private String firstName;

  @NotBlank @Size(max = 50) private String lastName;

  @NotBlank private String phoneNumber;

  @NotBlank private String regionCode;
}
