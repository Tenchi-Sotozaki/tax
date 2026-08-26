package jp.lg.asp.accommodation.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MBranchId implements Serializable {
    private String bankCode;
    private String branchCode;
}
