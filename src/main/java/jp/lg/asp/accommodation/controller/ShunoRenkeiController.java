package jp.lg.asp.accommodation.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.ShunoDto;
import jp.lg.asp.accommodation.service.ShunoRenkeiService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/shunoRenkei")
@RequiredArgsConstructor
public class ShunoRenkeiController {

	private final ScreenAccessChecker accessChecker;
	private final ShunoRenkeiService shunoRenkeiService;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	private static final String SCREEN_ID = ScreenManagement.SHUNO_RENKEI;

	@GetMapping("/list")
	@OpeLog(screenId = SCREEN_ID, operation = "一覧表示")
	public String index(
			@RequestParam(required = false) String shinkokuFrom,
			@RequestParam(required = false) String shinkokuTo,
			@RequestParam(required = false) String taishoMonth,
			@RequestParam(required = false) String shiteiNo,
			@RequestParam(required = false) String name,
			@RequestParam(required = false, defaultValue = "partial") String nameMatchType,
			Model model) {

		accessChecker.checkAccess(SCREEN_ID);
		LocalDate from = shinkokuFrom == null || shinkokuFrom.isEmpty() ? null
				: LocalDate.parse(shinkokuFrom);
		LocalDate to = shinkokuTo == null || shinkokuTo.isEmpty() ? null
				: LocalDate.parse(shinkokuTo);
		List<ShunoDto> items = shunoRenkeiService.search(jichitaiCd, from, to, taishoMonth, shiteiNo, name, nameMatchType);

		model.addAttribute("items", items);
		Map<String, Object> searchForm = new HashMap<>();
		searchForm.put("shinkokuFrom", shinkokuFrom);
		searchForm.put("shinkokuTo", shinkokuTo);
		searchForm.put("taishoMonth", taishoMonth);
		searchForm.put("shiteiNo", shiteiNo);
		searchForm.put("name", name);
		searchForm.put("nameMatchType", nameMatchType);
		model.addAttribute("searchForm", searchForm);

		return "renkei/shunoRenkei";
	}

	@GetMapping("/search")
	@ResponseBody
	@OpeLog(screenId = SCREEN_ID, operation = "検索")
	public List<ShunoDto> search(
			@RequestParam(required = false) String shinkokuFrom,
			@RequestParam(required = false) String shinkokuTo,
			@RequestParam(required = false) String taishoMonth,
			@RequestParam(required = false) String shiteiNo,
			@RequestParam(required = false) String name,
			@RequestParam(required = false, defaultValue = "partial") String nameMatchType) {

		accessChecker.checkAccess(SCREEN_ID);
		LocalDate from = shinkokuFrom == null || shinkokuFrom.isEmpty() ? null
				: LocalDate.parse(shinkokuFrom);
		LocalDate to = shinkokuTo == null || shinkokuTo.isEmpty() ? null
				: LocalDate.parse(shinkokuTo);
		return shunoRenkeiService.search(jichitaiCd, from, to, taishoMonth, shiteiNo, name, nameMatchType);
	}

	@PostMapping("/download")
	@OpeLog(screenId = SCREEN_ID, operation = "ダウンロード")
	public ResponseEntity<byte[]> downloadCsv(@RequestBody List<ShunoDto.Key> keys) {
		accessChecker.checkAccess(SCREEN_ID);
		List<ShunoDto> rows = shunoRenkeiService.findByKeys(jichitaiCd, keys);

		String[] csvHeaders = { "宛名番号", "賦課年度", "期別", "登録年月日", "申告年月日", "対象年月", "合計税額", "市区町村税額", "都道府県税額",
				"加算金額区分1", "加算割合1", "加算金額1", "納期限1", "加算金額区分2", "加算割合2", "加算金額2", "納期限2", "加算金額区分3", "加算割合3", "加算金額3",
				"納期限3" };
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < csvHeaders.length; i++) {
			if (i > 0)
				sb.append(',');
			sb.append('"').append(csvHeaders[i].replace("\"", "\"\"")).append('"');
		}
		sb.append('\n');

