package com.poly.taskapi.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PasswordPolicyValidator implements ConstraintValidator<ValidPassword, String> {

  private static final Pattern POLICY = Pattern.compile(
      "^(?=\\S{8,20}$)(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).*$"
  );

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return POLICY.matcher(value).matches();
  }
}
