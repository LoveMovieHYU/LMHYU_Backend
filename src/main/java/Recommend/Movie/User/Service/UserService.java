package Recommend.Movie.User.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.LikeMovie.Repository.LikeMovieRepository;
import Recommend.Movie.User.Converter.UserConverter;
import Recommend.Movie.User.Domain.Gender;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Dto.CheckUserResponseDTO;
import Recommend.Movie.User.Dto.FinalLoginDTO;
import Recommend.Movie.User.Dto.UpdateUserRequestDTO;
import Recommend.Movie.User.Dto.UserFindResponseDTO;
import Recommend.Movie.User.Repository.RefreshRepository;
import Recommend.Movie.User.Repository.UserRepository;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
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
        return UserConverter.toDTO(user);
    }

    @Transactional
    public String updateUserInfo(int userId, UpdateUserRequestDTO requestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다."));

        if (requestDTO.getBirthday() != null) {
            user.updateBirthday(requestDTO.getBirthday());
            deleteUserBiorhythmCaches(userId);
        }

        if (requestDTO.getNickName() != null) {
            // 본인 닉네임을 그대로 재제출하는 경우는 정상 통과, 다른 유저가 쓰는 닉네임이면 중복 예외
            boolean isDuplicate = userRepository.existsByNickname(requestDTO.getNickName());
            if (isDuplicate && !requestDTO.getNickName().equals(user.getNickname())) {
                throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME, "이미 사용 중인 닉네임입니다.");
            }
            user.updateNickname(requestDTO.getNickName());
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
            user.updateGender(Gender.valueOf(requestDTO.getGender()));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "올바르지 않은 성별 값입니다.");
        }

        user.updateNickname(requestDTO.getNickName());
        user.updateBirthday(requestDTO.getBirthday());

        userRepository.save(user);
        return "회원가입 완료됐습니다.";
    }

    public CheckUserResponseDTO checkUserInfo(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다."));

        boolean hasNickname = StringUtils.hasText(user.getNickname());
        boolean hasBirthday = user.getBirthday() != null;

        if (!hasNickname && !hasBirthday) {
            return UserConverter.toDTO(false, "BOTH", "닉네임과 생년월일 입력이 필요합니다.");
        }

        if (!hasBirthday) {
            return UserConverter.toDTO(false, "BIRTHDAY", "생년월일 입력이 필요합니다.");
        }

        if (!hasNickname) {
            return UserConverter.toDTO(false, "NICKNAME", "닉네임 입력이 필요합니다.");
        }

        return UserConverter.toDTO(true, null, "모두 입력이 되어있습니다.");
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

        Set<String> bioKeys = scanKeys(bioPattern);
        Set<String> movieKeys = scanKeys(moviePattern);

        if (!bioKeys.isEmpty()) {
            redisTemplate.delete(bioKeys);
        }

        if (!movieKeys.isEmpty()) {
            redisTemplate.delete(movieKeys);
        }
    }

    /**
     * SCAN 기반으로 패턴에 매칭되는 Redis 키를 조회한다.
     * keys(pattern) 은 O(N) 블로킹이라 SCAN(non-blocking) 으로 대체한다.
     */
    private Set<String> scanKeys(String pattern) {
        return redisTemplate.execute((RedisConnection connection) -> {
            Set<String> keys = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return keys;
        });
    }

    private User getUserByUserId(int userId) {
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다.");
        }
        return user;
    }
}
