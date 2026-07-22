package jp.lg.asp.accommodation.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class HashUtilTest {

    private HashUtil hashUtil;

    @BeforeEach
    void setUp() {
        hashUtil = new HashUtil();
        ReflectionTestUtils.setField(hashUtil, "salt", "testSalt");
    }

    @Test
    void sha256_正常にハッシュ化される() {
        String result = hashUtil.sha256("password");
        assertThat(result).isNotNull().hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void sha256_同じ入力は同じハッシュを返す() {
        String result1 = hashUtil.sha256("password");
        String result2 = hashUtil.sha256("password");
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void sha256_異なる入力は異なるハッシュを返す() {
        String result1 = hashUtil.sha256("password1");
        String result2 = hashUtil.sha256("password2");
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void sha256_ソルトが異なると異なるハッシュを返す() {
        String result1 = hashUtil.sha256("password");

        HashUtil otherUtil = new HashUtil();
        ReflectionTestUtils.setField(otherUtil, "salt", "otherSalt");
        String result2 = otherUtil.sha256("password");

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void sha256_空文字でもハッシュ化される() {
        String result = hashUtil.sha256("");
        assertThat(result).isNotNull().hasSize(64);
    }
}
