package jp.lg.asp.accommodation.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KyodoJigyoshaDto {

    @Size(max = 10, message = "10文字以内で入力してください")
    @Pattern(regexp = "^$|^[0-9]{3}-?[0-9]{4}$", message = "半角数字とハイフンで、1234567 または 123-4567 の形式で入力してください")
    private String kyodoAddressNo;

    @Size(max = 200, message = "200文字以内で入力してください")
    private String kyodoAddress;

    @Size(max = 200, message = "200文字以内で入力してください")
    private String kyodoName;

    @Size(max = 200, message = "200文字以内で入力してください")
    @Pattern(regexp = "^[\\u3041-\\u3096\\u309D\\u309E\\u30A1-\\u30F6\\u30FD\\u30FEー\u3000 ]*$", message = "氏名(ふりがな)はひらがなまたはカタカナで入力してください")
    private String kyodoNameKana;

    @Size(max = 20, message = "20文字以内で入力してください")
    @Pattern(regexp = "^[0-9-]*$", message = "半角数字とハイフンで入力してください")
    private String kyodoPhone;
}
