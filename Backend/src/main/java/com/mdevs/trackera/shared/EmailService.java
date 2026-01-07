package com.mdevs.trackera.shared;

import com.mdevs.trackera.dto.email.EmailRequest;
import com.mdevs.trackera.utils.TrackeraMailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final SpringTemplateEngine templateEngine;

    private final TrackeraMailSender mailSender;

    public void send(EmailRequest request) {
        Context context = new Context();
        request.getParameters().forEach(context::setVariable);

        String processedTemplate = templateEngine.process(request.getTemplateName(), context);
        mailSender.sendEmail(request.getTargetEmail(), request.getSubject(), processedTemplate, request.isHtml());
    }
}