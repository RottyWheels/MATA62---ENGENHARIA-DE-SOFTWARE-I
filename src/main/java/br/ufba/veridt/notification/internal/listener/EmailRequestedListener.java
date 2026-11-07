package br.ufba.veridt.notification.internal.listener;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import br.ufba.veridt.notification.EmailRequested;
import br.ufba.veridt.notification.internal.service.EmailService;

/**
 * Porta de entrada do módulo. Roda de forma assíncrona após o commit de quem publicou; se lançar exceção, a publicação
 * fica pendente no registro de eventos e é reenviada no próximo reinício.
 */
@Component
class EmailRequestedListener {

	private final EmailService emailService;

	EmailRequestedListener(EmailService emailService) {
		this.emailService = emailService;
	}

	@ApplicationModuleListener
	void on(EmailRequested event) {
		emailService.send(event);
	}

}
