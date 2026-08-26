package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_branch")
@IdClass(MBranchId.class)
@Getter
@Setter
public class MBranch extends BaseEntity {

    @Id
    @Column(name = "bank_code", length = 4)
    private String bankCode;

    @Id
    @Column(name = "branch_code", length = 3)
    private String branchCode;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "branch_kana")
    private String branchKana;
}
