package br.ufba.veridt.identity.internal.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

/**
 * Nega tudo por padrão e libera só o health check (ID-01). Login e logout usam as rotas padrão do Spring Security
 * ({@code POST /login} com {@code email} e {@code senha}, {@code POST /logout}), sem página nem redirecionamento:
 * o formato final e o CSRF para o frontend ficam para o ID-06 (P1).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IdentityProperties.class)
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorWriter errors) throws Exception {
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
			.anyRequest().authenticated())
			.formLogin(form -> form
				.loginPage("/login")
				.usernameParameter("email")
				.passwordParameter("senha")
				.successHandler((request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value()))
				// Mesma resposta para e-mail inexistente e senha errada: não revela se o e-mail está cadastrado.
				.failureHandler((request, response, exception) -> errors.write(response, HttpStatus.UNAUTHORIZED,
						"CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos.")))
			.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
			.exceptionHandling(handling -> handling
				.authenticationEntryPoint((request, response, exception) -> errors.write(response,
						HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Autenticação necessária."))
				.accessDeniedHandler((request, response, exception) -> errors.write(response, HttpStatus.FORBIDDEN,
						"ACESSO_NEGADO", "Acesso negado.")));
		return http.build();
	}

}
