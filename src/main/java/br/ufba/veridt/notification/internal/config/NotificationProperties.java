package br.ufba.veridt.notification.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** @param from remetente dos e-mails, definido por {@code MAIL_FROM} */
@Validated
@ConfigurationProperties("veridt.mail")
public record NotificationProperties(@NotBlank @Email String from) {
}
