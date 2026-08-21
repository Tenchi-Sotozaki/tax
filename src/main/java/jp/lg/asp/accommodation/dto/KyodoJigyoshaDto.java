package jp.lg.asp.accommodation.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KyodoJigyoshaDto {

    @Size(max = 10, message = "共同事業者情報の郵便番号は10文字以内で入力してください")
    private String kyodoAddressNo;

    @Size(max = 200, message = "共同事業者情報の住所は200文字以内で入力してください")
    private String kyodoAddress;

    @Size(max = 200, message = "共同事業者情報の氏名は200文字以内で入力してください")
    private String kyodoName;

    @Size(max = 200, message = "共同事業者情報の氏名(ふりがな)は200文字以内で入力してください")
    private String kyodoNameKana;

    @Size(max = 20, message = "共同事業者情報の電話番号は20文字以内で入力してください")
    private String kyodoPhone;
}
