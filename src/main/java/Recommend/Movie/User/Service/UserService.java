package Recommend.Movie.User.Service;

import Recommend.Movie.Config.Exception.UserNotFoundExceptionHandler;
import Recommend.Movie.User.Domain.User;
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
        } catch (UserNotFoundExceptionHandler ex){
            throw new UserNotFoundExceptionHandler("유저를 찾을 수 없습니다.");
        }
    }

    // 자체/소셜 유저 정보 조회
    @Transactional(readOnly = true)
    public UserFindResponseDTO readUser(int userId) {
        try{
            User user = userRepository.findByUserId(userId);
            UserFindResponseDTO responseDTO = new UserFindResponseDTO(user.getName(), user.getEmail());
            return responseDTO;
        } catch (UserNotFoundExceptionHandler ex){
            throw new UserNotFoundExceptionHandler("User not found.");
        }
    }

    @Transactional
    public void updateNickname(int userId, String newNickname) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다"));

        if (user.getNickname() != null && user.getNickname().equals(newNickname)) {
            throw new IllegalArgumentException("Nickname is the same");
        }

        if (userRepository.existsByNickname(newNickname)) {
            throw new IllegalArgumentException("Nickname is taken");
        }

        user.setNickname(newNickname);
    }
}
