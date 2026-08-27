package jp.lg.asp.accommodation.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KyodoJigyoshaDto {

    @Size(max = 10)
    private String kyodoAddressNo;

    @Size(max = 200)
    private String kyodoAddress;

    @Size(max = 200)
    private String kyodoName;

    @Size(max = 200)
    private String kyodoNameKana;

    @Size(max = 20)
    private String kyodoPhone;
}
