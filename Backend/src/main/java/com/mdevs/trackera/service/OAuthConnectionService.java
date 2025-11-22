package com.mdevs.trackera.service;

import com.mdevs.trackera.dto.auth.OAuthAccessCredentialsDTO;
import com.mdevs.trackera.dto.auth.OAuthUserInfoDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserEmail;
import com.mdevs.trackera.entity.OAuthConnection;
import com.mdevs.trackera.repository.OAuthConnectionRepository;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.oauth.OAuthProvider;
import com.mdevs.trackera.oauth.OAuthProviderFactory;
import com.mdevs.trackera.utils.CryptoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OAuthConnectionService {
    private final OAuthConnectionRepository oAuthConnectionRepository;

    private final OAuthConnectionService selfRef;

    private final OAuthProviderFactory oAuthProviderFactory;

    private final CryptoUtil cryptoUtil;

    public OAuthConnectionService(OAuthConnectionRepository oAuthConnectionRepository, @Lazy OAuthConnectionService selfRef, OAuthProviderFactory oAuthProviderFactory, CryptoUtil cryptoUtil) {
        this.oAuthConnectionRepository = oAuthConnectionRepository;
        this.selfRef = selfRef;
        this.oAuthProviderFactory = oAuthProviderFactory;
        this.cryptoUtil = cryptoUtil;
    }

    //<editor-fold desc="Retrieval">
    public Map<OAuthProvider, OAuthConnection> getConnectionsAsMap(User user) {
        return oAuthConnectionRepository.findByUser(user).stream().collect(Collectors.toMap(OAuthConnection::getProvider, o -> o));
    }

    public OAuthConnection getConnectionByUserAndEmailOrThrow(User user, String email, OAuthProvider provider) {
        return Optional.ofNullable(oAuthConnectionRepository.findByUserAndEmailAndProvider(user, email, provider))
                .orElseThrow(() -> new BusinessException("Account is not linked to " + provider.getDisplayName()));
    }

    public OAuthConnection validateAndGetConnection(User user, OAuthProvider provider) {
        OAuthConnection connection = oAuthConnectionRepository.findByUserAndProvider(user, provider);
        if (connection == null) {
            throw new BusinessException(provider + " account not linked");
        }
        if (connection.isRevoked()) {
            throw new BusinessException(provider + " account link has been revoked. Please relink your account.");
        }
        return connection;
    }

    public OAuthConnection getOrRefresh(User user, OAuthProvider provider) {
        OAuthConnection connection = validateAndGetConnection(user, provider);
        if (connection.isExpired()) {
            try {
                connection = selfRef.refreshAndUpdateCredentials(connection.getId());
            } catch (Exception e) {
                log.error("Error while resolving access token for provider {}: {}", connection.getProvider(), e.getMessage(), e);
                throw new RuntimeException("Failed to authenticate with " + connection.getProvider());
            }
            if (connection.isRevoked()) {
                throw new RuntimeException("Failed to refresh access token");
            }
        }
        return connection;
    }

    public OAuthConnection getOrRefresh(OAuthConnection connection) {
        return getOrRefresh(connection.getUser(), connection.getProvider());
    }

    public String getAccessToken(OAuthConnection connection) {
        return cryptoUtil.decryptFromBase64(connection.getAccessToken(), false);
    }
    //</editor-fold>

    //<editor-fold desc="Creation and Update">
    public void createOrUpdate(User user, OAuthUserInfoDTO oAuthUserInfoDTO, OAuthProvider provider, UserEmail userEmail) {
        OAuthConnection existingConnection = oAuthConnectionRepository.findByUserAndProvider(user, provider); // for handling re-linking in case of revoked link
        if (existingConnection == null) {
            existingConnection = new OAuthConnection(user, provider);
        }
        existingConnection.setAccountEmail(userEmail);
        updateAccessCredentials(existingConnection, oAuthUserInfoDTO.getAccessCredentials());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OAuthConnection refreshAndUpdateCredentials(Long connectionId) {
        OAuthConnection connection = oAuthConnectionRepository.findOne(connectionId);
        try {
            OAuthAccessCredentialsDTO newCredentials = oAuthProviderFactory.getProvider(connection.getProvider()).refreshOAuthProviderCredentials(connection);
            connection = updateAccessCredentials(connection, newCredentials);
        } catch (HttpClientErrorException exception) {
            connection.setRevoked(true);
            connection = oAuthConnectionRepository.save(connection);
        }
        return connection;
    }

    public OAuthConnection updateAccessCredentials(OAuthConnection existingConnection, OAuthAccessCredentialsDTO accessCredentialsDTO) {
        existingConnection.setAccessToken(cryptoUtil.encryptToBase64(accessCredentialsDTO.getAccessToken(), false));
        if (accessCredentialsDTO.getRefreshToken() != null) {
            existingConnection.setRefreshToken(cryptoUtil.encryptToBase64(accessCredentialsDTO.getRefreshToken(), false));
        }
        existingConnection.setAccessTokenExpiry(LocalDateTime.now().plusSeconds(accessCredentialsDTO.getExpiresIn()));
        existingConnection.setRevoked(false);
        return oAuthConnectionRepository.save(existingConnection);
    }
    //</editor-fold>

    //<editor-fold desc="Deletion">
    public OAuthConnection delete(User user, OAuthProvider provider) {
        OAuthConnection deletedConnection = oAuthConnectionRepository.findByUserAndProvider(user, provider);
        if (deletedConnection == null) {
            throw new BusinessException("Your account is not linked with " + provider.getDisplayName());
        }
        if (oAuthConnectionRepository.countByUser(user) <= 1 && !user.isPasswordSet()) {
            throw new BusinessException("You must have at least one sign-in method linked to your account");
        }
        oAuthConnectionRepository.delete(deletedConnection);
        return deletedConnection;
    }
    //</editor-fold>

    //<editor-fold desc="Validations">
    public void ensureNoConnection(User user, OAuthProvider provider) {
        OAuthConnection connection = oAuthConnectionRepository.findByUserAndProvider(user, provider);
        if (connection != null && !connection.isRevoked()) {
            throw new BusinessException("The current account is already linked with " + provider.getDisplayName());
        }
    }
    //</editor-fold>
}
