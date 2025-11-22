package Recommend.Movie.Service;

import Recommend.Movie.Converter.UserConverter;
import Recommend.Movie.DTO.GoogleLoginRequest;
import Recommend.Movie.DTO.UserDTO.LoginDTO;
import Recommend.Movie.Domain.SocialProviderType;
import Recommend.Movie.Domain.User;
import Recommend.Movie.Domain.UserRoleType;
import Recommend.Movie.Repository.UserRepository;
import Recommend.Movie.Util.JWTUtil;
import Recommend.Movie.DTO.UserDTO.LoginResponseDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier verifier;
    private final WebClient webClient;


    public AuthService(@Value("${GOOGLE_CLIENT_ID}") String googleClientId,
                       UserRepository userRepository,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;


        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        this.webClient = WebClient.builder().build();
    }

    @Transactional
    public LoginResponseDTO loginWithGoogle(GoogleLoginRequest request) {
        try {
            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                throw new IllegalArgumentException("ID Token is invalid or expired.");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleUniqueId = payload.getSubject();
            String email = payload.getEmail();
            String providerId = "GOOGLE_" + googleUniqueId;
            String name = (String) payload.get("name");

            final boolean[] isNewUser = {false};


            User user = userRepository.findByProviderIdAndIsSocial(providerId, true).orElseGet(() -> {
                isNewUser[0] = true;
                Map<String, String> extraInfo = fetchGooglePeopleInfo(request.getAccessToken());

                LoginDTO dto = LoginDTO.builder()
                        .name(name).providerId(providerId).email(email)
                        .isSocial(true).isLock(false)
                        .socialProviderType(SocialProviderType.GOOGLE).role(UserRoleType.USER)
                        .ageGroup(extraInfo.get("ageGroup"))
                        .gender(extraInfo.get("gender"))
                        .build();

                return userRepository.save(UserConverter.toEntity(dto));
            });


            String accessToken = JWTUtil.createJWT(String.valueOf(user.getUserId()), "ROLE_" + user.getRoleType().name(), true);
            String refreshToken = JWTUtil.createJWT(String.valueOf(user.getUserId()), "ROLE_" + user.getRoleType().name(), false);
            jwtService.addRefresh(String.valueOf(user.getUserId()), refreshToken);
            return new LoginResponseDTO(user.getUserId(), isNewUser[0], accessToken, refreshToken);
        } catch (Exception e) {
            throw new RuntimeException("Login processing failed", e);
        }
    }

    /**
     * Google People API를 호출하여 추가 정보(성별, 연령대)를 Map으로 반환합니다.
     * API 호출 실패 시, 로그만 남기고 빈 Map을 반환하여 가입 절차가 중단되지 않도록 합니다.
     */
    private Map<String, String> fetchGooglePeopleInfo(String googleAccessToken) {
        Map<String, String> extraInfo = new HashMap<>();

        try {
            String personFields = "ageRanges,genders";

            Map<String, Object> response = webClient.get()
                    .uri("https://people.googleapis.com/v1/people/me", uriBuilder ->
                            uriBuilder.queryParam("personFields", personFields).build())
                    .header("Authorization", "Bearer " + googleAccessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null) {
                log.warn("!!!!!! Google People API 응답이 null입니다.");
                return extraInfo;
            }

            // 성별 파싱 (genders는 List 형태)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> genders = (List<Map<String, Object>>) response.get("genders");
            if (genders != null && !genders.isEmpty()) {
                Map<String, Object> genderData = genders.get(0); // 보통 첫 번째 항목
                if (genderData.containsKey("value")) {
                    extraInfo.put("gender", normalizeGender(genderData.get("value").toString()));
                }
            }

            // 연령대 파싱 (ageRanges는 List 형태)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ageRanges = (List<Map<String, Object>>) response.get("ageRanges");
            if (ageRanges != null && !ageRanges.isEmpty()) {
                Map<String, Object> ageRangeData = ageRanges.get(0);
                if (ageRangeData.containsKey("ageRange")) {
                    extraInfo.put("ageGroup", normalizeAgeGroup(ageRangeData.get("ageRange").toString()));
                }
            }


        } catch (Exception e) {
            log.error("!!!!!! Google People API 호출 중 예외 발생 (추가 정보 없이 가입 진행) !!!!!!", e);
        }

        return extraInfo;
    }

    @Transactional
    public LoginResponseDTO loginWithNaver(String accessToken) {
        try {
            String response = webClient.get()
                    .uri("https://openapi.naver.com/v1/nid/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(response, new TypeReference<>() {});

            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) responseMap.get("response");

            if (userInfo == null) {
                throw new IllegalArgumentException("Invalid Naver Token or failed to parse user info.");
            }


            String naverUniqueId = (String) userInfo.get("id");
            String email = (String) userInfo.get("email");
            String providerId = "NAVER_" + naverUniqueId;
            String name = (String) userInfo.get("name");
            String rawAgeGroup = (String) userInfo.get("age");
            String rawGender = (String) userInfo.get("gender");
            String ageGroup = normalizeAgeGroup(rawAgeGroup);
            String gender = normalizeGender(rawGender);

            final boolean[] isNewUser = {false};

            User user = userRepository.findByProviderIdAndIsSocial(providerId, true).orElseGet(() -> {

                isNewUser[0] = true;
                LoginDTO dto = LoginDTO.builder()
                        .name(name).providerId(providerId).email(email)
                        .isSocial(true).isLock(false)
                        .socialProviderType(SocialProviderType.NAVER).role(UserRoleType.USER)
                        .ageGroup(ageGroup)
                        .gender(gender)
                        .build();
                return userRepository.save(UserConverter.toEntity(dto));
            });


            String ourAccessToken = JWTUtil.createJWT(String.valueOf(user.getUserId()), "ROLE_" + user.getRoleType().name(), true);
            String ourRefreshToken = JWTUtil.createJWT(String.valueOf(user.getUserId()), "ROLE_" + user.getRoleType().name(), false);
            jwtService.addRefresh(String.valueOf(user.getUserId()), ourRefreshToken);


            return new LoginResponseDTO(user.getUserId(), isNewUser[0], ourAccessToken, ourRefreshToken);

        } catch (Exception e) {
            throw new RuntimeException("Naver login processing failed", e);
        }

    }
    private String normalizeGender(String rawGender) {
        if (rawGender == null) {
            return null;
        }
        String upper = rawGender.toUpperCase();
        if (upper.equals("MALE") || upper.equals("M")) {
            return "M";
        }
        if (upper.equals("FEMALE") || upper.equals("F")) {
            return "F";
        }
        return null;
    }

    private String normalizeAgeGroup(String rawAgeGroup) {
        if (rawAgeGroup == null) {
            return null;
        }

        if (rawAgeGroup.contains("-")) {
            return rawAgeGroup.split("-")[0];
        }

        if (rawAgeGroup.equals("LESS_THAN_EIGHTEEN")) return "10";
        if (rawAgeGroup.equals("EIGHTEEN_TO_TWENTY")) return "10";
        if (rawAgeGroup.equals("TWENTY_ONE_TO_TWENTY_NINE")) return "20";
        if (rawAgeGroup.equals("THIRTY_TO_THIRTY_NINE")) return "30";
        if (rawAgeGroup.equals("FORTY_TO_FORTY_NINE")) return "40";
        if (rawAgeGroup.equals("FIFTY_TO_FIFTY_NINE")) return "50";
        if (rawAgeGroup.equals("SIXTY_TO_SIXTY_NINE")) return "60";
        if (rawAgeGroup.equals("SEVENTY_TO_SEVENTY_NINE")) return "70";
        if (rawAgeGroup.equals("EIGHTY_TO_EIGHTY_NINE")) return "80";
        if (rawAgeGroup.equals("NINETY_AND_OLDER")) return "90";


        if (rawAgeGroup.contains("TWENTY")) return "20";

        return null;
    }
}