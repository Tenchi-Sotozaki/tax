package jp.lg.asp.accommodation.controller;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.JichitaiContext;
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

	private final JichitaiContext jichitaiContext;

	private static final String SCREEN_ID = ScreenManagement.SHUNO_RENKEI;
	private static final String SCREEN_ID_KAKUNIN = ScreenManagement.SHUNO_RENKEI_KAKUNIN;

	@GetMapping("/list")
	@OpeLog(screenId = SCREEN_ID, operation = "一覧表示")
	public String index(
			@RequestParam(required = false) String shinkokuFrom,
			@RequestParam(required = false) String shinkokuTo,
			@RequestParam(required = false) String taishoMonth,
			@RequestParam(required = false) String shiteiNo,
			@RequestParam(required = false) String name,
			@RequestParam(required = false, defaultValue = "partial") String nameMatchType,
			@RequestParam(required = false, defaultValue = "0") int page,
			@RequestParam(required = false, defaultValue = "10") int pageSize,
			Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();

		accessChecker.checkAccess(SCREEN_ID);
		LocalDate from = shinkokuFrom == null || shinkokuFrom.isEmpty() ? null
				: LocalDate.parse(shinkokuFrom);
		LocalDate to = shinkokuTo == null || shinkokuTo.isEmpty() ? null
				: LocalDate.parse(shinkokuTo);
		List<ShunoDto> allItems = shunoRenkeiService.search(jichitaiCd, from, to, taishoMonth, shiteiNo, name,
				nameMatchType);

		int size = pageSize > 0 ? pageSize : 10;
		int total = allItems.size();
		int totalPages = (int) Math.ceil((double) total / size);
		int current = page < 0 ? 0 : page;
		if (totalPages > 0 && current >= totalPages) {
			current = totalPages - 1;
		}
		int fromIdx = Math.min(current * size, total);
		int toIdx = Math.min(fromIdx + size, total);
		Page<ShunoDto> items = new PageImpl<>(new ArrayList<>(allItems.subList(fromIdx, toIdx)),
				PageRequest.of(current, size), total);

		model.addAttribute("items", items);
		model.addAttribute("pageWindow", buildPageWindow(current, items.getTotalPages()));
		Map<String, Object> searchForm = new HashMap<>();
		searchForm.put("shinkokuFrom", shinkokuFrom);
		searchForm.put("shinkokuTo", shinkokuTo);
		searchForm.put("taishoMonth", taishoMonth);
		searchForm.put("shiteiNo", shiteiNo);
		searchForm.put("name", name);
		searchForm.put("nameMatchType", nameMatchType);
		searchForm.put("pageSize", size);
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
		String jichitaiCd = jichitaiContext.getJichitaiCd();

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
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		accessChecker.checkAccess(SCREEN_ID);
		List<ShunoDto> rows = shunoRenkeiService.findByKeys(jichitaiCd, keys);

		String[] csvHeaders = { "宛名番号", "賦課年度", "期別", "登録年月日", "申告年月日", "対象年月", "合計税額", "市区町村税額", "都道府県税額",
				"加算金額区分1", "加算割合1", "加算金額1",
				"加算金額区分2", "加算割合2", "加算金額2",
				"加算金額区分3", "加算割合3", "加算金額3",
				"納期限" };
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < csvHeaders.length; i++) {
			if (i > 0)
				sb.append(',');
			sb.append('"').append(csvHeaders[i].replace("\"", "\"\"")).append('"');
		}
		sb.append('\n');

		for (ShunoDto r : rows) {
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
				// 加算1
				convertKasanKbn(r.getKasanKbn1()),
				r.getKasanRitsu1() != null ? r.getKasanRitsu1().toString() : "",
				r.getKasanGaku1() != null ? String.valueOf(r.getKasanGaku1()) : "",
				// 加算2
				convertKasanKbn(r.getKasanKbn2()),
				r.getKasanRitsu2() != null ? r.getKasanRitsu2().toString() : "",
				r.getKasanGaku2() != null ? String.valueOf(r.getKasanGaku2()) : "",
				// 加算3
				convertKasanKbn(r.getKasanKbn3()),
				r.getKasanRitsu3() != null ? r.getKasanRitsu3().toString() : "",
				r.getKasanGaku3() != null ? String.valueOf(r.getKasanGaku3()) : "",
				// 納期限（3件は1つに統合し nokigen1 を出力）
				r.getNokigen1() != null ? r.getNokigen1().toString() : "" };
			for (int i = 0; i < cols.length; i++) {
				if (i > 0)
					sb.append(',');
				sb.append('"').append(cols[i].replace("\"", "\"\"")).append('"');
			}
			sb.append('\n');
		}

		byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }; // UTF-8 BOM
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
	@OpeLog(screenId = SCREEN_ID_KAKUNIN, operation = "確認")
	public String kakunin(@RequestParam("keysJson") String keysJson, Model model) {
		String jichitaiCd = jichitaiContext.getJichitaiCd();
		com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
		try {
			accessChecker.checkAccess(SCREEN_ID_KAKUNIN);
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

	private java.util.List<Integer> buildPageWindow(int current, int totalPages) {
		java.util.List<Integer> pages = new ArrayList<>();
		if (totalPages <= 0) {
			return pages;
		}
		java.util.TreeSet<Integer> show = new java.util.TreeSet<>();
		show.add(0);
		show.add(totalPages - 1);
		for (int i = current - 1; i <= current + 1; i++) {
			if (i >= 0 && i < totalPages) {
				show.add(i);
			}
		}
		int prev = -2;
		for (int pIdx : show) {
			if (prev != -2 && pIdx - prev > 1) {
				pages.add(-1);
			}
			pages.add(pIdx);
			prev = pIdx;
		}
		return pages;
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