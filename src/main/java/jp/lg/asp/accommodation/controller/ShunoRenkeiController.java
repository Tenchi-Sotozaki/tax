package jp.lg.asp.accommodation.controller;

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
	public String index(
			@RequestParam(required = false) String shinkokuFrom,
			@RequestParam(required = false) String shinkokuTo,
			@RequestParam(required = false) String taishoMonth,
			@RequestParam(required = false) String shiteiNo,
			@RequestParam(required = false) String name,
			Model model) {

		accessChecker.checkAccess(SCREEN_ID);
		LocalDate from = shinkokuFrom == null || shinkokuFrom.isEmpty() ? null
				: LocalDate.parse(shinkokuFrom);
		LocalDate to = shinkokuTo == null || shinkokuTo.isEmpty() ? null
				: LocalDate.parse(shinkokuTo);
		List<ShunoDto> items = shunoRenkeiService.search(jichitaiCd, from, to, taishoMonth, shiteiNo, name);

		model.addAttribute("items", items);
		Map<String, Object> searchForm = new HashMap<>();
		searchForm.put("shinkokuFrom", shinkokuFrom);
		searchForm.put("shinkokuTo", shinkokuTo);
		searchForm.put("taishoMonth", taishoMonth);
		searchForm.put("shiteiNo", shiteiNo);
		searchForm.put("name", name);
		model.addAttribute("searchForm", searchForm);

		return "renkei/shunoRenkei";
	}

	@GetMapping("/search")
	@ResponseBody
	public List<ShunoDto> search(
			@RequestParam(required = false) String shinkokuFrom,
			@RequestParam(required = false) String shinkokuTo,
			@RequestParam(required = false) String taishoMonth,
			@RequestParam(required = false) String shiteiNo,
			@RequestParam(required = false) String name) {

		accessChecker.checkAccess(SCREEN_ID);
		LocalDate from = shinkokuFrom == null || shinkokuFrom.isEmpty() ? null
				: LocalDate.parse(shinkokuFrom);
		LocalDate to = shinkokuTo == null || shinkokuTo.isEmpty() ? null
				: LocalDate.parse(shinkokuTo);
		return shunoRenkeiService.search(jichitaiCd, from, to, taishoMonth, shiteiNo, name);
	}

	@PostMapping("/download")
	public ResponseEntity<byte[]> downloadCsv(@RequestBody List<ShunoDto.Key> keys) {
		accessChecker.checkAccess(SCREEN_ID);
		List<ShunoDto> rows = shunoRenkeiService.findByKeys(jichitaiCd, keys);

		String[] csvHeaders = { "宛名番号", "賦課年度", "期別", "登録年月日", "申告年月日", "対象年月", "合計税額", "市区町村税額", "都道府県税額", "加算金額区分",
				"加算割合", "加算金額", "納期限" };
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < csvHeaders.length; i++) {
			if (i > 0)
				sb.append(',');
			sb.append('"').append(csvHeaders[i].replace("\"", "\"\"")).append('"');
		}
		sb.append('\n');

		for (ShunoDto r : rows) {
			String kasanKbnName = convertKasanKbn(r.getKasanKbn());
			String[] cols = new String[] {
					r.getAtenaNo() != null ? r.getAtenaNo() : "",
					r.getNendo() != null ? r.getNendo() : "",
					r.getKibetsu() != null ? String.valueOf(r.getKibetsu()) : "",
					r.getTorokuYmd() != null ? r.getTorokuYmd().toString() : "",
					r.getShinkokuYmd() != null ? r.getShinkokuYmd().toString() : "",
					r.getTaishoYm() != null ? r.getTaishoYm() : "",
					r.getTotalZeigaku() != null ? String.valueOf(r.getTotalZeigaku()) : "",
					r.getCityZeigaku() != null ? String.valueOf(r.getCityZeigaku()) : "",
					r.getKenZeigaku() != null ? String.valueOf(r.getKenZeigaku()) : "",
					kasanKbnName,
					r.getKasanRitsu() != null ? r.getKasanRitsu().toString() : "",
					r.getKasanGaku() != null ? String.valueOf(r.getKasanGaku()) : "",
					r.getNokigen() != null ? r.getNokigen().toString() : ""
			};
			for (int i = 0; i < cols.length; i++) {
				if (i > 0)
					sb.append(',');
				sb.append('"').append(cols[i].replace("\"", "\"\"")).append('"');
			}
			sb.append('\n');
		}

		byte[] body = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.parseMediaType("text/csv;charset=utf-8"));
		httpHeaders.setContentDisposition(ContentDisposition.attachment().filename("shuno_renkei.csv").build());
		return ResponseEntity.ok().headers(httpHeaders).body(body);
	}

	@PostMapping("/kakunin")
	public String kakunin(@RequestParam("keysJson") String keysJson, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		try {
			com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
			List<ShunoDto.Key> keys = om.readValue(keysJson,
					om.getTypeFactory().constructCollectionType(List.class, ShunoDto.Key.class));
			List<ShunoDto> rows = shunoRenkeiService.findByKeys(jichitaiCd, keys);
			model.addAttribute("rows", rows);
			return "renkei/shunoRenkeiKakunin";
		} catch (Exception e) {
			model.addAttribute("rows", java.util.Collections.emptyList());
			return "renkei/shunoRenkeiKakunin";
		}
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