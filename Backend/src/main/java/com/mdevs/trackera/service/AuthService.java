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

    private final UserEmailService userEmailService;

    private final AuthenticationManager authenticationManager;

    private final SecurityTokenService securityTokenService;

    private final UserOAuthProviderService userOAuthProviderService;

    private final OAuthProviderFactory oAuthProviderFactory;

    private final SecurityTokenRepository securityTokenRepository;

    private final UserInvalidTokenRepository userInvalidTokenRepository;

    private final UserOAuthProviderRepository userOAuthProviderRepository;

    private final JwtUtil jwtUtil;

    private final CookieHelper cookieHelper;

    private final CryptoUtil cryptoUtil;

    public AuthService(UserRepository userRepository, UserEmailRepository userEmailRepository, UserService userService, UserEmailService userEmailService, AuthenticationManager authenticationManager,
                       SecurityTokenService securityTokenService, UserOAuthProviderService userOAuthProviderService, OAuthProviderFactory oAuthProviderFactory, SecurityTokenRepository securityTokenRepository,
                       UserInvalidTokenRepository userInvalidTokenRepository, UserOAuthProviderRepository userOAuthProviderRepository, JwtUtil jwtUtil, CookieHelper cookieHelper, CryptoUtil cryptoUtil) {
        this.userRepository = userRepository;
        this.userEmailRepository = userEmailRepository;
        this.userService = userService;
        this.userEmailService = userEmailService;
        this.authenticationManager = authenticationManager;
        this.securityTokenService = securityTokenService;
        this.userOAuthProviderService = userOAuthProviderService;
        this.oAuthProviderFactory = oAuthProviderFactory;
        this.securityTokenRepository = securityTokenRepository;
        this.userInvalidTokenRepository = userInvalidTokenRepository;
        this.userOAuthProviderRepository = userOAuthProviderRepository;
        this.jwtUtil = jwtUtil;
        this.cookieHelper = cookieHelper;
        this.cryptoUtil = cryptoUtil;
    }

    @Transactional
    public void signUp(SignUpDTO signUpDTO) {
        User user = userService.create(signUpDTO);
        securityTokenService.createAndSendSecurityToken(user, user.getPrimaryEmail().getEmail(), SecurityToken.Type.ACCOUNT_ACTIVATION, null,
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
        if (userEmailRepository.existsByEmailAndUserNot(oAuthUserInfo.getEmail(), user)) {
            throw new BusinessException(provider.getDisplayName() + " account's email already in use");
        }
        UserOAuthProvider existingProvider = userOAuthProviderRepository.findByUserAndProvider(user, provider); // for handling re-linking in case of revoked link
        processOAuthData(user, provider, oAuthUserInfo, existingProvider);
        userService.handleOAuthEmailMatching(oAuthUserInfo, user);
        return user;
    }

    private User handleOAuthLoginOrSignupFlow(OAuthProvider provider, OAuthUserInfoDTO oAuthUserInfo) {
        Map<String, Object> userData = userService.createOrGetOAuthUser(oAuthUserInfo, provider);
        User user = (User) userData.get("user");
        boolean isNewUser = (boolean) userData.get("isNewUser");
        if (isNewUser) {
            processOAuthData(user, provider, oAuthUserInfo, null);
        }
        return user;
    }

    private void processOAuthData(User user, OAuthProvider provider, OAuthUserInfoDTO userInfo, UserOAuthProvider existingProvider) {
        UserEmail userEmail = userEmailService.getOrCreate(user, userInfo.getEmail());
        userOAuthProviderService.createOrUpdate(user, userInfo, provider, existingProvider, userEmail);
        oAuthProviderFactory.getProvider(provider).handlePostLinkingActions(user, userInfo);
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
        UserEmail oAuthProviderEmail = deletedOAuthProvider.getProviderEmail();
        if (!oAuthProviderEmail.getId().equals(loggedUser.getPrimaryEmail().getId()) && (loggedUser.getPendingEmail() == null || !loggedUser.getPendingEmail().getId().equals(oAuthProviderEmail.getId()))) {
            userEmailRepository.delete(oAuthProviderEmail);
        }
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
        Claims accessTokenClaims = jwtUtil.getTokenPayload(token, isAccessToken);
        User user = userRepository.findByUuid(accessTokenClaims.get("id", String.class));
        Date accessTokenClaimsExpiration = accessTokenClaims.getExpiration();
        UserInvalidToken invalidAccessToken = new UserInvalidToken(user, cryptoUtil.hash(token, false), accessTokenClaimsExpiration, isAccessToken);
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
