package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.*;
import com.mdevs.trackera.entity.*;
import com.mdevs.trackera.repository.*;
import com.mdevs.trackera.shared.EmailTemplates;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.exceptions.types.UnauthorizedException;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.shared.oauth_provider.OAuthProviderFactory;
import com.mdevs.trackera.utils.*;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AuthService {
    private final UserRepository userRepository;

    private final UserEmailRepository userEmailRepository;

    private final UserService userService;

    private final AuthenticationManager authenticationManager;

    private final SecurityTokenService securityTokenService;

    private final UserOAuthProviderService userOAuthProviderService;

    private final UserPreferredSettingService userPreferredSettingService;

    private final OAuthProviderFactory oAuthProviderFactory;

    private final SecurityTokenRepository securityTokenRepository;

    private final UserInvalidTokenRepository userInvalidTokenRepository;

    private final UserOAuthProviderRepository userOAuthProviderRepository;

    private final JwtUtil jwtUtil;

    private final CookieFactory cookieFactory;

    private final TrackeraHasher trackeraHasher;

    public AuthService(UserRepository userRepository, UserEmailRepository userEmailRepository, UserService userService, AuthenticationManager authenticationManager, SecurityTokenService securityTokenService,
                       UserOAuthProviderService userOAuthProviderService, UserPreferredSettingService userPreferredSettingService, OAuthProviderFactory oAuthProviderFactory,
                       SecurityTokenRepository securityTokenRepository, UserInvalidTokenRepository userInvalidTokenRepository, UserOAuthProviderRepository userOAuthProviderRepository, JwtUtil jwtUtil,
                       CookieFactory cookieFactory, TrackeraHasher trackeraHasher) {
        this.userRepository = userRepository;
        this.userEmailRepository = userEmailRepository;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.securityTokenService = securityTokenService;
        this.userOAuthProviderService = userOAuthProviderService;
        this.userPreferredSettingService = userPreferredSettingService;
        this.oAuthProviderFactory = oAuthProviderFactory;
        this.securityTokenRepository = securityTokenRepository;
        this.userInvalidTokenRepository = userInvalidTokenRepository;
        this.userOAuthProviderRepository = userOAuthProviderRepository;
        this.jwtUtil = jwtUtil;
        this.cookieFactory = cookieFactory;
        this.trackeraHasher = trackeraHasher;
    }

    @Transactional
    public void signUp(SignUpDTO signUpDTO) {
        User user = userService.create(signUpDTO);
        securityTokenService.createAndSendSecurityToken(user, user.getEmail(), SecurityToken.Type.ACCOUNT_ACTIVATION, null,
                EmailTemplateUtil.accountActivationTemplateParams(), null, EmailTemplates.VERIFICATION_MAIL_TEMPLATE);
    }

    @Transactional
    public Map<String, Object> login(LoginDTO loginDTO, HttpServletResponse httpResponse) {
        Authentication authentication;
        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword(), List.of());
            authentication = authenticationManager.authenticate(authToken);
        } catch (AuthenticationException e) {
            throw new SecurityException("Invalid credentials");
        }

        User loggedUser = (User) authentication.getPrincipal();
        if (!loggedUser.isVerified()) {
            String message;
            if (securityTokenService.hasRecentActivationToken(loggedUser, SecurityToken.Type.ACCOUNT_ACTIVATION)) {
                message = "Account not activated. Please check your email for the activation link.";
            } else {
                securityTokenService.createAndSendSecurityToken(loggedUser, loginDTO.getEmail(), SecurityToken.Type.ACCOUNT_ACTIVATION, null,
                        EmailTemplateUtil.accountActivationTemplateParams(), null, EmailTemplates.VERIFICATION_MAIL_TEMPLATE);
                message = "Account not activated. An activation link has been sent to your email.";
            }
            return Map.of("isError", true, "message", message);
        }

        return generateLoginInfo(loggedUser, httpResponse);
    }

    public String oAuth(String oAuthProvider, HttpServletRequest httpRequest) {
        OAuthProvider provider = OAuthProvider.fromCode(oAuthProvider);
        String jwtTokenHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isEmpty(jwtTokenHeader) || !jwtTokenHeader.startsWith("Bearer ")) {
            return oAuthProviderFactory.getProvider(provider).generateAuthFlowUrl(null);
        }

        Claims claims = jwtUtil.validateAndGetTokenPayload(jwtTokenHeader.substring(7), true);
        User user = userRepository.findByUuid(claims.get("id", String.class));
        userOAuthProviderService.ensureUserDoesNotHaveActiveLinkedProvider(user, provider);
        return oAuthProviderFactory.getProvider(provider).generateAuthFlowUrl(user);
    }

    @Transactional
    public Map<String, Object> oAuthCallback(String provider, OAuthRequestDTO oAuthRequestDTO, HttpServletResponse httpResponse) {
        OAuthProvider oAuthProvider = OAuthProvider.fromCode(provider);
        OAuthUserInfoDTO oAuthUserInfoDTO = oAuthProviderFactory.getProvider(oAuthProvider).authenticate(oAuthRequestDTO);
        boolean isLinkingFlow = oAuthUserInfoDTO.getUserId() != null;
        User user = isLinkingFlow ? handleOAuthLinkingFlow(oAuthProvider, oAuthUserInfoDTO) : handleOAuthLoginOrSignupFlow(oAuthProvider, oAuthUserInfoDTO);
        return isLinkingFlow ? Map.of("message", "Account linked successfully") : generateLoginInfo(user, httpResponse);
    }

    private User handleOAuthLinkingFlow(OAuthProvider provider, OAuthUserInfoDTO oAuthUserInfo) {
        User user = userRepository.findOne(oAuthUserInfo.getUserId());
        UserOAuthProvider existingProvider = userOAuthProviderRepository.findByUserAndProvider(user, provider);
        if (userEmailRepository.existsByEmailAndUserNot(oAuthUserInfo.getEmail(), user)) {
            throw new BusinessException(provider.getDisplayName() + " account's email already in use");
        }
        processOAuthData(user, provider, oAuthUserInfo, existingProvider);
        return user;
    }

    private User handleOAuthLoginOrSignupFlow(OAuthProvider provider, OAuthUserInfoDTO oAuthUserInfo) {
        Map<String, Object> userData = userService.createOrGetOAuthUser(oAuthUserInfo, provider);
        User user = (User) userData.get("user");
        boolean shouldLinkOAuthProvider = (boolean) userData.get("shouldLinkOAuthProvider");
        if (shouldLinkOAuthProvider) {
            processOAuthData(user, provider, oAuthUserInfo, null);
        }
        return user;
    }

    private void processOAuthData(User user, OAuthProvider provider, OAuthUserInfoDTO userInfo, UserOAuthProvider existingProvider) {
        userOAuthProviderService.createOrUpdate(user, userInfo, provider, existingProvider);
        userService.createOrUpdateUserEmail(user, userInfo.getEmail(), userInfo.getEmail().equals(user.getEmail()), true, List.of(provider.getEmailTag()));
        if (provider.equals(OAuthProvider.JIRA)) {
            String primaryProjectSetting = AppUtils.convertObjectToJsonString(userInfo.getAdditionalInfo().get(JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY));
            userPreferredSettingService.create(user, JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY, primaryProjectSetting);
        }
    }

    private Map<String, Object> generateLoginInfo(User user, HttpServletResponse httpResponse) {
        String accessToken = jwtUtil.generateToken(user.getUuid(), true);
        String refreshToken = jwtUtil.generateToken(user.getUuid(), false);
        httpResponse.addCookie(cookieFactory.create(CookieFactory.REFRESH_TOKEN_COOKIE_NAME, refreshToken, true, "/trackera/auth", CookieFactory.getRefreshTokenCookieMaxAge()));
        return Map.of("token", accessToken);
    }

    @Transactional
    public String unlinkOAuthProvider(String provider) {
        OAuthProvider oAuthProvider = OAuthProvider.fromCode(provider);
        User loggedUser = AppConfig.getAuthenticatedCurrentUser();
        UserOAuthProvider deletedOAuthProvider = userOAuthProviderService.delete(loggedUser, oAuthProvider);
        userService.removeEmailTag(loggedUser, deletedOAuthProvider.getEmail(), oAuthProvider.getEmailTag());
        return oAuthProvider.getDisplayName() + " unlinked successfully";
    }

    @Transactional
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String accessToken = httpRequest.getHeader(HttpHeaders.AUTHORIZATION).substring(7);
        String refreshToken = httpRequest.getCookies() != null ? Arrays.stream(httpRequest.getCookies()).filter(cookie -> cookie.getName().equals(CookieFactory.REFRESH_TOKEN_COOKIE_NAME)).map(Cookie::getValue).findFirst().orElse(null) : null;
        saveInvalidToken(accessToken, true);
        if (!StringUtils.isEmpty(refreshToken)) {
            saveInvalidToken(refreshToken, false);
        }
        httpResponse.addCookie(cookieFactory.create(CookieFactory.REFRESH_TOKEN_COOKIE_NAME, null, true, "/trackera/auth", 0));
    }

    private void saveInvalidToken(String token, boolean isAccessToken) {
        Claims accessTokenClaims = jwtUtil.getTokenPayload(token, isAccessToken);
        User user = userRepository.findByUuid(accessTokenClaims.get("id", String.class));
        Date accessTokenClaimsExpiration = accessTokenClaims.getExpiration();
        UserInvalidToken invalidAccessToken = new UserInvalidToken(user, trackeraHasher.hash(token, false), accessTokenClaimsExpiration, isAccessToken);
        userInvalidTokenRepository.save(invalidAccessToken);
    }

    public Map<String, Object> refreshJwt(String refreshToken) {
        Claims accessTokenClaims;
        try {
            accessTokenClaims = jwtUtil.validateAndGetTokenPayload(refreshToken, false);
        } catch (SecurityException e) {
            throw new SecurityException("Session expired.");
        }
        String newAccessToken = jwtUtil.generateToken(accessTokenClaims.get("id", String.class), true);
        return Map.of("token", newAccessToken);
    }

    @Transactional
    public AuthResultDTO processToken(String token) {
        SecurityToken securityToken = securityTokenService.validateAndGet(token);
        User user = securityToken.getUser();

        String message;
        switch (securityToken.getType()) {
            case ACCOUNT_ACTIVATION -> {
                userService.verifyUser(user);
                message = "Your account has been successfully activated";
            }
            case NEW_EMAIL_VERIFICATION -> {
                userService.verifyEmail(user, securityToken.getAdditionalInfo());
                message = "Your email has been successfully verified";
            }
            default -> throw new UnauthorizedException("Url is expired or invalid");
        }
        securityTokenRepository.delete(securityToken);

        return new AuthResultDTO(securityToken.getType().getLabel(), message);
    }

    @Transactional
    public String sendResetPassword(String email) {
        User user;
        try {
            user = userService.validateAndGetUserByEmail(email);
            if (user.canResetPassword() && user.getEmail().equals(email)) {
                userService.sendPasswordFlowEmail(user, email, true);
            }
        } catch (Exception e) {
            // do nothing
        }

        return "If the email exists, a password reset link has been sent to your email.";
    }

    @Transactional
    public void resetUserPassword(String token, PasswordDTO passwordDTO) {
        SecurityToken securityToken = securityTokenService.validateAndGet(token);
        if (!securityToken.getType().equals(SecurityToken.Type.PASSWORD_RESET)) {
            throw new UnauthorizedException("Url is expired or invalid");
        }
        User user = securityToken.getUser();
        userService.validateAndUpdateUserPassword(user, passwordDTO, true);
        securityTokenRepository.delete(securityToken);
    }
}
