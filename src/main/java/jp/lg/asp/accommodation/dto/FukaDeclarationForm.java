package jp.lg.asp.accommodation.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * 宿泊税情報登録/編集(照会)画面用 Formクラス
 * 画面とController間で申告データを受け渡す役割を持つ
 */
@Data
public class FukaDeclarationForm {

	// ========== 定数 ==========
	// ※初期値が必要な項目などがある場合は規約に則り定数で定義（例として）
	private static final int DEFAULT_AMOUNT = 0;

	// ========== 制御用フィールド ==========
	// 申告記録のID（新規登録時はnull）
	private Long declarationId;

	// 特別徴収義務者の指定番号
	private String shiteiNo;

	// 編集モード判定フラグ
	private boolean isEdit;

	// 照会モード判定フラグ（全項目リードオンリー制御用）
	private boolean isView;

	// ========== 納税額情報エリア ==========
	// 登録日
    @DateTimeFormat(pattern = "yyyy-MM-dd")
	@NotNull(message = "登録日を入力してください")
	private LocalDate registrationDate;

	// 特別徴収義務者 (リードオンリー)
	private String obligorName;

	// 宿泊施設名称 (リードオンリー)
	private String facilityName;
	

	@Valid
	private FukaMonthlyDeclarationDto monthlyDetail = new FukaMonthlyDeclarationDto();

	// ========== 加算金額入力エリア ==========
	// 加算金額の区分 (過少申告加算金/不申告加算金/重加算金)
	private String additionalCategory;

	// 加算金額の割合
	private String additionalRate;

	// 加算金額
	private Long additionalAmount;

	// 加算金額の納期限
	@org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd")
	private java.time.LocalDate additionalDueDate;

	// ========== 更生/修正エリア ==========
	// 変更の区分 (更生/修正)
	private String modificationCategory;
	
	// 変更理由
	private String modificationReason;
	
	// 年度
	private String nendo; 
	
	// 期別
    private Integer kibetsu; 
    

    // 徴収原簿用の日別データ（モーダルの入力値）
    private FukaMonthlyTallyDto monthlyTally = new FukaMonthlyTallyDto();

    private Integer rno;
    
    private boolean taxCheckBypassed = false;


}
