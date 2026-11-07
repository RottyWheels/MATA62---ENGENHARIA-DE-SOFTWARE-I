package br.ufba.veridt.notification.internal.sender;

/** Falha ao montar a mensagem para o servidor de e-mail. */
public class EmailDeliveryException extends RuntimeException {

	public EmailDeliveryException(String message, Throwable cause) {
		super(message, cause);
	}

}
