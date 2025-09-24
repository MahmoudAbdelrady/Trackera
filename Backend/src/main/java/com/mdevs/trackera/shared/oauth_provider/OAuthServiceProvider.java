package com.mdevs.trackera.shared.oauth_provider;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.dto.auth.OAuthV2RequestDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.repository.UserOAuthProviderRepository;
import com.mdevs.trackera.repository.UserRepository;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

public abstract class OAuthServiceProvider {
    protected abstract OAuthProvider getOAuthProvider();

    protected abstract String getAuthFlowUrl(User user);

    public abstract OAuthUserInfoDTO authenticate(String code);

    public abstract OAuthUserInfoDTO authenticateV2(OAuthV2RequestDTO authRequest);

    public abstract String refreshAccessToken(String refreshToken);

    public String generateAuthFlowUrl(HttpServletRequest request) {
        return getAuthFlowUrl(validateAndGetAuthFlowUser(request));
    }

    private User validateAndGetAuthFlowUser(HttpServletRequest request) {
        String jwtTokenHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isEmpty(jwtTokenHeader) || !jwtTokenHeader.startsWith("Bearer ")) {
            return null;
        }
        JwtUtil jwtUtil = AppConfig.getApplicationContext().getBean(JwtUtil.class);
        UserRepository userRepository = AppConfig.getApplicationContext().getBean(UserRepository.class);
        UserOAuthProviderRepository userOAuthProviderRepository = AppConfig.getApplicationContext().getBean(UserOAuthProviderRepository.class);

        Claims claims = jwtUtil.validateAndGetTokenPayload(jwtTokenHeader.substring(7), true);
        User user = userRepository.findByEmail(claims.get("email", String.class));
        if (userOAuthProviderRepository.existsByUserAndProvider(user, getOAuthProvider())) {
            throw new BusinessException(getOAuthProvider().getLabel() + " account is already linked to the current account");
        }
        return user;
    }
}
