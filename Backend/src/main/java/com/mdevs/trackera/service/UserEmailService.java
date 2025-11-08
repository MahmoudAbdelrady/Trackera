package com.mdevs.trackera.service;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserEmail;
import com.mdevs.trackera.repository.UserEmailRepository;
import com.mdevs.trackera.repository.UserOAuthProviderRepository;
import org.springframework.stereotype.Service;

@Service
public class UserEmailService {
    private final UserEmailRepository userEmailRepository;

    private final UserOAuthProviderRepository userOAuthProviderRepository;

    public UserEmailService(UserEmailRepository userEmailRepository, UserOAuthProviderRepository userOAuthProviderRepository) {
        this.userEmailRepository = userEmailRepository;
        this.userOAuthProviderRepository = userOAuthProviderRepository;
    }

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

    public void deleteIfNotLinkedToOAuth(UserEmail userEmail) {
        if (userOAuthProviderRepository.notExistsByUserAndProviderEmail(userEmail.getUser(), userEmail)) {
            userEmailRepository.delete(userEmail);
        }
    }
}
