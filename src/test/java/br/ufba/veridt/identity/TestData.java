package br.ufba.veridt.identity;

import br.ufba.veridt.identity.internal.domain.Uf;
import br.ufba.veridt.identity.internal.domain.UserType;
import br.ufba.veridt.identity.internal.dto.RegisterUserRequest;

/** Dados válidos para testes. Os CPFs são gerados com os dígitos verificadores corretos. */
final class TestData {

	static final String SENHA = "Senha#Forte1";

	private TestData() {
	}

	static String cpf(String nineDigits) {
		String digits = nineDigits + checkDigit(nineDigits, 9);
		return digits + checkDigit(digits, 10);
	}

	static RegisterUserRequest comum(String email, String cpf) {
		return new RegisterUserRequest("Ana Souza", email, SENHA, cpf, UserType.COMUM, null, null);
	}

	static RegisterUserRequest advogado(String email, String cpf, String oabNumber, Uf oabState) {
		return new RegisterUserRequest("Bruno Lima", email, SENHA, cpf, UserType.ADVOGADO, oabNumber, oabState);
	}

	private static int checkDigit(String digits, int length) {
		int sum = 0;
		for (int i = 0; i < length; i++) {
			sum += (digits.charAt(i) - '0') * (length + 1 - i);
		}
		int rest = (sum * 10) % 11;
		return rest == 10 ? 0 : rest;
	}

}
