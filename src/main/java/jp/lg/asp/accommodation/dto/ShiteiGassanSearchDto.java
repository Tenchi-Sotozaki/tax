package jp.lg.asp.accommodation.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShiteiGassanSearchDto implements Serializable {
    private String atenaNo;
    private String shiteiNo;
    private String gassanShiteiNo;
    private String name;
    private String shisetsuName;
}
