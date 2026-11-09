package br.ufba.veridt.notification.internal.cleanup;

import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.ufba.veridt.notification.EmailRequested;

/**
 * Apaga os eventos já enviados de tipos sensíveis, como a recuperação de senha, cujo link carrega o token em texto
 * puro. O histórico dos demais e-mails é mantido.
 */
@Component
public class SensitivePublicationCleanup {

	private final CompletedEventPublications completedPublications;

	public SensitivePublicationCleanup(CompletedEventPublications completedPublications) {
		this.completedPublications = completedPublications;
	}

	@Scheduled(fixedDelayString = "${veridt.mail.sensitive-cleanup-interval}")
	@Transactional
	public void deleteSentSensitiveEmails() {
		completedPublications.deletePublications(
				publication -> publication.getEvent() instanceof EmailRequested email && email.type().sensitive());
	}

}
