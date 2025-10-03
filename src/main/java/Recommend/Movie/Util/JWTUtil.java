package Recommend.Movie.Util;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component // Spring이 이 클래스를 관리하도록 @Component를 추가합니다.
public class JWTUtil {

    private static SecretKey secretKey;
    private static final Long accessTokenExpiresIn = 3600L * 1000; // 1시간
    private static final Long refreshTokenExpiresIn = 604800L * 1000; // 7일


    @Value("${SECRET_KEY}")
    public void setSecretKey(String secret) {
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }


    public static String getUsername(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("sub", String.class);
    }
    public static String getRole(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }
    public static Boolean isValid(String token, Boolean isAccess) {
        try {
            String type = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("type", String.class);
            return isAccess ? type.equals("access") : type.equals("refresh");
        } catch (Exception e) {
            return false;
        }
    }
    public static String createJWT(String name, String role, Boolean isAccess) {
        long now = System.currentTimeMillis();
        long expiry = isAccess ? accessTokenExpiresIn : refreshTokenExpiresIn;
        String type = isAccess ? "access" : "refresh";
        return Jwts.builder()
                .claim("sub", name)
                .claim("role", role)
                .claim("type", type)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiry))
                .signWith(secretKey)
                .compact();
    }
}