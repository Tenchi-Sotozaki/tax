package jp.lg.asp.accommodation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Data
public class ConsolidatedDeclarationForm {

    @NotNull(message = "※登録日を入力してください")
    private LocalDate registrationDate;

    /** 内部処理用（hidden で保持） */
    private String obligorId;

    /** 表示用（readonly） */
    private String obligorName;

    @NotNull(message = "※適用時期を入力してください")
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth applicablePeriod;

    @NotEmpty(message = "※施設を1つ以上選択してください")
    private List<String> facilities;
}
