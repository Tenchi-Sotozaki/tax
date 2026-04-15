package jp.lg.asp.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddressDto {
    private String addressNumber;
    private String name;
    private String nameKana;
    private String address;
    private String phone;
}
