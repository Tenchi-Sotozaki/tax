package jp.lg.asp.accommodation.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.constant.FukaConstants;
import jp.lg.asp.accommodation.constant.ZeiritsuConstants;
import jp.lg.asp.accommodation.entity.Fuka;
import jp.lg.asp.accommodation.entity.FukaUchi;
import jp.lg.asp.accommodation.entity.ZeiritsuTeigaku;
import jp.lg.asp.accommodation.entity.ZeiritsuTeiritsu;
import jp.lg.asp.accommodation.repository.FukaRepository;
import jp.lg.asp.accommodation.repository.FukaUchiRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeigakuRepository;
import jp.lg.asp.accommodation.repository.ZeiritsuTeiritsuRepository;
import jp.lg.asp.accommodation.service.FukaCommonService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FukaCommonServiceImpl implements FukaCommonService {

	private final FukaRepository fukaRepository;
	private final FukaUchiRepository fukaUchiRepository;
	private final ZeiritsuTeigakuRepository zeiritsuTeigakuRepository;
	private final ZeiritsuTeiritsuRepository zeiritsuTeiritsuRepository;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	@Override
	@Transactional
	public void saveFuka(String shiteiNo, String taishoYm, String teishutsuYmd,
			FukaConstants fukaKbn, String[] dataRow, Map<Integer, String> yoshikiMap,
			String taishoYmPrefix) {

		String nendo = toNendo(taishoYm);
		Integer kibetsu = toKibetsu(taishoYm);
		boolean isTeigaku = FukaConstants.TEIGAKU.equals(fukaKbn);

		// 前履歴取得
		List<Fuka> prevList = fukaRepository.findLatestByNendoAndKibetsu(jichitaiCd, shiteiNo, nendo, kibetsu);
		Fuka prev = prevList.isEmpty() ? null : prevList.get(0);

		int newRno = (prev != null ? prev.getRno() : 0) + 1;
		LocalDate shinkokuYmd = parseDate(teishutsuYmd);
		if (shinkokuYmd == null)
			shinkokuYmd = LocalDate.now();

		// t_fuka 項目インデックス（特例申告は「納入税額－行為年月ｎ－」プレフィックスで検索）
		int kazeiHakusuIdx = isTeigaku
				? findIndexByPrefix(yoshikiMap, taishoYmPrefix + "課税対象宿泊合計【宿泊数】")
				: findIndexByPrefix(yoshikiMap, taishoYmPrefix + "課税対象宿泊合計【宿泊者数】");
		int kazeiRyokinIdx = isTeigaku ? -1
				: findIndexByPrefix(yoshikiMap, taishoYmPrefix + "課税対象宿泊合計【宿泊料金】");
		int zeigakuIdx = findIndexByPrefix(yoshikiMap, taishoYmPrefix + "課税対象宿泊合計【税額】");
		int menjoHakusuIdx = isTeigaku
				? findIndexByPrefix(yoshikiMap, taishoYmPrefix + "課税免除【宿泊数】")
				: findIndexByPrefix(yoshikiMap, taishoYmPrefix + "課税免除【宿泊者数】");
		int menjoRyokinIdx = isTeigaku ? -1
				: findIndexByPrefix(yoshikiMap, taishoYmPrefix + "課税免除【宿泊料金】");
		int totalHakusuIdx = isTeigaku
				? findIndexByPrefix(yoshikiMap, taishoYmPrefix + "合計【宿泊数】")
				: findIndexByPrefix(yoshikiMap, taishoYmPrefix + "合計【宿泊者数】");
		int totalZeigakuIdx = findIndexByPrefix(yoshikiMap, taishoYmPrefix + "合計【税額】");

		Fuka fuka = new Fuka();
		fuka.setJichitaiCd(jichitaiCd);
		fuka.setShiteiNo(shiteiNo);
		fuka.setRno(newRno);
		fuka.setNendo(nendo);
		fuka.setKibetsu(kibetsu);
		fuka.setTorokuYmd(shinkokuYmd);
		fuka.setShinkokuYmd(shinkokuYmd);
		fuka.setTaishoYm(taishoYm);
		fuka.setFukaKbn(fukaKbn.getValue());
		fuka.setHenkoKbn(prev != null ? prev.getHenkoKbn() : FukaConstants.SHINKI.getValue());
		fuka.setKazeiHakusu(parseLong(getDataValue(dataRow, kazeiHakusuIdx)));
		fuka.setKazeiRyokin(parseLong(getDataValue(dataRow, kazeiRyokinIdx)));
		fuka.setZeigaku(parseLong(getDataValue(dataRow, zeigakuIdx)));
		fuka.setMenjoHakusu(parseLong(getDataValue(dataRow, menjoHakusuIdx)));
		fuka.setMenjoRyokin(parseLong(getDataValue(dataRow, menjoRyokinIdx)));
		fuka.setTotalHakusu(parseLong(getDataValue(dataRow, totalHakusuIdx)));
		fuka.setTotalZeigaku(parseLong(getDataValue(dataRow, totalZeigakuIdx)));
		fuka.setKenZeigaku(0L);
		fuka.setCityZeigaku(0L);
		fuka.setKasanKbn(prev != null ? prev.getKasanKbn() : null);
		fuka.setKasanRitsu(prev != null ? prev.getKasanRitsu() : null);
		fuka.setKasanGaku(prev != null ? prev.getKasanGaku() : null);
		fuka.setNokigen(prev != null ? prev.getNokigen() : null);
		fuka.setNewFlg("1");
		fuka.setDelFlg("0");
		fuka = fukaRepository.save(fuka);

		long totalKenZeigaku = 0L;
		// t_fuka_uchi：申告区分１〜１０
		for (int kbn = 1; kbn <= 10; kbn++) {
			String kbnStr = toFullWidth(kbn);
			int hakusuIdx, ryokinSogakuIdx, ryokinIdx, zeiRitsuIdx, uchiZeigakuIdx;
			if (isTeigaku) {
				zeiRitsuIdx = findIndexByPrefix(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【税率】");
				hakusuIdx = findIndexByPrefix(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【宿泊数】");
				uchiZeigakuIdx = findIndexByPrefix(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【税額】");
				ryokinSogakuIdx = -1;
				ryokinIdx = -1;
			} else {
				ryokinSogakuIdx = findIndexByPrefix(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【宿泊料金の総額】");
				hakusuIdx = findIndexByPrefix(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【宿泊者数】");
				ryokinIdx = findIndexByPrefix(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【宿泊料金】");
				zeiRitsuIdx = findIndexByPrefix(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【税率】");
				uchiZeigakuIdx = findIndexByPrefix(yoshikiMap, taishoYmPrefix + "申告区分" + kbnStr + "【税額】");
			}
			if (hakusuIdx < 0 || getDataValue(dataRow, hakusuIdx).isBlank())
				continue;

			// 都道府県税額算出（内訳）
			Long uchiKenZeigaku = null;
			Long uchiCityZeigaku = null;
			if (isTeigaku) {
				Long zeigaku = parseLong(getDataValue(dataRow, zeigakuIdx));
				uchiKenZeigaku = calcKenZeigaku(parseLong(getDataValue(dataRow, ryokinIdx)), taishoYm);
				uchiCityZeigaku = zeigaku - uchiKenZeigaku;
				totalKenZeigaku += uchiCityZeigaku;
			}

			// 税率管理番号
			BigDecimal zeiritsuSeq = null;
			if (isTeigaku) {
				List<ZeiritsuTeigaku> teigakuList = zeiritsuTeigakuRepository
						.findActiveByTaishoKbnAndTekiyoYm(jichitaiCd, ZeiritsuConstants.CITY.getValue(), taishoYm);
				if (teigakuList.size() < kbn) {
					throw new RuntimeException("申告区分" + kbn + "に該当する税率定額詳細マスタが存在しません。");
				}
				zeiritsuSeq = teigakuList.get(kbn - 1).getTeigakuSeq();
			} else {
				Optional<ZeiritsuTeiritsu> teiritsuOpt = zeiritsuTeiritsuRepository
						.findActiveByTaishoKbnAndTekiyoYm(jichitaiCd, ZeiritsuConstants.CITY.getValue(), taishoYm);
				if (teiritsuOpt.isEmpty()) {
					throw new RuntimeException("税率定率詳細マスタに該当データが存在しません。");
				}
				zeiritsuSeq = teiritsuOpt.get().getTeiritsuSeq();
			}

			FukaUchi uchi = new FukaUchi();
			uchi.setJichitaiCd(jichitaiCd);
			uchi.setShiteiNo(shiteiNo);
			uchi.setRno(newRno);
			uchi.setNendo(nendo);
			uchi.setKibetsu(kibetsu);
			uchi.setKazeiKbn(kbn);
			uchi.setZeiritsuSeq(zeiritsuSeq);
			uchi.setFukaKbn(fukaKbn.getValue());
			uchi.setRyokinSogaku(parseLong(getDataValue(dataRow, ryokinSogakuIdx)));
			uchi.setHakusu(parseLong(getDataValue(dataRow, hakusuIdx)));
			uchi.setRyokin(parseLong(getDataValue(dataRow, ryokinIdx)));
			uchi.setZeiRitsu(parseBigDecimal(getDataValue(dataRow, zeiRitsuIdx)));
			uchi.setZeigaku(parseLong(getDataValue(dataRow, uchiZeigakuIdx)));
			uchi.setCityZeigaku(uchiCityZeigaku);
			uchi.setKenZeigaku(uchiKenZeigaku);
			fukaUchiRepository.save(uchi);
		}

		// 都道府県税額算出
		if (isTeigaku) {
			fuka.setKenZeigaku(totalKenZeigaku);
			fuka.setCityZeigaku(fuka.getTotalZeigaku() - totalKenZeigaku);
		} else {
			long totalRyokin = fuka.getKazeiRyokin();
			long totalShukuhakushaSu = fuka.getKazeiHakusu();
			long ryokin = totalShukuhakushaSu > 0 ? totalRyokin / totalShukuhakushaSu : 0;
			long kenZeigaku = calcKenZeigaku(ryokin, taishoYm);
			fuka.setKenZeigaku(kenZeigaku);
			fuka.setCityZeigaku(fuka.getTotalZeigaku() - kenZeigaku);
		}
		fukaRepository.save(fuka);
	}

	@Override
	public Long calcKenZeigaku(Long shukuhakuRyokin, String taishoYM) {
		return 0L;

	}

	/** 行為年月（yyyyMM）→ 年度（yyyy）。3月始まり会計年度。 */
	private String toNendo(String taishoYm) {
		if (taishoYm == null || taishoYm.length() < 6)
			return "";
		int year = Integer.parseInt(taishoYm.substring(0, 4));
		int month = Integer.parseInt(taishoYm.substring(4, 6));
		// 1月・2月は前年度
		return String.valueOf(month <= 2 ? year - 1 : year);
	}

	/** 行為年月（yyyyMM）→ 期別。3月=1期、4月=2期、…、2月=12期。 */
	private Integer toKibetsu(String taishoYm) {
		if (taishoYm == null || taishoYm.length() < 6)
			return null;
		int month = Integer.parseInt(taishoYm.substring(4, 6));
		// 3月→1、4月→2、…、12月→10、1月→11、2月→12
		return month >= 3 ? month - 2 : month + 10;
	}

	/** 申告区分番号を全角数字に変換（様式CSV項目名に合わせる）。 */
	private String toFullWidth(int n) {
		String[] fw = { "", "１", "２", "３", "４", "５", "６", "７", "８", "９", "１０" };
		return n <= 10 ? fw[n] : String.valueOf(n);
	}

	/** 様式マップからCSV項目名称の前方一致でインデックス（0始まり）を返す。見つからない場合は-1。 */
	private int findIndexByPrefix(Map<Integer, String> yoshikiMap, String prefix) {
		return yoshikiMap.entrySet().stream()
				.filter(e -> e.getValue().startsWith(prefix))
				.mapToInt(e -> e.getKey() - 1)
				.findFirst()
				.orElse(-1);
	}

	private String getDataValue(String[] dataRow, int index) {
		if (index < 0 || index >= dataRow.length)
			return "";
		return dataRow[index].trim();
	}

	private LocalDate parseDate(String value) {
		if (value == null || value.isBlank())
			return null;
		for (DateTimeFormatter fmt : List.of(
				DateTimeFormatter.ofPattern("yyyy/MM/dd"),
				DateTimeFormatter.ofPattern("yyyyMMdd"),
				DateTimeFormatter.ISO_LOCAL_DATE)) {
			try {
				return LocalDate.parse(value, fmt);
			} catch (DateTimeParseException ignored) {
			}
		}
		return null;
	}

	private Long parseLong(String value) {
		if (value == null || value.isBlank())
			return null;
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private BigDecimal parseBigDecimal(String value) {
		if (value == null || value.isBlank())
			return null;
		try {
			return new BigDecimal(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
