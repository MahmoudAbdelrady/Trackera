package com.mdevs.trackera.shared.utils.mail;

import com.mdevs.trackera.config.AppConfig;
import lombok.Builder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Builder
public class TrackeraEmailTarget {
    private String targetEmail;

    private String subject;

    private String templateName;

    private Map<String, String> parameters;

    @Builder.Default
    private boolean isHtml = true;

    public void send() {
        Context thymeleafContext = new Context();
        parameters.forEach(thymeleafContext::setVariable);
        String processedTemplate = AppConfig.getApplicationContext().getBean(SpringTemplateEngine.class).process(templateName, thymeleafContext);
        AppConfig.getApplicationContext().getBean(TrackeraMailSender.class).sendEmail(targetEmail, subject, processedTemplate, isHtml);
    }
}
