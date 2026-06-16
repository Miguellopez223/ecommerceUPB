package com.upb.ecommerce.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Envío de correos electrónicos. Usa el {@link JavaMailSender} autoconfigurado por
 * Spring Boot a partir de las propiedades {@code spring.mail.*} de application.properties.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    /** Remitente (normalmente igual al usuario SMTP). */
    @Value("${spring.mail.username:}")
    private String remitente;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Recordatorio al cliente de que dejó productos en su carrito sin completar la compra. */
    public void enviarRecordatorioCarrito(String destino, String nombreCliente,
                                          int cantidadItems, BigDecimal totalEstimado) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        if (remitente != null && !remitente.isBlank()) {
            mensaje.setFrom(remitente);
        }
        mensaje.setTo(destino);
        mensaje.setSubject("¡Te quedaron productos en tu carrito!");
        mensaje.setText(
                "Hola " + nombreCliente + ",\n\n" +
                "Notamos que dejaste " + cantidadItems + " producto(s) en tu carrito " +
                "por un total estimado de " + totalEstimado + " sin completar la compra.\n\n" +
                "¡Todavía estás a tiempo! Vuelve a la tienda y finaliza tu pedido antes de que se agoten.\n\n" +
                "Gracias por preferirnos.");
        mailSender.send(mensaje);
    }
}
