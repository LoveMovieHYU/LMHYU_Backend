package Recommend.Movie.User.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.LikeMovie.Repository.LikeMovieRepository;
import Recommend.Movie.User.Domain.Gender;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Dto.CheckUserResponseDTO;
import Recommend.Movie.User.Dto.FinalLoginDTO;
import Recommend.Movie.User.Dto.UpdateUserRequestDTO;
import Recommend.Movie.User.Dto.UserFindResponseDTO;
import Recommend.Movie.User.Repository.RefreshRepository;
import Recommend.Movie.User.Repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
public class UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final LikeMovieRepository likeMovieRepository;
    private final RefreshRepository refreshRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public UserService(UserRepository userRepository, JwtService jwtService, LikeMovieRepository likeMovieRepository,
                       RefreshRepository refreshRepository, RedisTemplate<String, Object> redisTemplate) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.likeMovieRepository = likeMovieRepository;
        this.refreshRepository = refreshRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void deleteUser(int userId) {
        User user = getUserByUserId(userId);
        likeMovieRepository.deleteAllByUserId(userId);
        jwtService.removeRefreshUser(user.getName());
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public UserFindResponseDTO readUser(int userId) {
        User user = getUserByUserId(userId);
        return new UserFindResponseDTO(user.getName(), user.getEmail(), user.getNickname());
    }

    @Transactional
    public String updateUserInfo(int userId, UpdateUserRequestDTO requestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다."));

        if (user.getNickname() != null && user.getNickname().equals(requestDTO.getNickName())) {
            throw new BusinessException(ErrorCode.SAME_NICKNAME, "닉네임이 중복됐습니다.");
        }

        if (requestDTO.getBirthday() != null) {
            user.setBirthday(requestDTO.getBirthday());
            deleteUserBiorhythmCaches(userId);
        }

        if (requestDTO.getNickName() != null) {
            user.setNickname(requestDTO.getNickName());
        }

        userRepository.save(user);
        return "수정 완료됐습니다.";
    }

    @Transactional
    public String loginUserUpdate(int userId, FinalLoginDTO requestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다."));

        boolean isDuplicate = userRepository.existsByNickname(requestDTO.getNickName());
        if (isDuplicate && !requestDTO.getNickName().equals(user.getNickname())) {
            throw new BusinessException(ErrorCode.SAME_NICKNAME, "이미 사용 중인 닉네임입니다.");
        }

        try {
            user.setGender(Gender.valueOf(requestDTO.getGender()));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "올바르지 않은 성별 값입니다.");
        }

        user.setNickname(requestDTO.getNickName());
        user.setBirthday(requestDTO.getBirthday());

        userRepository.save(user);
        return "회원가입 완료됐습니다.";
    }

    public CheckUserResponseDTO checkUserInfo(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다."));

        boolean hasNickname = StringUtils.hasText(user.getNickname());
        boolean hasBirthday = user.getBirthday() != null;

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

    public String logoutUser(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다."));

        refreshRepository.deleteByName(user.getName());
        return "로그아웃 됐습니다.";
    }

    private void deleteUserBiorhythmCaches(int userId) {
        String bioPattern = "biorhythm:" + userId + ":*";
        String moviePattern = "recommend:biorhythm:" + userId + ":*";

        Set<String> bioKeys = redisTemplate.keys(bioPattern);
        Set<String> movieKeys = redisTemplate.keys(moviePattern);

        if (bioKeys != null && !bioKeys.isEmpty()) {
            redisTemplate.delete(bioKeys);
        }

        if (movieKeys != null && !movieKeys.isEmpty()) {
            redisTemplate.delete(movieKeys);
        }
    }

    private User getUserByUserId(int userId) {
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다.");
        }
        return user;
    }
}
