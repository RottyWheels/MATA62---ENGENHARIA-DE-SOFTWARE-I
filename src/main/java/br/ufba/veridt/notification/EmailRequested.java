package br.ufba.veridt.notification;

import java.util.Map;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Pedido de envio de e-mail. Outros módulos publicam este evento dentro da própria transação; o envio ocorre de forma
 * assíncrona, só após o commit, e uma falha no envio não desfaz a operação de quem publicou.
 *
 * @param recipient endereço de destino
 * @param type tipo do e-mail, que define assunto, modelo e parâmetros obrigatórios
 * @param parameters valores usados no modelo, conforme {@link EmailType#requiredParameters()}
 */
public record EmailRequested(@NotBlank @Email String recipient, @NotNull EmailType type,
		@NotNull Map<String, String> parameters) {

	public EmailRequested {
		parameters = parameters == null ? null : Map.copyOf(parameters);
	}

}
