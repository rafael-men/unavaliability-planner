package com.unavaliability.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    JavaMailSender mailSender;

    private EmailService service;

    @BeforeEach
    void setUp() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        service = new EmailService(mailSenderProvider, "indisponibilidade@luneekirie.resend.app");
    }

    @Test
    void sendsMessageThroughResendSmtp() {
        service.send(
                "destinatario@example.com",
                "Sua senha foi redefinida",
                "Sua nova senha e: senha123");

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("indisponibilidade@luneekirie.resend.app");
        assertThat(message.getTo()).containsExactly("destinatario@example.com");
        assertThat(message.getSubject()).isEqualTo("Sua senha foi redefinida");
        assertThat(message.getText()).isEqualTo("Sua nova senha e: senha123");
    }
}
