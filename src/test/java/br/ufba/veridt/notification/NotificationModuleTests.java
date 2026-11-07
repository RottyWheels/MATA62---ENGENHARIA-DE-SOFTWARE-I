package br.ufba.veridt.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.transaction.annotation.Transactional;

import br.ufba.veridt.TestcontainersConfiguration;

@ApplicationModuleTest
@Import({ TestcontainersConfiguration.class, NotificationModuleTests.TestBeans.class })
class NotificationModuleTests {

	private static final Duration ESPERA = Duration.ofSeconds(10);

	@Autowired
	private TestPublisher publisher;

	@Autowired
	private RecordingEmailSender sender;

	@Autowired
	private IncompleteEventPublications incompletePublications;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void limpar() {
		sender.reset();
		jdbc.update("DELETE FROM event_publication");
	}

	@Test
	void eventoPublicadoGeraEnvio() {
		publisher.publish(pedido("ana@exemplo.com"));

		await().atMost(ESPERA).until(() -> sender.sent().size() == 1);
		RecordingEmailSender.SentEmail enviado = sender.sent().getFirst();
		assertThat(enviado.recipient()).isEqualTo("ana@exemplo.com");
		assertThat(enviado.email().subject()).isEqualTo("Compra de créditos confirmada");
		await().atMost(ESPERA).until(() -> publicacoesPendentes() == 0);
	}

	@Test
	void falhaNoEnvioNaoDesfazTransacaoDeQuemPublicou() {
		sender.failing(true);

		publisher.publish(pedido("ana@exemplo.com"));

		// A publicação é gravada na transação de quem publicou: se ela existe e segue pendente, o commit aconteceu
		// e a falha do envio, posterior, não o desfez.
		await().atMost(ESPERA).until(() -> publicacoesComFalha() == 1);
		assertThat(publicacoesPendentes()).isEqualTo(1);
		assertThat(sender.sent()).isEmpty();
	}

	@Test
	void eventoComFalhaFicaPendenteEEReenviado() {
		sender.failing(true);
		publisher.publish(pedido("ana@exemplo.com"));
		await().atMost(ESPERA).until(() -> publicacoesComFalha() == 1);
		assertThat(publicacoesPendentes()).isEqualTo(1);

		sender.failing(false);
		// Mesmo mecanismo usado no reinício (republish-outstanding-events-on-restart).
		incompletePublications.resubmitIncompletePublications(publication -> true);

		await().atMost(ESPERA).until(() -> sender.sent().size() == 1);
		await().atMost(ESPERA).until(() -> publicacoesPendentes() == 0);
	}

	@Test
	void rollbackDeQuemPublicouNaoEnviaEmail() {
		assertThatThrownBy(() -> publisher.publishAndFail(pedido("ana@exemplo.com")))
			.isInstanceOf(IllegalStateException.class);

		await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3)).until(() -> sender.sent().isEmpty());
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM event_publication", Integer.class)).isZero();
	}

	@Test
	void eventoInvalidoNaoEEnviado() {
		publisher.publish(pedido("nao-e-um-email"));

		await().atMost(ESPERA).until(() -> publicacoesComFalha() == 1);
		assertThat(sender.sent()).isEmpty();
		assertThat(publicacoesPendentes()).isEqualTo(1);
	}

	private int publicacoesPendentes() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM event_publication WHERE completion_date IS NULL",
				Integer.class);
	}

	/** Espera pelo status FAILED: a tentativa passa antes por PROCESSING, e reenviar nesse momento não tem efeito. */
	private int publicacoesComFalha() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM event_publication WHERE status = 'FAILED'", Integer.class);
	}

	private static EmailRequested pedido(String recipient) {
		return new EmailRequested(recipient, EmailType.COMPRA_CONFIRMADA,
				Map.of("nome", "Ana", "pacote", "Premium", "creditos", "50", "valor", "R$ 250,00"));
	}

	/** Simula outro módulo publicando o evento dentro da própria transação. */
	static class TestPublisher {

		private final ApplicationEventPublisher events;

		TestPublisher(ApplicationEventPublisher events) {
			this.events = events;
		}

		@Transactional
		public void publish(EmailRequested event) {
			events.publishEvent(event);
		}

		@Transactional
		public void publishAndFail(EmailRequested event) {
			events.publishEvent(event);
			throw new IllegalStateException("Falha na operação de quem publicou");
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestBeans {

		@Bean
		@Primary
		RecordingEmailSender recordingEmailSender() {
			return new RecordingEmailSender();
		}

		@Bean
		TestPublisher testPublisher(ApplicationEventPublisher events) {
			return new TestPublisher(events);
		}

	}

}
