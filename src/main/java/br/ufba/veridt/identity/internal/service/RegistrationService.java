package br.ufba.veridt.identity.internal.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import br.ufba.veridt.identity.internal.domain.User;
import br.ufba.veridt.identity.internal.dto.RegisterUserRequest;
import br.ufba.veridt.identity.internal.dto.UserResponse;
import br.ufba.veridt.identity.internal.mapper.UserMapper;
import br.ufba.veridt.identity.internal.repository.UserRepository;
import br.ufba.veridt.shared.error.exception.ConflictException;
import jakarta.validation.Valid;

/** Cadastro de usuários (REQ 01). A senha é gravada só como hash Argon2id. */
@Service
@Validated
public class RegistrationService {

	private final UserRepository users;
	private final UserMapper mapper;
	private final PasswordEncoder passwordEncoder;

	public RegistrationService(UserRepository users, UserMapper mapper, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.mapper = mapper;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UserResponse register(@Valid RegisterUserRequest request) {
		if (users.existsByEmail(User.normalizeEmail(request.email()))) {
			throw new ConflictException("EMAIL_JA_CADASTRADO", "E-mail já cadastrado.");
		}
		if (users.existsByCpf(User.normalizeCpf(request.cpf()))) {
			throw new ConflictException("CPF_JA_CADASTRADO", "CPF já cadastrado.");
		}
		User user = mapper.toEntity(request, passwordEncoder.encode(request.password()));
		try {
			users.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException ex) {
			// Outro cadastro com o mesmo e-mail ou CPF passou pelas checagens ao mesmo tempo.
			throw new ConflictException("CADASTRO_DUPLICADO", "E-mail ou CPF já cadastrado.");
		}
		return mapper.toResponse(user);
	}

}
