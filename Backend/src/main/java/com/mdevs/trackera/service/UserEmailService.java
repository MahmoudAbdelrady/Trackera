package com.mdevs.trackera.service;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserEmail;
import com.mdevs.trackera.repository.UserEmailRepository;
import com.mdevs.trackera.repository.OAuthConnectionRepository;
import com.mdevs.trackera.shared.exceptions.types.BusinessException;
import com.mdevs.trackera.oauth.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserEmailService {
    private final UserEmailRepository userEmailRepository;

    private final OAuthConnectionRepository OAuthConnectionRepository;

    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,}$";

    //<editor-fold desc="Creation">
    public UserEmail create(User user, String email) {
        UserEmail userEmail = new UserEmail(user, email);
        return userEmailRepository.save(userEmail);
    }

    public UserEmail getOrCreate(User user, String email) {
        UserEmail userEmail = userEmailRepository.findByUserAndEmail(user, email);
        if (userEmail == null) {
            userEmail = create(user, email);
        }
        return userEmail;
    }
    //</editor-fold>

    //<editor-fold desc="Deletion">
    public void deleteIfUnused(UserEmail userEmail) {
        User user = userEmail.getUser();
        if (!user.isEmailLinked(userEmail) && OAuthConnectionRepository.notExistsByUserAndProviderEmail(userEmail.getUser(), userEmail)) {
            userEmailRepository.delete(userEmail);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Validation">
    public void ensureValidEmailFormat(String email) {
        if (StringUtils.isEmpty(email) || !email.matches(EMAIL_REGEX)) {
            throw new BusinessException("Invalid email format");
        }
    }

    public void ensureOAuthEmailAvailable(String email, User excludedUser, OAuthProvider provider) {
        if (userEmailRepository.existsByEmailAndUserNot(email, excludedUser)) {
            throw new BusinessException(provider.getDisplayName() + " account's email already in use");
        }
    }

    public void ensureEmailAvailableForUser(User loggedUser, UserEmail existingUserEmail) {
        if (!existingUserEmail.getUser().getId().equals(loggedUser.getId())) {
            throw new BusinessException("Email is already in use");
        }

        boolean isPrimaryOrPending = loggedUser.getPrimaryEmail().getId().equals(existingUserEmail.getId()) || (loggedUser.getPendingEmail() != null && loggedUser.getPendingEmail().getId().equals(existingUserEmail.getId()));
        if (isPrimaryOrPending) {
            throw new BusinessException("Email is already associated with your account");
        }
    }
    //</editor-fold>
}
