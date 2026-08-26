package jp.lg.asp.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "m_bank")
@Getter
@Setter
public class MBank extends BaseEntity {

    @Id
    @Column(name = "bank_code", length = 4)
    private String bankCode;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_kana")
    private String bankKana;
}
