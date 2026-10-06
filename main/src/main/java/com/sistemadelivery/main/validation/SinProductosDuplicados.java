package com.sistemadelivery.main.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricción de clase: impide que un pedido repita el mismo producto
 * en dos líneas distintas (las cantidades deben consolidarse en una sola).
 */
@Target({ElementType.TYPE, ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SinProductosDuplicadosValidator.class)
public @interface SinProductosDuplicados {

    String message() default "El pedido contiene el mismo producto más de una vez; consolide las cantidades en una sola línea";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
