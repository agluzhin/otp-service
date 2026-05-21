package ru.agluzhin;

import ru.agluzhin.config.AppConfig;
import ru.agluzhin.handler.AdminHandler;
import ru.agluzhin.handler.AuthHandler;
import ru.agluzhin.handler.OtpHandler;
import ru.agluzhin.middleware.JwtFilter;
import ru.agluzhin.model.Role;
import ru.agluzhin.scheduler.OtpExpirationScheduler;
import ru.agluzhin.service.AdminService;
import ru.agluzhin.service.AuthService;
import ru.agluzhin.service.OtpService;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.getINSTANCE();

        // --- Instantiate services ---
        AuthService  authService  = new AuthService();
        OtpService   otpService   = new OtpService();
        AdminService adminService = new AdminService();

        // --- Build HTTP server ---
        int port = config.getServerPort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Public endpoints — no JWT required
        server.createContext("/auth", new AuthHandler(authService));

        // User endpoints — any authenticated user
        server.createContext("/otp",
                new JwtFilter(new OtpHandler(otpService), authService, null));

        // Admin endpoints — ADMIN role only
        server.createContext("/admin",
                new JwtFilter(new AdminHandler(adminService), authService, Role.ADMIN));

        // Use a thread pool so requests don't block each other
        server.setExecutor(Executors.newFixedThreadPool(10));

        // --- Start OTP expiration scheduler ---
        OtpExpirationScheduler scheduler = new OtpExpirationScheduler();
        scheduler.start();

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            scheduler.stop();
            server.stop(3);
            log.info("Server stopped");
        }));

        server.start();
        log.info("OTP Service started on port {}", port);
    }

}
