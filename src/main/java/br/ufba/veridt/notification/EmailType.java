package br.ufba.veridt.notification;

import java.util.List;
import java.util.Locale;

/** Tipos de e-mail, com assunto fixo e os parâmetros que o modelo exige. */
public enum EmailType {

	RECUPERACAO_SENHA("Recuperação de senha", List.of("nome", "link", "validadeMinutos")),
	COMPRA_CONFIRMADA("Compra de créditos confirmada", List.of("nome", "pacote", "creditos", "valor")),
	GRAVACAO_CONCLUIDA("Gravação concluída", List.of("titulo", "url", "registroId", "concluidoEm")),
	CAPTURA_CONCLUIDA("Captura de tela concluída", List.of("titulo", "url", "registroId", "concluidoEm"));

	private final String subject;
	private final List<String> requiredParameters;

	EmailType(String subject, List<String> requiredParameters) {
		this.subject = subject;
		this.requiredParameters = requiredParameters;
	}

	public String subject() {
		return subject;
	}

	public List<String> requiredParameters() {
		return requiredParameters;
	}

	/** Nome do arquivo de modelo em {@code templates/email}, sem extensão. */
	public String templateName() {
		return name().toLowerCase(Locale.ROOT);
	}

}
