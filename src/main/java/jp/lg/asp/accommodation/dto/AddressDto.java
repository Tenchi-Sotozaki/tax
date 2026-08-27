package jp.lg.asp.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {
    private String addressNumber;
    private String name;
    private String nameKana;
    private String yubinNo;
    private String address;
    private String phone;
    private String kojinNo;
    private String hojinNo;
    
    private boolean alreadyRegistered;
    private boolean viewOnly;
    private String gassanShiteiNo;
    private String tekiyoEdYmd;
}