		for (ShunoDto r : rows) {
			String kasanKbnName1 = convertKasanKbn(r.getKasanKbn1());
			String kasanKbnName2 = convertKasanKbn(r.getKasanKbn2());
			String kasanKbnName3 = convertKasanKbn(r.getKasanKbn3());
			String[] cols = new String[] {
					r.getAtenaNo() != null ? r.getAtenaNo() : "",
					r.getNendo() != null ? r.getNendo() : "",
					r.getKibetsu() != null ? String.valueOf(r.getKibetsu()) : "",
					r.getTorokuYmd() != null ? r.getTorokuYmd().toString() : "",
					r.getShinkokuYmd() != null ? r.getShinkokuYmd().toString() : "",
					r.getTaishoYm() != null ? formatTaishoYm(r.getTaishoYm()) : "",
					r.getTotalZeigaku() != null ? String.valueOf(r.getTotalZeigaku()) : "",
					r.getCityZeigaku() != null ? String.valueOf(r.getCityZeigaku()) : "",
					r.getKenZeigaku() != null ? String.valueOf(r.getKenZeigaku()) : "",
					kasanKbnName1,
					r.getKasanRitsu1() != null ? r.getKasanRitsu1().toString() : "",
					r.getKasanGaku1() != null ? String.valueOf(r.getKasanGaku1()) : "",
					r.getNokigen1() != null ? r.getNokigen1().toString() : "",
					kasanKbnName2,
					r.getKasanRitsu2() != null ? r.getKasanRitsu2().toString() : "",
					r.getKasanGaku2() != null ? String.valueOf(r.getKasanGaku2()) : "",
					r.getNokigen2() != null ? r.getNokigen2().toString() : "",
					kasanKbnName3,
					r.getKasanRitsu3() != null ? r.getKasanRitsu3().toString() : "",
					r.getKasanGaku3() != null ? String.valueOf(r.getKasanGaku3()) : "",
					r.getNokigen3() != null ? r.getNokigen3().toString() : "",
			};
			for (int i = 0; i < cols.length; i++) {
				if (i > 0)
					sb.append(',');
				sb.append('"').append(cols[i].replace("\"", "\"\"")).append('"');
			}
			sb.append('\n');
		}

		byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}; // UTF-8 BOM
		byte[] csvData = sb.toString().getBytes(StandardCharsets.UTF_8);
		byte[] body = new byte[bom.length + csvData.length];
		System.arraycopy(bom, 0, body, 0, bom.length);
		System.arraycopy(csvData, 0, body, bom.length, csvData.length);
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.parseMediaType("text/csv;charset=utf-8"));
		httpHeaders.setContentDisposition(ContentDisposition.attachment().filename("shuno_renkei.csv").build());
		return ResponseEntity.ok().headers(httpHeaders).body(body);
	}

	@PostMapping("/kakunin")
	@OpeLog(screenId = SCREEN_ID, operation = "確認")
	public String kakunin(@RequestParam("keysJson") String keysJson, Model model) {
		com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
		try {
			accessChecker.checkAccess(SCREEN_ID);
			List<ShunoDto.Key> keys = om.readValue(keysJson,
					om.getTypeFactory().constructCollectionType(List.class, ShunoDto.Key.class));
			List<ShunoDto> rows = shunoRenkeiService.findByKeys(jichitaiCd, keys);
			model.addAttribute("rows", rows);
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("rows", java.util.Collections.emptyList());
		}
		return "renkei/shunoRenkeiKakunin";
	}

	private String formatTaishoYm(String taishoYm) {
		if (taishoYm == null || taishoYm.isEmpty()) {
			return "";
		}
		String[] parts = taishoYm.split("-");
		if (parts.length == 2) {
			return parts[0] + "年" + parts[1] + "月";
		}
		return taishoYm;
	}

	private String convertKasanKbn(String kasanKbn) {
		if (kasanKbn == null) {
			return "";
		}
		switch (kasanKbn) {
		case "1":
			return "過少申告加算金";
		case "2":
			return "不申告加算金";
		case "3":
			return "重加算金";
		default:
			return kasanKbn;
		}
	}
}