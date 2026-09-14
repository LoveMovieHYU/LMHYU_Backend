package Recommend.Movie.User.Converter;

import Recommend.Movie.User.Dto.CheckUserResponseDTO;
import Recommend.Movie.User.Dto.LoginDTO;
import Recommend.Movie.User.Dto.UpdateDTO;
import Recommend.Movie.User.Dto.UserFindResponseDTO;
import Recommend.Movie.User.Domain.User;

import java.time.LocalDate;

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
                .createAt(LocalDate.now())
                .build();
    }
    public static User updateUser(UpdateDTO dto){
        return User.builder()
                .providerId(dto.getProviderId())
                .email(dto.getEmail())
                .build();
    }

    /**
     * User 엔티티 -> 유저 정보 조회 응답 DTO 변환
     */
    public static UserFindResponseDTO toDTO(User user){
        return new UserFindResponseDTO(user.getName(), user.getEmail(), user.getNickname());
    }

    /**
     * 회원 필수 정보 입력 여부 확인 응답 DTO 조립
     */
    public static CheckUserResponseDTO toDTO(boolean isChecked, String missingField, String message){
        return CheckUserResponseDTO.builder()
                .isChecked(isChecked)
                .missingField(missingField)
                .message(message)
                .build();
    }
}
