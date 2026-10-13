package com.sagant.leads.leads_api.evaluacion.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * El valor debe ser un entero, sin importar cómo esté escrito: {@code 20} y {@code 20.0} son
 * válidos; {@code 20.5} no. {@code @Digits(fraction = 0)} no sirve porque rechaza {@code 20.0}.
 * Un valor null se considera válido (lo controla {@code @NotNull}).
 */
@Documented
@Constraint(validatedBy = Entero.Validador.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface Entero {

	String message() default "debe ser un número entero";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validador implements ConstraintValidator<Entero, BigDecimal> {

		@Override
		public boolean isValid(BigDecimal valor, ConstraintValidatorContext context) {
			return valor == null || valor.stripTrailingZeros().scale() <= 0;
		}
	}
}
