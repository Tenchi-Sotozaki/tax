package jp.lg.asp.accommodation.entity;

import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDetailId implements Serializable {
    private String jichitaiCd;
    private Long roleId;
    private String screenId;
}
