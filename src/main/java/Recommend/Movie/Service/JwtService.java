package Recommend.Movie.Service;

import Recommend.Movie.DTO.JWTResponseDTO;
import Recommend.Movie.Domain.RefreshEntity;
import Recommend.Movie.Repository.RefreshRepository;
import Recommend.Movie.Util.JWTUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class JwtService {

    private final RefreshRepository refreshRepository;

    public JwtService(RefreshRepository refreshRepository) {
        this.refreshRepository = refreshRepository;
    }

    /**
     * Access Token 재발급 (Refresh Token Rotation)
     * @param refreshToken 헤더에 담겨 온 리프레시 토큰
     * @return 새로운 Access Token과 Refresh Token
     */
    @Transactional
    public JWTResponseDTO refreshRotate(String refreshToken) {

        // 1. Refresh 토큰 자체의 유효성 검증
        Boolean isValid = JWTUtil.isValid(refreshToken, false);
        if (!isValid) {
            throw new RuntimeException("유효하지 않은 refreshToken입니다.");
        }

        if (!existsRefresh(refreshToken)) {
            throw new RuntimeException("DB에 존재하지 않는 refreshToken입니다.");
        }

        String name = JWTUtil.getUsername(refreshToken);
        String role = JWTUtil.getRole(refreshToken);

        String newAccessToken = JWTUtil.createJWT(name, role, true);
        String newRefreshToken = JWTUtil.createJWT(name, role, false);

        removeRefresh(refreshToken);
        addRefresh(name, newRefreshToken);

        return new JWTResponseDTO(newAccessToken, newRefreshToken);
    }

    /**
     * JWT Refresh 토큰을 DB에 저장 또는 업데이트하는 메소드
     * @param name 사용자 이름 (고유 식별자)
     * @param refreshToken 새로 발급된 리프레시 토큰
     */
    @Transactional
    public void addRefresh(String name, String refreshToken) {
        // 핵심 로직: 사용자의 기존 토큰이 있으면 업데이트(덮어쓰기), 없으면 새로 저장
        Optional<RefreshEntity> existingToken = refreshRepository.findByName(name);

        if (existingToken.isPresent()) {
            // 기존 토큰이 있다면, 새 리프레시 토큰으로 값을 업데이트
            existingToken.get().updateRefresh(refreshToken);
        } else {
            // 기존 토큰이 없다면, 새로 생성해서 저장
            RefreshEntity entity = RefreshEntity.builder()
                    .name(name)
                    .refresh(refreshToken)
                    .build();
            refreshRepository.save(entity);
        }
    }

    /**
     * DB에 해당 Refresh 토큰이 존재하는지 확인
     */
    @Transactional(readOnly = true)
    public Boolean existsRefresh(String refreshToken) {
        return refreshRepository.existsByRefresh(refreshToken);
    }

    /**
     * Refresh 토큰 값을 기준으로 DB에서 삭제
     */
    @Transactional
    public void removeRefresh(String refreshToken) {
        refreshRepository.deleteByRefresh(refreshToken);
    }

    /**
     * 특정 유저의 모든 Refresh 토큰 삭제 (회원 탈퇴 시 사용)
     */
    @Transactional
    public void removeRefreshUser(String name) {
        refreshRepository.deleteByName(name);
    }
}