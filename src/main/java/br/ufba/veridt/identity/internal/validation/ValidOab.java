package br.ufba.veridt.identity.internal.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** OAB (número de até 6 dígitos e UF) obrigatória para advogado e ausente para os demais tipos. */
@Documented
@Constraint(validatedBy = OabValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOab {

	String message() default "OAB inválida para o tipo de usuário";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
