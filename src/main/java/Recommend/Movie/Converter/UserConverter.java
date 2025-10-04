package Recommend.Movie.Converter;

import Recommend.Movie.DTO.UserDTO.LoginDTO;
import Recommend.Movie.DTO.UserDTO.UpdateDTO;
import Recommend.Movie.Domain.User;

public class UserConverter {

    public static User toEntity(LoginDTO dto){
        return User.builder()
                .name(dto.getName())
                .providerId(dto.getProviderId())
                .email(dto.getEmail())
                .socialProviderType(dto.getSocialProviderType())
                .isLock(dto.isLock())
                .isSocial(dto.isSocial())
                .roleType(dto.getRole())
                .build();
    }
    public static User updateUser(UpdateDTO dto){
        return User.builder()
                .providerId(dto.getProviderId())
                .email(dto.getEmail())
                .build();
    }
}
