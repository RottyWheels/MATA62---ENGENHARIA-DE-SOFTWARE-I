package br.ufba.veridt.identity.internal.dto;

import java.time.Instant;
import java.util.UUID;

import br.ufba.veridt.identity.internal.domain.Uf;
import br.ufba.veridt.identity.internal.domain.UserType;

/** Dados públicos do usuário. Nunca inclui senha, hash ou CPF. */
public record UserResponse(UUID id, String fullName, String email, UserType type, String oabNumber, Uf oabState,
		Instant createdAt) {
}
