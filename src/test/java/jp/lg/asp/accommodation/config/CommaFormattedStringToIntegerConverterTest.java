package jp.lg.asp.accommodation.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommaFormattedStringToIntegerConverterTest {

    private CommaFormattedStringToIntegerConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CommaFormattedStringToIntegerConverter();
    }

    @Test
    void convert_空文字はnullを返す() {
        assertThat(converter.convert("")).isNull();
    }

    @Test
    void convert_スペースのみはnullを返す() {
        assertThat(converter.convert("   ")).isNull();
    }

    @Test
    void convert_カンマなし数値を変換できる() {
        assertThat(converter.convert("1234")).isEqualTo(1234);
    }

    @Test
    void convert_カンマあり数値を変換できる() {
        assertThat(converter.convert("1,234,567")).isEqualTo(1234567);
    }

    @Test
    void convert_ゼロを変換できる() {
        assertThat(converter.convert("0")).isEqualTo(0);
    }

    @Test
    void convert_負の数を変換できる() {
        assertThat(converter.convert("-1000")).isEqualTo(-1000);
    }

    @Test
    void convert_不正な文字列は例外をスローする() {
        assertThatThrownBy(() -> converter.convert("abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convert_数値と文字の混在は例外をスローする() {
        assertThatThrownBy(() -> converter.convert("123abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
