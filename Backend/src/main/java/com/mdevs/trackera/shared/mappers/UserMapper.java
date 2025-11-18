package com.mdevs.trackera.shared.mappers;

import com.mdevs.trackera.dto.user.LoggedUserDTO;
import com.mdevs.trackera.dto.auth.SignUpDTO;
import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.entity.UserEmail;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(SignUpDTO signUpDTO);

    LoggedUserDTO toLoggedUserDTO(User user);

    default String map(UserEmail userEmail) {
        return userEmail != null ? userEmail.getEmail() : null;
    }
}