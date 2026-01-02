package com.mdevs.trackera.config.general;

import com.mdevs.trackera.entity.User;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Properties;

@Configuration
public class AppConfig {
    @Getter
    private static ApplicationContext applicationContext;

    @Value("${trackera.mail.username}")
    private String emailUsername;

    @Value("${trackera.mail.password}")
    private String emailPassword;

    public AppConfig(ApplicationContext applicationContext) {
        AppConfig.applicationContext = applicationContext;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);

        mailSender.setUsername(emailUsername);
        mailSender.setPassword(emailPassword);

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.debug", "true");

        return mailSender;
    }

    public static String getFrontendUrl() {
        return applicationContext.getEnvironment().getProperty("trackera.frontend.url");
    }

    public static User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof User) ? (User) principal : null;
    }

    public static User getAuthenticatedCurrentUser() {
        return Objects.requireNonNull(getCurrentUser(), "Authenticated user not found");
    }

    public static LocalDate getMinQueryableDate() {
        return LocalDate.now().minusYears(1).withDayOfYear(1);
    }

    public static boolean isProductionEnv() {
        return applicationContext.getEnvironment().getProperty("trackera.environment", "dev").equalsIgnoreCase("prod");
    }

    public static int getMaxJobFailures() {
        return Integer.parseInt(Objects.requireNonNull(applicationContext.getEnvironment().getProperty("trackera.job.max-failures")));
    }
}
