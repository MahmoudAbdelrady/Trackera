package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.*;
import com.mdevs.trackera.entity.*;
import com.mdevs.trackera.repository.*;
import com.mdevs.trackera.shared.EmailTemplates;
import com.mdevs.trackera.shared.SecurityTokenBuilder;
import com.mdevs.trackera.shared.exceptions.types.UnauthorizedException;
import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import com.mdevs.trackera.shared.oauth_provider.OAuthProviderFactory;
import com.mdevs.trackera.utils.*;
import io.jsonwebtoken.Claims;
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

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {
    private final UserRepository userRepository;

    private final UserService userService;

    private final UserEmailService userEmailService;

    private final AuthenticationManager authenticationManager;

    private final SecurityTokenService securityTokenService;

    private final UserOAuthProviderService userOAuthProviderService;

    private final OAuthProviderFactory oAuthProviderFactory;

    private final SecurityTokenRepository securityTokenRepository;

    private final UserInvalidTokenRepository userInvalidTokenRepository;

    private final JwtUtil jwtUtil;

    private final CookieHelper cookieHelper;

    private final CryptoUtil cryptoUtil;

    private static final int REFRESH_TOKEN_ROTATION_THRESHOLD_DAYS = 3;

    public AuthService(UserRepository userRepository, UserService userService, UserEmailService userEmailService, AuthenticationManager authenticationManager, SecurityTokenService securityTokenService,
                       UserOAuthProviderService userOAuthProviderService, OAuthProviderFactory oAuthProviderFactory, SecurityTokenRepository securityTokenRepository, UserInvalidTokenRepository userInvalidTokenRepository,
                       JwtUtil jwtUtil, CookieHelper cookieHelper, CryptoUtil cryptoUtil) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.userEmailService = userEmailService;
        this.authenticationManager = authenticationManager;
        this.securityTokenService = securityTokenService;
        this.userOAuthProviderService = userOAuthProviderService;
        this.oAuthProviderFactory = oAuthProviderFactory;
        this.securityTokenRepository = securityTokenRepository;
        this.userInvalidTokenRepository = userInvalidTokenRepository;
        this.jwtUtil = jwtUtil;
        this.cookieHelper = cookieHelper;
        this.cryptoUtil = cryptoUtil;
    }

    @Transactional
    public void signUp(SignUpDTO signUpDTO) {
        User user = userService.create(signUpDTO);
        SecurityTokenBuilder securityTokenBuilder = SecurityTokenBuilder.builder()
                .user(user)
                .targetEmail(user.getPrimaryEmail().getEmail())
                .type(SecurityToken.Type.ACCOUNT_ACTIVATION)
                .templateName(EmailTemplates.VERIFICATION_MAIL_TEMPLATE)
                .extraParameters(EmailTemplateUtil.getTemplateParams(SecurityToken.Type.ACCOUNT_ACTIVATION))
                .build();
        securityTokenService.createAndSendSecurityToken(securityTokenBuilder);
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
                SecurityTokenBuilder securityTokenBuilder = SecurityTokenBuilder.builder()
                        .user(loggedUser)
                        .targetEmail(loginDTO.getEmail())
                        .type(SecurityToken.Type.ACCOUNT_ACTIVATION)
                        .templateName(EmailTemplates.VERIFICATION_MAIL_TEMPLATE)
                        .extraParameters(EmailTemplateUtil.getTemplateParams(SecurityToken.Type.ACCOUNT_ACTIVATION))
                        .build();
                securityTokenService.createAndSendSecurityToken(securityTokenBuilder);
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

        Map<String, Object> result;
        if (oAuthUserInfoDTO.getUserId() != null) {
            handleOAuthLinkingFlow(oAuthProvider, oAuthUserInfoDTO);
            result = Map.of("message", "Account linked successfully");
        } else {
            User user = handleOAuthLoginOrSignupFlow(oAuthProvider, oAuthUserInfoDTO);
            result = generateLoginInfo(user, httpResponse);
        }

        return result;
    }

    private void handleOAuthLinkingFlow(OAuthProvider provider, OAuthUserInfoDTO oAuthUserInfo) {
        User user = userRepository.findOne(oAuthUserInfo.getUserId());
        userEmailService.validateOAuthEmailNotInUse(oAuthUserInfo.getEmail(), user, provider);
        processOAuthData(user, provider, oAuthUserInfo);
        userService.handleOAuthEmailMatching(user, oAuthUserInfo);
    }

    private User handleOAuthLoginOrSignupFlow(OAuthProvider provider, OAuthUserInfoDTO oAuthUserInfo) {
        Map<String, Object> userData = userService.createOrGetOAuthUser(oAuthUserInfo, provider);
        User user = (User) userData.get("user");
        boolean isNewUser = (boolean) userData.get("isNewUser");
        if (isNewUser) {
            processOAuthData(user, provider, oAuthUserInfo);
        }
        return user;
    }

    private void processOAuthData(User user, OAuthProvider provider, OAuthUserInfoDTO oAuthUserInfoDTO) {
        UserEmail userEmail = userEmailService.getOrCreate(user, oAuthUserInfoDTO.getEmail());
        userOAuthProviderService.createOrUpdate(user, oAuthUserInfoDTO, provider, userEmail);
        oAuthProviderFactory.getProvider(provider).handlePostLinkingActions(user, oAuthUserInfoDTO);
    }

    private Map<String, Object> generateLoginInfo(User user, HttpServletResponse httpResponse) {
        String accessToken = jwtUtil.generateToken(user.getUuid(), true);
        String refreshToken = jwtUtil.generateToken(user.getUuid(), false);
        httpResponse.addCookie(cookieHelper.create(CookieHelper.REFRESH_TOKEN_COOKIE_NAME, refreshToken, true, "/trackera/auth", CookieHelper.getRefreshTokenCookieMaxAge()));
        return Map.of("token", accessToken);
    }

    @Transactional
    public String unlinkOAuthProvider(String provider) {
        OAuthProvider oAuthProvider = OAuthProvider.fromCode(provider);
        User loggedUser = AppConfig.getAuthenticatedCurrentUser();
        UserOAuthProvider deletedOAuthProvider = userOAuthProviderService.delete(loggedUser, oAuthProvider);
        oAuthProviderFactory.getProvider(oAuthProvider).handlePostUnLinkingActions(loggedUser);
        userEmailService.deleteIfUnused(deletedOAuthProvider.getProviderEmail());
        return oAuthProvider.getDisplayName() + " unlinked successfully";
    }

    @Transactional
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String accessToken = httpRequest.getHeader(HttpHeaders.AUTHORIZATION).substring(7);
        String refreshToken = cookieHelper.extractCookieValue(httpRequest, CookieHelper.REFRESH_TOKEN_COOKIE_NAME);
        saveInvalidToken(accessToken, true);
        if (!StringUtils.isEmpty(refreshToken)) {
            saveInvalidToken(refreshToken, false);
        }
        httpResponse.addCookie(cookieHelper.create(CookieHelper.REFRESH_TOKEN_COOKIE_NAME, null, true, "/trackera/auth", 0));
    }

    private void saveInvalidToken(String token, boolean isAccessToken) {
        Claims tokenClaims = jwtUtil.getTokenPayload(token, isAccessToken);
        User user = userRepository.findByUuid(tokenClaims.get("id", String.class));
        LocalDateTime tokenExpiryDate = AppUtils.convertDateToLocalDateTime(tokenClaims.getExpiration());
        UserInvalidToken invalidAccessToken = new UserInvalidToken(user, cryptoUtil.hash(token, false), tokenExpiryDate, isAccessToken);
        userInvalidTokenRepository.save(invalidAccessToken);
    }

    public Map<String, Object> refreshJwt(String refreshToken, HttpServletResponse httpResponse) {
        Claims refreshTokenClaims = jwtUtil.validateAndGetTokenPayload(refreshToken, false);
        String newAccessToken = jwtUtil.generateToken(refreshTokenClaims.get("id", String.class), true);

        LocalDateTime refreshTokenExpiry = AppUtils.convertDateToLocalDateTime(refreshTokenClaims.getExpiration());
        if (refreshTokenExpiry.isBefore(LocalDateTime.now().plusDays(REFRESH_TOKEN_ROTATION_THRESHOLD_DAYS))) {
            String newRefreshToken = jwtUtil.generateToken(refreshTokenClaims.get("id", String.class), false);
            httpResponse.addCookie(cookieHelper.create(CookieHelper.REFRESH_TOKEN_COOKIE_NAME, newRefreshToken, true, "/trackera/auth", CookieHelper.getRefreshTokenCookieMaxAge()));
            saveInvalidToken(refreshToken, false);
        }

        return Map.of("token", newAccessToken);
    }

    @Transactional
    public AuthResultDTO consumeToken(String token) {
        SecurityToken securityToken = securityTokenService.validateAndGet(token);
        User user = securityToken.getUser();

        String message;
        switch (securityToken.getType()) {
            case ACCOUNT_ACTIVATION -> {
                userService.verifyUser(user);
                message = "Your account has been successfully activated";
            }
            case NEW_EMAIL_VERIFICATION -> {
                userService.verifyAndChangePrimaryEmail(user, securityToken.getAdditionalInfo());
                message = "Your email has been successfully verified";
            }
            default -> throw new UnauthorizedException("Url is expired or invalid");
        }
        securityTokenRepository.delete(securityToken);

        return new AuthResultDTO(securityToken.getType().getLabel(), message);
    }

    @Transactional
    public void requestResetPassword(String email) {
        User user = userRepository.findByPrimaryEmail(email);
        if (user != null && user.canResetPassword()) {
            userService.sendPasswordFlowEmail(user, true);
        }
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
