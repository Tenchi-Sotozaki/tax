package jp.lg.asp.accommodation.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HashUtil {

    @Value("${app.hash.salt}")
    private String salt;

    public String sha256(String value) {
        try {
            String salted = salt + value;
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(salted.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("ハッシュ化に失敗しました", e);
        }
    }
}
