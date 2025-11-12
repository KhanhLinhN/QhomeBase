package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.config.NotificationProperties;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceBooking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender mailSender;
    private final NotificationProperties notificationProperties;

    public void sendBookingPaymentSuccess(ServiceBooking booking, String txnRef) {
        Set<String> recipients = new HashSet<>();
        addAllSafe(recipients, notificationProperties.getServiceBookingSuccessRecipients());
        addAllSafe(recipients, notificationProperties.getServiceBookingSuccessCc());

        if (CollectionUtils.isEmpty(recipients)) {
            log.info("📧 [Email] No recipients configured for booking payment success notifications");
            return;
        }

        String subject = "[QHome] Thanh toán dịch vụ thành công - " + booking.getService().getName();
        String body = buildBody(booking, txnRef);

        sendEmail(recipients, subject, body);
    }

    private void addAllSafe(Set<String> target, Collection<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                target.add(value.trim());
            }
        }
    }

    private void sendEmail(Collection<String> recipients, String subject, String body) {
        for (String recipient : recipients) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(recipient);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                log.info("📧 [Email] Sent payment email to {}", recipient);
            } catch (MailException ex) {
                log.error("❌ [Email] Failed to send email to {}: {}", recipient, ex.getMessage());
            }
        }
    }

    private String buildBody(ServiceBooking booking, String txnRef) {
        var service = booking.getService();
        String bookingDate = booking.getBookingDate() != null ? booking.getBookingDate().format(DATE_FORMATTER) : "—";
        String startTime = booking.getStartTime() != null ? booking.getStartTime().format(TIME_FORMATTER) : "—";
        String endTime = booking.getEndTime() != null ? booking.getEndTime().format(TIME_FORMATTER) : "—";
        String amount = booking.getTotalAmount() != null ? booking.getTotalAmount().toPlainString() : "0";

        return """
                Xin chào,

                Hệ thống đã ghi nhận thanh toán thành công cho đơn đặt dịch vụ:

                • Dịch vụ: %s
                • Mã đơn: %s
                • Ngày sử dụng: %s
                • Khung giờ: %s - %s
                • Tổng tiền: %s VND
                • Mã giao dịch VNPAY: %s

                Trân trọng,
                QHome Resident
                """.formatted(
                service != null ? service.getName() : "Dịch vụ",
                booking.getId(),
                bookingDate,
                startTime,
                endTime,
                amount,
                txnRef != null ? txnRef : "N/A"
        );
    }
}

