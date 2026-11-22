package com.lab.projetosanquim.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class MaxBytesValidator implements ConstraintValidator<MaxBytes, CharSequence> {
    private int limite;

    @Override
    public void initialize(MaxBytes constraint) {
        limite = constraint.value();
    }

    @Override
    public boolean isValid(CharSequence valor, ConstraintValidatorContext context) {
        return cabeEmUtf8(valor, limite);
    }

    public static boolean cabeEmUtf8(CharSequence valor, int limite) {
        return valor == null || valor.toString().getBytes(StandardCharsets.UTF_8).length <= limite;
    }
}
