package Recommend.Movie.User.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Dto.CheckUserResponseDTO;
import Recommend.Movie.User.Dto.UpdateUserRequestDTO;
import Recommend.Movie.User.Dto.UserFindResponseDTO;
import Recommend.Movie.User.Repository.RefreshRepository;
import Recommend.Movie.User.Repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


@Service
public class UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshRepository refreshRepository;

    public UserService(UserRepository userRepository, JwtService jwtService, RefreshRepository refreshRepository) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshRepository = refreshRepository;
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

    public CheckUserResponseDTO checkUserInfo(int userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다."));

        boolean hasNickname = StringUtils.hasText(user.getNickname());
        boolean hasBirthday = (user.getBirthday() != null);

        if (!hasNickname && !hasBirthday) {
            return CheckUserResponseDTO.builder()
                    .isChecked(false)
                    .missingField("BOTH")
                    .message("닉네임과 생년월일 입력이 필요합니다.")
                    .build();
        }

        if (!hasBirthday) {
            return CheckUserResponseDTO.builder()
                    .isChecked(false)
                    .missingField("BIRTHDAY")
                    .message("생년월일 입력이 필요합니다.")
                    .build();
        }

        if (!hasNickname) {
            return CheckUserResponseDTO.builder()
                    .isChecked(false)
                    .missingField("NICKNAME")
                    .message("닉네임 입력이 필요합니다.")
                    .build();
        }

        return CheckUserResponseDTO.builder()
                .message("모두 입력이 되어있습니다.")
                .isChecked(true)
                .build();
    }

    public String logoutUser(int userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다."));

        refreshRepository.deleteByName(user.getName());
        return "삭제완료됐습니다.";
    }
}
