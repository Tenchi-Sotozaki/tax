package jp.lg.asp.accommodation.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jp.lg.asp.accommodation.dto.FukaDaichoForm;
import jp.lg.asp.accommodation.dto.FukaDeclarationForm;
import jp.lg.asp.accommodation.dto.FukaMonthlyDeclarationDto;

/**
 * 宿泊税納入（賦課）に関するビジネスロジックのインターフェース。
 */
public interface FukaService {

	/**
	 * 納入金額管理台帳のデータを取得する。
	 */
	FukaDaichoForm getDaichoData(String shiteiNo, String nendo, String status);

	/**
	 * 新規登録用の初期表示データを取得する。
	 */
	FukaDeclarationForm getDeclarationFormForRegister(String shiteiNo, String paymentMonth);

	/**
	 * 編集・照会用の表示データを取得する。
	 */
	FukaDeclarationForm getDeclarationFormForEdit(String shiteiNo, String nendo, Integer kibetsu);

	/**
	 * 照会用の表示データを取得する。
	 */
	FukaDeclarationForm getDeclarationFormForView(String shiteiNo, String nendo, Integer kibetsu);

	/**
	 * rno指定で照会用の表示データを取得する。
	 */
	FukaDeclarationForm getDeclarationFormForViewByRno(String shiteiNo, String nendo, Integer kibetsu, Integer rno);

	/**
	 * 宿泊税情報の保存処理を実行する。
	 */
	void saveDeclaration(FukaDeclarationForm form);

	/**
	 * 指定された条件のデータが登録済みか判定する。
	 */
	boolean isAlreadyRegistered(String shiteiNo, String paymentMonth);

	/**
	 * 指定された年度・期別に該当する申告データが存在するか判定する。
	 */
	boolean isAlreadyRegisteredByKibetsu(String shiteiNo, String nendo, Integer kibetsu);

	/**
	 * 賦課区分に応じた税額計算を行う。
	 */
	long calculateTax(String fukaKbn, long baseValue, BigDecimal cityRate, BigDecimal kenRate);

	/**
	 * 指定された指定番号に該当するデータが存在する年度の一覧を取得する。
	 */
	List<Integer> getExistingNendoList(String shiteiNo);

	/**
	 * 対象年月が合算納入対象月か判定する。
	 * @param gassanShiteiNo 合算指定番号
	 * @param taishoDate 対象年月の1日
	 * @return 合算納入対象月の場合true
	 */
	boolean isGassanTargetMonth(String gassanShiteiNo, LocalDate taishoDate);

	/**
	 * 指定番号が合算申告の構成員として合算対象月かを判定する。
	 * @param shiteiNo 指定番号
	 * @param taishoDate 対象年月の1日
	 * @return 合算対象月の場合true
	 */
	boolean isShiteiNoGassanTargetMonth(String shiteiNo, LocalDate taishoDate);

	/**
	 * 指定番号が合算対象月の場合、該当する合算レコードの適用期間文字列を返す。
	 * 対象外の場合は null を返す。
	 * @param shiteiNo 指定番号
	 * @param taishoDate 対象年月の1日
	 * @return 「○年○月～○年○月」形式の文字列、対象外の場合null
	 */
	String resolveGassanTekiyoPeriod(String shiteiNo, LocalDate taishoDate);

	/**
	 * 保存前に、入力中の内容から市区町村税額・都道府県税額の内訳を試算する（内訳試算ボタン用）。
	 * 定額制・定率制のどちらにも対応する。
	 * @param fukaKbn 賦課区分コード（"1"=定額, "2"=定率）
	 * @param monthlyDetail 画面入力中の申告情報
	 * @return 内訳（cityZeigaku/kenZeigaku）を設定した申告情報
	 */
	FukaMonthlyDeclarationDto estimateBreakdown(String fukaKbn, FukaMonthlyDeclarationDto monthlyDetail);
}