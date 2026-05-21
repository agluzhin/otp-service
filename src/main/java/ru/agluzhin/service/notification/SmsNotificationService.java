package ru.agluzhin.service.notification;

import org.jsmpp.bean.*;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class SmsNotificationService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddress;

    public SmsNotificationService() {
        Properties cfg = loadConfig();
        this.host = cfg.getProperty("smpp.host");
        this.port = Integer.parseInt(cfg.getProperty("smpp.port"));
        this.systemId = cfg.getProperty("smpp.system_id");
        this.password = cfg.getProperty("smpp.password");
        this.systemType = cfg.getProperty("smpp.system_type");
        this.sourceAddress = cfg.getProperty("smpp.source_addr");
    }

    @Override
    public void send(String destination, String code) {
        SMPPSession session = new SMPPSession();
        try {
            BindParameter bindParam = new BindParameter(
                    BindType.BIND_TX,
                    systemId, password, systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress
            );
            session.connectAndBind(host, port, bindParam);

            session.submitShortMessage(
                    systemType,
                    TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, sourceAddress,
                    TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, destination,
                    new ESMClass(), (byte) 0, (byte) 1, null, null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                    (byte) 0,
                    ("Your OTP code: " + code).getBytes(StandardCharsets.UTF_8)
            );

            log.info("OTP sent via SMS to {}", destination);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", destination, e.getMessage(), e);
            throw new RuntimeException("SMS delivery failed", e);
        } finally {
            session.unbindAndClose();
        }
    }

    private Properties loadConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sms.properties")) {
            if (is == null) throw new IllegalStateException("sms.properties not found");
            Properties props = new Properties();
            props.load(is);
            return props;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load sms.properties", e);
        }
    }

}
