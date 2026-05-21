package ru.agluzhin.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.agluzhin.exception.InvalidOtpException;
import ru.agluzhin.service.OtpService;
import ru.agluzhin.service.notification.NotificationChannelType;

import java.io.IOException;
import java.util.Map;

/**
 * Handles user OTP endpoints (protected by JwtFilter with any authenticated role):
 * POST /otp/generate   — generate and deliver an OTP code
 * POST /otp/validate   — validate a submitted OTP code
 */
public class OtpHandler extends BaseHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(OtpHandler.class);

    private final OtpService otpService;

    public OtpHandler(OtpService otpService) {
        this.otpService = otpService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        log.info("→ {} {} userId={}", method, path, getUserId(exchange));

        try {
            if ("POST".equalsIgnoreCase(method) && path.endsWith("/generate")) {
                handleGenerate(exchange);
            } else if ("POST".equalsIgnoreCase(method) && path.endsWith("/validate")) {
                handleValidate(exchange);
            } else {
                sendError(exchange, 404, "Not found");
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in OtpHandler", e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleGenerate(HttpExchange exchange) throws IOException {
        Map<String, Object> body = parseJson(readBody(exchange));

        String operationId = requireString(body, "operationId");
        String channelStr = requireString(body, "channel");
        String destination = requireString(body, "destination");

        NotificationChannelType channelType;
        try {
            channelType = NotificationChannelType.valueOf(channelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, "Channel must be one of: EMAIL, SMS, TELEGRAM, FILE");
            return;
        }

        long userId = getUserId(exchange);
        otpService.generate(userId, operationId, channelType, destination);

        log.info("← 200 OTP generated userId={} operationId={} channel={}", userId, operationId, channelType);
        sendOk(exchange, "OTP code sent successfully");
    }

    private void handleValidate(HttpExchange exchange) throws IOException {
        Map<String, Object> body = parseJson(readBody(exchange));

        String operationId = requireString(body, "operationId");
        String submittedCode = requireString(body, "code");

        long userId = getUserId(exchange);

        try {
            otpService.validate(userId, operationId, submittedCode);
            log.info("← 200 OTP validated userId={} operationId={}", userId, operationId);
            sendOk(exchange, "OTP code is valid");
        } catch (InvalidOtpException e) {
            log.warn("← 400 OTP invalid userId={} reason={}", userId, e.getMessage());
            sendError(exchange, 400, e.getMessage());
        }
    }

}
