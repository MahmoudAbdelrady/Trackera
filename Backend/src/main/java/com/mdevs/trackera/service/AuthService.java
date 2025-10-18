package com.mdevs.trackera.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.*;
import com.mdevs.trackera.entity.*;
import com.mdevs.trackera.repository.*;
import com.mdevs.trackera.shared.EmailTemplates;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.shared.exceptions.types.UnauthorizedException;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.shared.oauth_provider.OAuthProviderFactory;
import com.mdevs.trackera.shared.utils.AppUtils;
import com.mdevs.trackera.shared.utils.JwtUtil;
import com.mdevs.trackera.shared.utils.TrackeraHasher;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {
    private final UserRepository userRepository;

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

    private final TrackeraHasher trackeraHasher;

    private final static String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    @Value("${trackera.environment}")
    private String trackeraEnv;

    @Value("${trackera.cookie.max-age}")
    private String cookieMaxAge;

    @Autowired
    public AuthService(UserRepository userRepository, UserService userService, AuthenticationManager authenticationManager, SecurityTokenService securityTokenService, UserOAuthProviderService userOAuthProviderService, UserPreferredSettingService userPreferredSettingService, OAuthProviderFactory oAuthProviderFactory,
                       SecurityTokenRepository securityTokenRepository, UserInvalidTokenRepository userInvalidTokenRepository, UserOAuthProviderRepository userOAuthProviderRepository,
                       JwtUtil jwtUtil, TrackeraHasher trackeraHasher) {
        this.userRepository = userRepository;
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
        this.trackeraHasher = trackeraHasher;
    }

    public List<Map<String, Object>> getUserOAuthProviders() {
        User loggedUser = AppConfig.getCurrentUser();
        List<UserOAuthProvider> userOAuthProviders = userOAuthProviderRepository.findByUser(loggedUser);
        return Arrays.stream(OAuthProvider.values()).map(p -> {
            UserOAuthProvider userOAuthProvider = userOAuthProviders.stream().filter(uop -> uop.getProvider().equals(p)).findFirst().orElse(null);
            Map<String, Object> providerInfo = new HashMap<>();
            providerInfo.put("provider", Map.of("code", p.getCode(), "name", p.getDisplayName()));
            boolean userOAuthProviderExists = userOAuthProvider != null;
            providerInfo.put("isLinked", userOAuthProvider != null);
            providerInfo.put("email", userOAuthProvider != null && !StringUtils.isEmpty(userOAuthProvider.getEmail()) ? userOAuthProvider.getEmail() : null);
            if (userOAuthProviderExists) {
                providerInfo.put("isRevoked", userOAuthProvider.isRevoked());
            }
            return providerInfo;
        }).toList();
    }

    @Transactional
    public String signUp(SignUpDTO signUpDTO) {
        User user = userService.create(signUpDTO);
        Map<String, String> templateParameters = new HashMap<>();
        templateParameters.put("emailTypeDesc", "Please click the link below to activate your account.");
        templateParameters.put("linkLabel", "Activate my account");
        securityTokenService.createAndSendSecurityToken(user, user.getEmail(), SecurityToken.Type.ACCOUNT_ACTIVATION, null, templateParameters, null, EmailTemplates.VERIFICATION_MAIL_TEMPLATE);
        return "Account created successfully. Please check your email for verification.";
    }

    @Transactional
    public Map<String, Object> login(LoginDTO loginDTO, HttpServletResponse httpResponse) {
        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword(), List.of());
            Authentication authentication = authenticationManager.authenticate(authToken);
            User loggedUser = (User) authentication.getPrincipal();

            Map<String, Object> result = new HashMap<>();
            if (!loggedUser.isVerified()) {
                String message;
                if (securityTokenRepository.existsByUserAndTypeAndCreatedAtGreaterThanEqual(loggedUser, SecurityToken.Type.ACCOUNT_ACTIVATION, LocalDateTime.now().minusMinutes(SecurityTokenService.MAX_SECURITY_TOKEN_MINUTES))) {
                    message = "Account not activated. Please check your email for the activation link.";
                } else {
                    Map<String, String> templateParameters = new HashMap<>();
                    templateParameters.put("emailTypeDesc", "Please click the link below to activate your account.");
                    templateParameters.put("linkLabel", "Activate my account");
                    securityTokenService.createAndSendSecurityToken(loggedUser, loginDTO.getEmail(), SecurityToken.Type.ACCOUNT_ACTIVATION, null, templateParameters, null, EmailTemplates.VERIFICATION_MAIL_TEMPLATE);
                    message = "Account not activated. An activation link has been sent to your email.";
                }
                result.put("isError", true);
                result.put("message", message);
            } else {
                result = generateLoginInfo(loggedUser, httpResponse);
            }

            return result;
        } catch (AuthenticationException e) {
            throw new SecurityException("Invalid credentials");
        }
    }

    public Map<String, Object> oAuth(String oAuthProvider, HttpServletRequest httpRequest) {
        String flowUrl = oAuthProviderFactory.getProvider(OAuthProvider.fromCode(oAuthProvider)).generateAuthFlowUrl(httpRequest);
        return Map.of("url", flowUrl);
    }

    @Transactional
    public Map<String, Object> oAuthCallback(String provider, OAuthRequestDTO oAuthRequestDTO, HttpServletResponse httpResponse) {
        OAuthProvider oAuthProvider = OAuthProvider.fromCode(provider);
        OAuthUserInfoDTO oAuthUserInfoDTO = oAuthProviderFactory.getProvider(oAuthProvider).authenticate(oAuthRequestDTO);
        User authenticatedUser;
        boolean createOrUpdateOAuthProvider = true;
        boolean isLinkingAccount = false;
        UserOAuthProvider userOAuthProvider = null;

        if (oAuthUserInfoDTO.getUserId() != null) { // means the user is linking an oAuth provider as user id is fetched from jwt
            isLinkingAccount = true;
            authenticatedUser = userRepository.findOne(oAuthUserInfoDTO.getUserId());
            userOAuthProvider = userOAuthProviderRepository.findByUserAndProvider(authenticatedUser, oAuthProvider);
            if (userOAuthProvider != null && !userOAuthProvider.isExpired()) {
                throw new BusinessException("The current account is already linked with " + oAuthProvider.getDisplayName());
            }
            if (userRepository.existsByUserEmailOrOAuthProvidersEmailAndIdNot(oAuthUserInfoDTO.getEmail(), authenticatedUser.getId())) {
                throw new BusinessException(oAuthProvider.getDisplayName() + " account's email already in use");
            }
        } else {
            Map<String, Object> oAuthUserData = userService.createOrGetOAuthUser(oAuthUserInfoDTO, oAuthProvider);
            authenticatedUser = (User) oAuthUserData.get("user");
            createOrUpdateOAuthProvider = (boolean) oAuthUserData.get("createOAuthProvider");
        }

        if (createOrUpdateOAuthProvider) {
            userOAuthProviderService.createOrUpdate(authenticatedUser, oAuthUserInfoDTO, oAuthProvider, userOAuthProvider);
            userService.createUserEmail(authenticatedUser, oAuthUserInfoDTO.getEmail(), oAuthUserInfoDTO.getEmail().equals(authenticatedUser.getEmail()), true, List.of(oAuthProvider.getEmailTag()));
        }

        handleOAuthProviderAdditionalInfo(authenticatedUser, oAuthProvider, oAuthUserInfoDTO.getAdditionalInfo());

        return isLinkingAccount ? Map.of("message", "Account linked successfully") : generateLoginInfo(authenticatedUser, httpResponse);
    }

    private void handleOAuthProviderAdditionalInfo(User authenticatedUser, OAuthProvider oAuthProvider, Map<String, Object> additionalInfo) {
        if (additionalInfo == null || additionalInfo.isEmpty())
            return;

        if (oAuthProvider.equals(OAuthProvider.JIRA)) {
            try {
                userPreferredSettingService.create(authenticatedUser, JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY, AppUtils.getObjectMapper().writeValueAsString(additionalInfo.get(JiraService.JIRA_PRIMARY_PROJECT_SETTING_KEY)));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Map<String, Object> generateLoginInfo(User user, HttpServletResponse httpResponse) {
        String accessToken = jwtUtil.generateToken(user.getEmail(), true);
        String refreshToken = jwtUtil.generateToken(user.getEmail(), false);
        httpResponse.addCookie(createTrackeraCookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken, true, Integer.parseInt(cookieMaxAge)));
        return Map.of("token", accessToken);
    }

    @Transactional
    public String unlinkOAuthProvider(String provider) {
        OAuthProvider oAuthProvider = OAuthProvider.fromCode(provider);
        User loggedUser = Objects.requireNonNull(AppConfig.getCurrentUser());
        UserOAuthProvider deletedOAuthProvider = userOAuthProviderService.delete(loggedUser, oAuthProvider);
        userService.removeEmailTag(loggedUser, deletedOAuthProvider.getEmail(), oAuthProvider.getEmailTag());
        return oAuthProvider.getDisplayName() + " unlinked successfully";
    }

    @Transactional
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String accessToken = httpRequest.getHeader(HttpHeaders.AUTHORIZATION).substring(7);
        String refreshToken = httpRequest.getCookies() != null ? Arrays.stream(httpRequest.getCookies()).filter(cookie -> cookie.getName().equals(REFRESH_TOKEN_COOKIE_NAME)).map(Cookie::getValue).findFirst().orElse(null) : null;
        saveInvalidToken(accessToken, true);
        if (!StringUtils.isEmpty(refreshToken)) {
            saveInvalidToken(refreshToken, false);
        }
        httpResponse.addCookie(createTrackeraCookie(REFRESH_TOKEN_COOKIE_NAME, null, true, 0));
    }

    private Cookie createTrackeraCookie(String name, String value, boolean isHttpOnly, int cookieMaxAge) {
        Cookie trackeraCookie = new Cookie(name, value);
        trackeraCookie.setHttpOnly(isHttpOnly);
        trackeraCookie.setSecure(trackeraEnv.equals("prod"));
        trackeraCookie.setPath(isHttpOnly ? "/trackera/auth" : "/");
        trackeraCookie.setMaxAge(cookieMaxAge);
        return trackeraCookie;
    }

    public void saveInvalidToken(String token, boolean isAccessToken) {
        Claims accessTokenClaims = jwtUtil.getTokenPayload(token, isAccessToken);
        User user = userRepository.findByEmail(accessTokenClaims.get("email", String.class));
        Date accessTokenClaimsExpiration = accessTokenClaims.getExpiration();
        UserInvalidToken invalidAccessToken = new UserInvalidToken(user, trackeraHasher.hash(token, false), accessTokenClaimsExpiration, isAccessToken);
        userInvalidTokenRepository.save(invalidAccessToken);
    }

    public Map<String, Object> refreshJwt(String refreshToken) {
        Claims accessTokenClaims;
        try {
            accessTokenClaims = jwtUtil.validateAndGetTokenPayload(refreshToken, false);
        } catch (SecurityException e) {
            throw new SecurityException("Login has expired. Please sign in again.");
        }
        String newAccessToken = jwtUtil.generateToken(accessTokenClaims.get("email", String.class), true);
        return Map.of("token", newAccessToken);
    }

    @Transactional
    public AuthResultDTO processToken(String token) {
        SecurityToken securityToken = securityTokenService.getSecurityToken(token);
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

    public void validateToken(String token) {
        securityTokenService.getSecurityToken(token);
    }

    @Transactional
    public String sendResetPassword(String email) {
        User user = null;
        try {
            user = userService.validateAndGetUserByEmail(email);
        } catch (Exception e) {
            // do nothing
        }

        if (user != null && user.canResetPassword()) {
            userService.sendResetPasswordEmail(user, email, "Please click the link below to reset your password.", true);
        }

        return "If the email exists, a password reset link has been sent to your email.";
    }

    @Transactional
    public String changePassword(String token, PasswordDTO passwordDTO) {
        SecurityToken securityToken = securityTokenService.getSecurityToken(token);
        if (securityToken.getType().equals(SecurityToken.Type.ACCOUNT_ACTIVATION)) {
            throw new UnauthorizedException("Url is expired or invalid");
        }
        User user = securityToken.getUser();
        user.setPassword(userService.getUserNewPassword(user, passwordDTO));
        userRepository.save(user);
        securityTokenRepository.delete(securityToken);
        return "Password changed successfully";
    }
}
