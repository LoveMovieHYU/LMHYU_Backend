package Recommend.Movie.User.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Dto.UpdateUserRequestDTO;
import Recommend.Movie.User.Dto.UserFindResponseDTO;
import Recommend.Movie.User.Repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.stereotype.Service;



@Service
public class UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    // 자체/소셜 로그인 회원 탈퇴
    @Transactional
    public void deleteUser(int userId) {
        try{
            User user = userRepository.findByUserId(userId);
            jwtService.removeRefreshUser(user.getName());
            // 유저 삭제
            userRepository.delete(user);
        } catch (BusinessException ex){
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다.");
        }
    }

    // 자체/소셜 유저 정보 조회
    @Transactional(readOnly = true)
    public UserFindResponseDTO readUser(int userId) {
        try{
            User user = userRepository.findByUserId(userId);
            UserFindResponseDTO responseDTO = new UserFindResponseDTO(user.getName(), user.getEmail());
            return responseDTO;
        } catch (BusinessException ex){
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다.");
        }
    }


    @Transactional
    public String updateUserInfo(int userId, UpdateUserRequestDTO requestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다."));
        if (user.getNickname() != null && user.getNickname().equals(requestDTO.getNickName())) {
            throw new BusinessException(ErrorCode.SAME_NICKNAME, "닉네임이 중복됐습니다.");
        }

        user.setBirthday(requestDTO.getBirthday());
        user.setNickname(requestDTO.getNickName());
        userRepository.save(user);
        return "업데이트가 완료됐습니다.";
    }
}
