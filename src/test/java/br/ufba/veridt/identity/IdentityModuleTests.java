package br.ufba.veridt.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import br.ufba.veridt.TestcontainersConfiguration;
import br.ufba.veridt.identity.internal.domain.PasswordResetToken;
import br.ufba.veridt.identity.internal.domain.Uf;
import br.ufba.veridt.identity.internal.domain.User;
import br.ufba.veridt.identity.internal.dto.ResetPasswordRequest;
import br.ufba.veridt.identity.internal.dto.UserResponse;
import br.ufba.veridt.identity.internal.repository.PasswordResetTokenRepository;
import br.ufba.veridt.identity.internal.repository.UserRepository;
import br.ufba.veridt.identity.internal.service.PasswordResetService;
import br.ufba.veridt.identity.internal.service.RegistrationService;
import br.ufba.veridt.notification.EmailRequested;
import br.ufba.veridt.notification.EmailType;
import br.ufba.veridt.shared.error.exception.BusinessException;
import br.ufba.veridt.shared.error.exception.ConflictException;
import jakarta.validation.ConstraintViolationException;

@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class IdentityModuleTests {

	private static final String CPF_ANA = TestData.cpf("529982247");
	private static final String CPF_BRUNO = TestData.cpf("111444777");

	@Autowired
	private RegistrationService registration;

	@Autowired
	private PasswordResetService passwordReset;

	@Autowired
	private UserRepository users;

	@Autowired
	private PasswordResetTokenRepository tokens;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private FindByIndexNameSessionRepository<? extends Session> sessions;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void limpar() {
		jdbc.update("DELETE FROM spring_session");
		jdbc.update("DELETE FROM token_recuperacao_senha");
		jdbc.update("DELETE FROM usuario");
	}

	// ID-01 e ID-03: cadastro

	@Test
	void cadastroGravaSenhaSoComoHashArgon2id() {
		UserResponse response = registration.register(TestData.comum("ana@exemplo.com", CPF_ANA));

		User user = users.findById(response.id()).orElseThrow();
		assertThat(user.getPasswordHash()).startsWith("$argon2id$").doesNotContain(TestData.SENHA);
		assertThat(passwordEncoder.matches(TestData.SENHA, user.getPasswordHash())).isTrue();
	}

	@Test
	void cadastroNormalizaEmailECpf() {
		String cpfComMascara = CPF_ANA.substring(0, 3) + "." + CPF_ANA.substring(3, 6) + "." + CPF_ANA.substring(6, 9)
				+ "-" + CPF_ANA.substring(9);

		UserResponse response = registration.register(TestData.comum("Ana@Exemplo.COM", cpfComMascara));

		User user = users.findById(response.id()).orElseThrow();
		assertThat(user.getEmail()).isEqualTo("ana@exemplo.com");
		assertThat(user.getCpf()).isEqualTo(CPF_ANA);
		assertThat(response.email()).isEqualTo("ana@exemplo.com");
	}

	@Test
	void emailRepetidoERecusadoMesmoComOutraCaixa() {
		registration.register(TestData.comum("ana@exemplo.com", CPF_ANA));

		assertThatThrownBy(() -> registration.register(TestData.comum("ANA@exemplo.com", CPF_BRUNO)))
			.isInstanceOf(ConflictException.class)
			.extracting(ex -> ((ConflictException) ex).getCode())
			.isEqualTo("EMAIL_JA_CADASTRADO");
	}

	@Test
	void cpfRepetidoERecusado() {
		registration.register(TestData.comum("ana@exemplo.com", CPF_ANA));

		assertThatThrownBy(() -> registration.register(TestData.comum("outra@exemplo.com", CPF_ANA)))
			.isInstanceOf(ConflictException.class)
			.extracting(ex -> ((ConflictException) ex).getCode())
			.isEqualTo("CPF_JA_CADASTRADO");
	}

	@Test
	void cpfInvalidoERecusado() {
		assertThatThrownBy(() -> registration.register(TestData.comum("ana@exemplo.com", "529.982.247-26")))
			.isInstanceOf(ConstraintViolationException.class)
			.hasMessageContaining("cpf");
		assertThat(users.count()).isZero();
	}

	@Test
	void senhaFracaERecusada() {
		var request = new br.ufba.veridt.identity.internal.dto.RegisterUserRequest("Ana Souza", "ana@exemplo.com",
				"senhafraca", CPF_ANA, br.ufba.veridt.identity.internal.domain.UserType.COMUM, null, null);

		assertThatThrownBy(() -> registration.register(request)).isInstanceOf(ConstraintViolationException.class)
			.hasMessageContaining("letra, número e símbolo");
	}

	@Test
	void advogadoExigeOab() {
		assertThatThrownBy(() -> registration.register(TestData.advogado("bruno@exemplo.com", CPF_BRUNO, null, null)))
			.isInstanceOf(ConstraintViolationException.class)
			.hasMessageContaining("oabNumber");
		assertThatThrownBy(
				() -> registration.register(TestData.advogado("bruno@exemplo.com", CPF_BRUNO, "1234567", Uf.BA)))
			.isInstanceOf(ConstraintViolationException.class);
		assertThatThrownBy(() -> registration.register(TestData.advogado("bruno@exemplo.com", CPF_BRUNO, "12345", null)))
			.isInstanceOf(ConstraintViolationException.class)
			.hasMessageContaining("oabState");
	}

	@Test
	void advogadoComOabValidaECadastrado() {
		UserResponse response = registration.register(TestData.advogado("bruno@exemplo.com", CPF_BRUNO, "12345", Uf.BA));

		assertThat(response.oabNumber()).isEqualTo("12345");
		assertThat(response.oabState()).isEqualTo(Uf.BA);
	}

	@Test
	void usuarioComumNaoPodeInformarOab() {
		var request = new br.ufba.veridt.identity.internal.dto.RegisterUserRequest("Ana Souza", "ana@exemplo.com",
				TestData.SENHA, CPF_ANA, br.ufba.veridt.identity.internal.domain.UserType.COMUM, "12345", Uf.BA);

		assertThatThrownBy(() -> registration.register(request)).isInstanceOf(ConstraintViolationException.class)
			.hasMessageContaining("só pode ser informado por advogado");
	}

	// ID-02: garantias no banco, independentes da aplicação

	@Test
	void bancoRecusaDuplicadosEOabInconsistente() {
		inserir("ana@exemplo.com", CPF_ANA, "COMUM", null, null);

		assertThatThrownBy(() -> inserir("ana@exemplo.com", CPF_BRUNO, "COMUM", null, null))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> inserir("outra@exemplo.com", CPF_ANA, "COMUM", null, null))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> inserir("adv@exemplo.com", CPF_BRUNO, "ADVOGADO", null, null))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> inserir("comum@exemplo.com", CPF_BRUNO, "COMUM", "12345", "BA"))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> inserir("Maiuscula@exemplo.com", CPF_BRUNO, "COMUM", null, null))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	// ID-05: recuperação de senha

	@Test
	void pedidoParaEmailInexistenteNaoFazNada(AssertablePublishedEvents events) {
		passwordReset.requestReset("ninguem@exemplo.com");

		assertThat(events.ofType(EmailRequested.class)).isEmpty();
		assertThat(tokens.count()).isZero();
	}

	@Test
	void pedidoEnviaLinkEGuardaSoOHashDoToken(AssertablePublishedEvents events) {
		registration.register(TestData.comum("ana@exemplo.com", CPF_ANA));

		passwordReset.requestReset("ANA@exemplo.com");

		EmailRequested email = unico(events);
		assertThat(email.recipient()).isEqualTo("ana@exemplo.com");
		assertThat(email.type()).isEqualTo(EmailType.RECUPERACAO_SENHA);
		assertThat(email.parameters()).containsEntry("nome", "Ana Souza").containsEntry("validadeMinutos", "15");
		String token = tokenDoLink(email);
		PasswordResetToken salvo = tokens.findAll().getFirst();
		assertThat(salvo.getTokenHash()).isEqualTo(sha256(token)).isNotEqualTo(token);
		assertThat(salvo.getExpiresAt()).isBetween(Instant.now().plusSeconds(14 * 60), Instant.now().plusSeconds(15 * 60));
	}

	@Test
	void redefinicaoTrocaSenhaEInvalidaOToken(AssertablePublishedEvents events) {
		UserResponse ana = registration.register(TestData.comum("ana@exemplo.com", CPF_ANA));
		passwordReset.requestReset("ana@exemplo.com");
		String token = tokenDoLink(unico(events));

		passwordReset.resetPassword(new ResetPasswordRequest(token, "Nova#Senha2"));

		User user = users.findById(ana.id()).orElseThrow();
		assertThat(passwordEncoder.matches("Nova#Senha2", user.getPasswordHash())).isTrue();
		assertThatThrownBy(() -> passwordReset.resetPassword(new ResetPasswordRequest(token, "Outra#Senha3")))
			.isInstanceOf(BusinessException.class)
			.extracting(ex -> ((BusinessException) ex).getCode())
			.isEqualTo("TOKEN_INVALIDO");
	}

	@Test
	void tokenExpiradoOuInexistenteERecusado() {
		UserResponse ana = registration.register(TestData.comum("ana@exemplo.com", CPF_ANA));
		Instant passado = Instant.now().minusSeconds(3600);
		tokens.save(new PasswordResetToken(ana.id(), sha256("expirado"), passado, passado.plusSeconds(900)));

		assertThatThrownBy(() -> passwordReset.resetPassword(new ResetPasswordRequest("expirado", "Nova#Senha2")))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Link de redefinição de senha inválido ou expirado.");
		assertThatThrownBy(() -> passwordReset.resetPassword(new ResetPasswordRequest("inexistente", "Nova#Senha2")))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Link de redefinição de senha inválido ou expirado.");
	}

	@Test
	void novoPedidoInvalidaOTokenAnterior(AssertablePublishedEvents events) {
		registration.register(TestData.comum("ana@exemplo.com", CPF_ANA));
		passwordReset.requestReset("ana@exemplo.com");
		passwordReset.requestReset("ana@exemplo.com");

		List<String> links = new ArrayList<>();
		events.ofType(EmailRequested.class).forEach(email -> links.add(tokenDoLink(email)));
		assertThat(links).hasSize(2);

		assertThatThrownBy(() -> passwordReset.resetPassword(new ResetPasswordRequest(links.get(0), "Nova#Senha2")))
			.isInstanceOf(BusinessException.class);
		passwordReset.resetPassword(new ResetPasswordRequest(links.get(1), "Nova#Senha2"));
	}

	@Test
	void redefinicaoEncerraAsSessoesDoUsuario(AssertablePublishedEvents events) {
		UserResponse ana = registration.register(TestData.comum("ana@exemplo.com", CPF_ANA));
		criarSessao(ana.id());
		criarSessao(ana.id());
		assertThat(sessions.findByPrincipalName(ana.id().toString())).hasSize(2);
		passwordReset.requestReset("ana@exemplo.com");

		passwordReset.resetPassword(new ResetPasswordRequest(tokenDoLink(unico(events)), "Nova#Senha2"));

		assertThat(sessions.findByPrincipalName(ana.id().toString())).isEmpty();
	}

	@Test
	void novaSenhaFracaERecusada(AssertablePublishedEvents events) {
		registration.register(TestData.comum("ana@exemplo.com", CPF_ANA));
		passwordReset.requestReset("ana@exemplo.com");

		assertThatThrownBy(() -> passwordReset.resetPassword(new ResetPasswordRequest(tokenDoLink(unico(events)), "fraca")))
			.isInstanceOf(ConstraintViolationException.class);
	}

	private void criarSessao(UUID userId) {
		criarSessao(sessions, userId);
	}

	/** Genérico porque o tipo concreto da sessão do Spring Session JDBC não é público. */
	private static <S extends Session> void criarSessao(FindByIndexNameSessionRepository<S> repository, UUID userId) {
		S session = repository.createSession();
		session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, userId.toString());
		repository.save(session);
	}

	private void inserir(String email, String cpf, String tipo, String oabNumero, String oabUf) {
		jdbc.update("INSERT INTO usuario (id, nome_completo, email, senha_hash, cpf, tipo, oab_numero, oab_uf, criado_em) "
				+ "VALUES (?, 'Teste', ?, 'hash', ?, ?, ?, ?, now())", UUID.randomUUID(), email, cpf, tipo, oabNumero,
				oabUf);
	}

	private static EmailRequested unico(AssertablePublishedEvents events) {
		List<EmailRequested> emails = new ArrayList<>();
		events.ofType(EmailRequested.class).forEach(emails::add);
		assertThat(emails).hasSize(1);
		return emails.getFirst();
	}

	private static String tokenDoLink(EmailRequested email) {
		String link = email.parameters().get("link");
		assertThat(link).startsWith("http://localhost:8080/redefinir-senha?token=");
		return link.substring(link.indexOf("token=") + "token=".length());
	}

	private static String sha256(String value) {
		try {
			return HexFormat.of()
				.formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
