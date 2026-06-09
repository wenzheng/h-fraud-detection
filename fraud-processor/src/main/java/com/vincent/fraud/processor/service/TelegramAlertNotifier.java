package com.vincent.fraud.processor.service;

import com.vincent.fraud.processor.config.TelegramProperties;
import com.vincent.fraud.shared.model.AlertEvent;
import com.vincent.fraud.shared.model.TransactionEvent;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

@Component
public class TelegramAlertNotifier implements AlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(TelegramAlertNotifier.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final TelegramProperties properties;
    private final RestClient restClient;

    public TelegramAlertNotifier(TelegramProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.resolvedBaseUrl()).build();
    }

    @Override
    public void notify(AlertEvent alertEvent, TransactionEvent transactionEvent) {
        if (!properties.enabled()) {
            return;
        }
        if (!properties.isConfigured()) {
            log.warn("Telegram alerting is enabled but botToken/chatId is missing");
            return;
        }

        TelegramSendMessageRequest request = new TelegramSendMessageRequest(
                properties.chatId(),
                buildMessage(alertEvent, transactionEvent),
                "HTML",
                properties.disableNotification()
        );

        restClient.post()
                .uri(URI.create("/bot" + properties.botToken() + "/sendMessage"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private String buildMessage(AlertEvent alertEvent, TransactionEvent transactionEvent) {
        return "<b>Fraud alert detected</b>\n"
                + "<b>Severity:</b> " + escape(alertEvent.severity().name()) + "\n"
                + "<b>Transaction ID:</b> " + escape(transactionEvent.transactionId()) + "\n"
                + "<b>Account ID:</b> " + escape(transactionEvent.accountId()) + "\n"
                + "<b>Merchant ID:</b> " + escape(transactionEvent.merchantId()) + "\n"
                + "<b>Amount:</b> " + escape(transactionEvent.amount().toPlainString() + " " + transactionEvent.currency()) + "\n"
                + "<b>Occurred At:</b> "
                + escape(TIMESTAMP_FORMATTER.format(transactionEvent.occurredAt().atOffset(ZoneOffset.UTC))) + "\n"
                + "<b>Reasons:</b>\n" + reasonsAsHtml(alertEvent.reasons());
    }

    private String reasonsAsHtml(List<String> reasons) {
        return reasons.stream()
                .map(reason -> "- " + escape(reason))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("- No reasons provided");
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value);
    }

    record TelegramSendMessageRequest(
            String chat_id,
            String text,
            String parse_mode,
            boolean disable_notification
    ) {
    }
}
