package br.ufba.veridt.identity.internal.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import br.ufba.veridt.identity.internal.dto.LoginRequest;
import br.ufba.veridt.identity.internal.exception.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Login e logout (REQ 03 e 04), com sessão guardada no banco pelo Spring Session. */
@Service
public class AuthenticationService {

	private final AuthenticationManager authenticationManager;
	private final SessionAuthenticationStrategy sessionStrategy;
	private final SecurityContextRepository contextRepository;
	private final LogoutHandler logoutHandler;

	public AuthenticationService(AuthenticationManager authenticationManager,
			SessionAuthenticationStrategy sessionStrategy, SecurityContextRepository contextRepository,
			LogoutHandler logoutHandler) {
		this.authenticationManager = authenticationManager;
		this.sessionStrategy = sessionStrategy;
		this.contextRepository = contextRepository;
		this.logoutHandler = logoutHandler;
	}

	public void login(LoginRequest credentials, HttpServletRequest request, HttpServletResponse response) {
		Authentication authentication = authenticate(credentials);
		// Troca o ID da sessão e o token CSRF, impedindo a fixação de sessão.
		sessionStrategy.onAuthentication(authentication, request, response);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		contextRepository.saveContext(context, request, response);
	}

	public void logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
		logoutHandler.logout(request, response, authentication);
	}

	private Authentication authenticate(LoginRequest credentials) {
		try {
			return authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(credentials.email(), credentials.password()));
		}
		catch (AuthenticationException ex) {
			throw new InvalidCredentialsException();
		}
	}

}
