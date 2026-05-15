package jp.lg.asp.accommodation.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChoshuGenboUchiId implements Serializable {
    // 💡 このエンティティの本当の主キーだけを定義する
    private String jichitaiCd;
    private Long uchiIdx;
}