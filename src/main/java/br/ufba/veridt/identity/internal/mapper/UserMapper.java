package br.ufba.veridt.identity.internal.mapper;

import org.springframework.stereotype.Component;

import br.ufba.veridt.identity.internal.domain.User;
import br.ufba.veridt.identity.internal.dto.RegisterUserRequest;
import br.ufba.veridt.identity.internal.dto.UserResponse;

@Component
public class UserMapper {

	/** Recebe o hash já calculado: a senha em texto puro nunca chega à entidade. */
	public User toEntity(RegisterUserRequest request, String passwordHash) {
		return new User(request.fullName(), request.email(), passwordHash, request.cpf(), request.type(),
				request.oabNumber(), request.oabState());
	}

	public UserResponse toResponse(User user) {
		return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getType(), user.getOabNumber(),
				user.getOabState(), user.getCreatedAt());
	}

}
