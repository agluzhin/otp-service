package ru.agluzhin.scheduler;

import ru.agluzhin.config.AppConfig;
import ru.agluzhin.dao.OtpCodeDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OtpExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OtpExpirationScheduler.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "otp-expiry-scheduler");
        t.setDaemon(true);
        return t;
    });

    private final OtpCodeDao otpCodeDao;
    private final long intervalSeconds;

    public OtpExpirationScheduler() {
        this(new OtpCodeDao(), AppConfig.getINSTANCE().getSchedulerIntervalSeconds());
    }

    public OtpExpirationScheduler(OtpCodeDao otpCodeDao, long intervalSeconds) {
        this.otpCodeDao      = otpCodeDao;
        this.intervalSeconds = intervalSeconds;
    }

    public void start() {
        executor.scheduleAtFixedRate(this::expireOutdatedCodes,
                intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        log.info("OTP expiration scheduler started, interval={}s", intervalSeconds);
    }

    public void stop() {
        executor.shutdownNow();
        log.info("OTP expiration scheduler stopped");
    }

    private void expireOutdatedCodes() {
        try {
            int count = otpCodeDao.expireOutdatedCodes();
            log.debug("Scheduler run complete: {} code(s) expired", count);
        } catch (Exception e) {
            // Never let a scheduler task die silently
            log.error("Error during OTP expiration run", e);
        }
    }

}
