package br.ufba.veridt.identity.internal.validation;

import java.util.regex.Pattern;

import br.ufba.veridt.identity.internal.domain.UserType;
import br.ufba.veridt.identity.internal.dto.RegisterUserRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OabValidator implements ConstraintValidator<ValidOab, RegisterUserRequest> {

	private static final Pattern NUMBER = Pattern.compile("\\d{1,6}");

	@Override
	public boolean isValid(RegisterUserRequest request, ConstraintValidatorContext context) {
		if (request == null || request.type() == null) {
			return true;
		}
		if (request.type() == UserType.ADVOGADO) {
			if (request.oabNumber() == null || !NUMBER.matcher(request.oabNumber()).matches()) {
				return violation(context, "oabNumber", "obrigatório para advogado, com até 6 dígitos");
			}
			if (request.oabState() == null) {
				return violation(context, "oabState", "obrigatório para advogado");
			}
			return true;
		}
		boolean hasOab = (request.oabNumber() != null && !request.oabNumber().isBlank()) || request.oabState() != null;
		return !hasOab || violation(context, "oabNumber", "só pode ser informado por advogado");
	}

	private static boolean violation(ConstraintValidatorContext context, String field, String message) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(message).addPropertyNode(field).addConstraintViolation();
		return false;
	}

}
