package com.mdevs.trackera.service;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.dto.auth.*;
import com.mdevs.trackera.entity.*;
import com.mdevs.trackera.repository.*;
import com.mdevs.trackera.shared.EmailTemplates;
import com.mdevs.trackera.shared.SecurityTokenBuilder;
import com.mdevs.trackera.shared.exceptions.types.UnauthorizedException;
import com.mdevs.trackera.oauth.OAuthProvider;
import com.mdevs.trackera.oauth.OAuthProviderFactory;
import com.mdevs.trackera.utils.*;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    private final UserService userService;

    private final UserEmailService userEmailService;

    private final AuthenticationManager authenticationManager;

    private final SecurityTokenService securityTokenService;

    private final OAuthConnectionService oAuthConnectionService;

    private final UserInvalidTokenService userInvalidTokenService;

    private final OAuthProviderFactory oAuthProviderFactory;

    private final SecurityTokenRepository securityTokenRepository;

    private final JwtUtil jwtUtil;

    private final CookieHelper cookieHelper;

    //<editor-fold desc="Registration & Authentication">
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
        securityTokenService.createAndSend(securityTokenBuilder);
    }

    @Transactional
    public Map<String, Object> login(LoginDTO loginDTO, HttpServletResponse response) {
        Authentication authentication;
        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword(), List.of());
            authentication = authenticationManager.authenticate(authToken);
        } catch (AuthenticationException exception) {
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
                securityTokenService.createAndSend(securityTokenBuilder);
                message = "Account not activated. An activation link has been sent to your email.";
            }
            return Map.of("isError", true, "message", message);
        }

        generateLoginInfo(loggedUser, response);
        return null;
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = jwtUtil.getToken(request, true);
        Claims accessTokenClaims = jwtUtil.getTokenPayload(accessToken, true);
        String refreshToken = jwtUtil.getToken(request, false);
        Claims refreshTokenClaims = jwtUtil.getTokenPayload(accessToken, true);

        User loggedUser = AppConfig.getAuthenticatedCurrentUser();
        userInvalidTokenService.create(loggedUser, accessToken, accessTokenClaims.getExpiration(), true);
        if (!StringUtils.isEmpty(refreshToken)) {
            userInvalidTokenService.create(loggedUser, refreshToken, refreshTokenClaims.getExpiration(), false);
        }

        response.addCookie(cookieHelper.create(CookieHelper.ACCESS_TOKEN_COOKIE_NAME, null, true, CookieHelper.COOKIE_GENERAL_PATH, 0));
        response.addCookie(cookieHelper.create(CookieHelper.REFRESH_TOKEN_COOKIE_NAME, null, true, CookieHelper.COOKIE_AUTH_PATH, 0));
    }

    public void getSession(String refreshToken) {
        jwtUtil.validateAndGetTokenPayload(refreshToken, false);
    }

    public void refreshJwt(String refreshToken, HttpServletResponse response) {
        Claims refreshTokenClaims = jwtUtil.validateAndGetTokenPayload(refreshToken, false);
        String userUuid = refreshTokenClaims.get("id", String.class);
        String newAccessToken = jwtUtil.generateToken(userUuid, true);
        User tokenUser = userService.findByUuidOrThrow(userUuid);

        response.addCookie(cookieHelper.create(CookieHelper.ACCESS_TOKEN_COOKIE_NAME, newAccessToken, true, CookieHelper.COOKIE_GENERAL_PATH, CookieHelper.getTokenCookieMaxAge(true)));

        LocalDateTime refreshTokenExpiry = AppUtils.convertDateToLocalDateTime(refreshTokenClaims.getExpiration());
        if (refreshTokenExpiry.isBefore(LocalDateTime.now().plusDays(CookieHelper.REFRESH_TOKEN_ROTATION_THRESHOLD_DAYS))) {
            String newRefreshToken = jwtUtil.generateToken(userUuid, false);
            response.addCookie(cookieHelper.create(CookieHelper.REFRESH_TOKEN_COOKIE_NAME, newRefreshToken, true, CookieHelper.COOKIE_AUTH_PATH, CookieHelper.getTokenCookieMaxAge(false)));
            userInvalidTokenService.create(tokenUser, refreshToken, refreshTokenClaims.getExpiration(), false);
        }
    }
    //</editor-fold>

    //<editor-fold desc="OAuth2 Integration">
    public String oAuth(String providerCode, Boolean forceLink, HttpServletRequest request) {
        OAuthProvider provider = OAuthProvider.fromCode(providerCode);
        String jwt = jwtUtil.getToken(request, true);
        if (jwt == null) {
            return oAuthProviderFactory.getProvider(provider).generateAuthFlowUrl(null);
        }

        Claims claims = jwtUtil.validateAndGetTokenPayload(jwt, true);
        User user = userService.findByUuidOrThrow(claims.get("id", String.class));
        if (!forceLink) {
            oAuthConnectionService.ensureNoConnection(user, provider);
        }
        return oAuthProviderFactory.getProvider(provider).generateAuthFlowUrl(user);
    }

    @Transactional
    public Map<String, Object> oAuthCallback(String providerCode, OAuthRequestDTO oAuthRequestDTO, HttpServletResponse response) {
        OAuthProvider provider = OAuthProvider.fromCode(providerCode);
        OAuthUserInfoDTO oAuthUserInfoDTO = oAuthProviderFactory.getProvider(provider).authenticate(oAuthRequestDTO);

        Map<String, Object> result = null;
        if (oAuthUserInfoDTO.getUserId() != null) {
            handleOAuthLinkingFlow(provider, oAuthUserInfoDTO);
            result = Map.of("message", "Account linked successfully");
        } else {
            User user = handleOAuthLoginOrSignupFlow(provider, oAuthUserInfoDTO);
            generateLoginInfo(user, response);
        }

        return result;
    }

    @Transactional
    public String unlinkOAuthProvider(String providerCode) {
        OAuthProvider provider = OAuthProvider.fromCode(providerCode);
        User currentUser = AppConfig.getAuthenticatedCurrentUser();
        OAuthConnection deletedConnection = oAuthConnectionService.delete(currentUser, provider);
        oAuthProviderFactory.getProvider(provider).handlePostUnLinkingActions(currentUser);
        userEmailService.deleteIfUnused(deletedConnection.getAccountEmail());
        return provider.getDisplayName() + " unlinked successfully";
    }
    //</editor-fold>

    //<editor-fold desc="Security Token Management">
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
    //</editor-fold>

    //<editor-fold desc="Password Management">
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
    //</editor-fold>

    //<editor-fold desc="Internal Methods & Validations">
    private void handleOAuthLinkingFlow(OAuthProvider provider, OAuthUserInfoDTO oAuthUserInfo) {
        User user = userRepository.findOne(oAuthUserInfo.getUserId());
        userEmailService.ensureOAuthEmailAvailable(oAuthUserInfo.getEmail(), user, provider);
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
        oAuthConnectionService.createOrUpdate(user, oAuthUserInfoDTO, provider, userEmail);
        oAuthProviderFactory.getProvider(provider).handlePostLinkingActions(user, oAuthUserInfoDTO);
    }

    private void generateLoginInfo(User user, HttpServletResponse response) {
        String accessToken = jwtUtil.generateToken(user.getUuid(), true);
        String refreshToken = jwtUtil.generateToken(user.getUuid(), false);
        response.addCookie(cookieHelper.create(CookieHelper.ACCESS_TOKEN_COOKIE_NAME, accessToken, true, CookieHelper.COOKIE_GENERAL_PATH, CookieHelper.getTokenCookieMaxAge(true)));
        response.addCookie(cookieHelper.create(CookieHelper.REFRESH_TOKEN_COOKIE_NAME, refreshToken, true, CookieHelper.COOKIE_AUTH_PATH, CookieHelper.getTokenCookieMaxAge(false)));
    }
    //</editor-fold>
}
