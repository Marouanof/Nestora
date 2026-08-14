package com.example.notificationservice.messaging;

import com.example.notificationservice.client.BookingClient;
import com.example.notificationservice.client.PropertyClient;
import com.example.notificationservice.client.UserClient;
import com.example.notificationservice.config.RabbitConfig;
import com.example.notificationservice.dto.*;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
public class NotificationListener {

    private final EmailService emailService;
    private final UserClient userClient;
    private final BookingClient bookingClient;
    private final PropertyClient propertyClient;
    private final NotificationRepository notificationRepository;

    @org.springframework.amqp.rabbit.annotation.RabbitHandler
    public void handlePaymentVerification(BookingPaymentEvent event, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        if ("PAYMENT_SUCCESS".equals(routingKey)) {
            processPaymentSuccess(event);
        } else if ("PAYMENT_FAILED".equals(routingKey)) {
            processPaymentFailed(event);
        } else if ("PAYMENT_COMPLETED".equals(routingKey)) {
            processPaymentCompleted(event);
        } else {
            log.warn("Received unknown routing key for payment verification: {}", routingKey);
        }
    }

    private void processPaymentCompleted(BookingPaymentEvent event) {
        log.info("Handling PAYMENT_COMPLETED for booking: {}", event.getBookingId());
        String tenantMsg = "Votre séjour pour la réservation #" + event.getBookingId() + " est maintenant terminé. Merci de votre confiance !";
        String ownerMsg = "Le séjour pour la réservation #" + event.getBookingId() + " est terminé. Les fonds ont été libérés vers votre compte.";
        
        try {
            BookingDTO booking = bookingClient.getBookingById(event.getBookingId());
            if (booking != null) {
                // Notif Tenant
                notifyUser(booking.getTenantId(), tenantMsg, "Séjour Terminé", "PAYMENT_COMPLETED");
                // Notif Owner
                notifyUser(booking.getOwnerId(), ownerMsg, "Revenu Libéré", "PAYMENT_COMPLETED");
            }
        } catch (Exception e) {
            log.error("Error processing PAYMENT_COMPLETED for booking {}: {}", event.getBookingId(), e.getMessage());
        }
    }

    private void processPaymentSuccess(BookingPaymentEvent event) {
        log.info("Handling PAYMENT_SUCCESS for booking: {}", event.getBookingId());
        String tenantMsg = "Votre paiement pour la réservation #" + event.getBookingId() + " a été validé !";
        String ownerMsg = "Paiement validé pour la réservation #" + event.getBookingId() + ". Votre revenu est sécurisé.";

        try {
            BookingDTO booking = bookingClient.getBookingById(event.getBookingId());
            if (booking != null) {
                // Notif Tenant
                notifyUser(booking.getTenantId(), tenantMsg, "Paiement Confirmé", "PAYMENT_SUCCESS");
                // Notif Owner
                notifyUser(booking.getOwnerId(), ownerMsg, "Nouvelle Réservation Confirmée", "PAYMENT_SUCCESS");
            }
        } catch (Exception e) {
            log.error("Error processing PAYMENT_SUCCESS for booking {}: {}", event.getBookingId(), e.getMessage());
        }
    }

    private void processPaymentFailed(BookingPaymentEvent event) {
        log.warn("Handling PAYMENT_FAILED for booking: {} | Reason: {}", event.getBookingId(), event.getReason());
        String tenantMsg = "Le paiement pour votre réservation #" + event.getBookingId() + " a échoué. Raison : " + event.getReason();
        String ownerMsg = "Une tentative de réservation a échoué pour votre propriété (Booking #" + event.getBookingId() + "). Le bien reste disponible.";

        try {
            BookingDTO booking = bookingClient.getBookingById(event.getBookingId());
            if (booking != null) {
                // Notif Tenant
                notifyUser(booking.getTenantId(), tenantMsg, "Échec du Paiement", "PAYMENT_FAILED");
                // Notif Owner
                notifyUser(booking.getOwnerId(), ownerMsg, "Tentative de Réservation Échouée", "PAYMENT_FAILED");
            }
        } catch (Exception e) {
            log.error("Error processing PAYMENT_FAILED for booking {}: {}", event.getBookingId(), e.getMessage());
        }
    }

    @org.springframework.amqp.rabbit.annotation.RabbitHandler
    public void handlePropertyApproval(PropertyApprovalEvent event) {
        log.info("Handling PROPERTY_APPROVAL for property: {} | Status: {}", event.getPropertyId(), event.getStatus());
        String msg;
        String subject;
        if ("ACTIVE".equals(event.getStatus())) {
            msg = "Félicitations ! Votre propriété #" + event.getPropertyId() + " a été approuvée par l'administrateur et est maintenant en ligne.";
            subject = "Propriété Approuvée";
        } else {
            msg = "Votre propriété #" + event.getPropertyId() + " a été refusée par l'administrateur. Raison : " + event.getReason();
            subject = "Propriété Refusée";
        }

        try {
            PropertyDTO property = propertyClient.getPropertyById(event.getPropertyId());
            if (property != null && property.getOwnerId() != null) {
                notifyUser(property.getOwnerId(), msg, subject, "PROPERTY_STATUS");
            }
        } catch (Exception e) {
            log.error("Error processing PROPERTY_APPROVAL: {}", e.getMessage());
        }
    }

    @org.springframework.amqp.rabbit.annotation.RabbitHandler
    public void handleBookingCancelled(BookingCancellationEvent event) {
        log.info("Handling BOOKING_CANCELLED for booking: {}", event.getBookingId());
        String tenantMsg = "Votre réservation #" + event.getBookingId() + " a été annulée. Raison : " + event.getReason();
        String ownerMsg = "La réservation #" + event.getBookingId() + " pour votre propriété a été annulée. Raison : " + event.getReason();

        try {
            BookingDTO booking = bookingClient.getBookingById(event.getBookingId());
            if (booking != null) {
                notifyUser(booking.getTenantId(), tenantMsg, "Réservation Annulée", "BOOKING_CANCELLED");
                notifyUser(booking.getOwnerId(), ownerMsg, "Réservation Annulée", "BOOKING_CANCELLED");
            }
        } catch (Exception e) {
            log.error("Error processing BOOKING_CANCELLED: {}", e.getMessage());
        }
    }

    private void notifyUser(Long userId, String message, String emailSubject, String type) {
        if (userId == null) return;
        
        // 1. In-App Notification
        saveInAppNotification(userId, message, type);
        
        // 2. Email Notification
        try {
            UserProfileDTO user = userClient.getUserById(userId);
            if (user != null && user.getEmail() != null) {
                emailService.sendEmail(user.getEmail(), emailSubject, 
                    "Bonjour " + user.getFirstName() + ",\n\n" + message);
            }
        } catch (Exception e) {
            log.error("Failed to send email to user {}: {}", userId, e.getMessage());
        }
    }

    private void saveInAppNotification(Long userId, String message, String type) {
        try {
            Notification notification = Notification.builder()
                    .userId(userId)
                    .message(message)
                    .type(type)
                    .build();
            notificationRepository.save(notification);
            log.info("In-App notification saved for user {}: {}", userId, type);
        } catch (Exception e) {
            log.error("Failed to save In-App notification: {}", e.getMessage());
        }
    }
}
