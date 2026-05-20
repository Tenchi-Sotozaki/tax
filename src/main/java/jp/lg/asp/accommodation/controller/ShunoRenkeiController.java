package jp.lg.asp.accommodation.controller;

import java.util.List;

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
import jp.lg.asp.accommodation.service.ShunoRenkeiService;
import jp.lg.asp.accommodation.service.dto.ShunoDto;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/shunoRenkei")
@RequiredArgsConstructor
public class ShunoRenkeiController {

	private final ScreenAccessChecker accessChecker;
	private final ShunoRenkeiService shunoRenkeiService;

	// 既存の画面IDを流用（既存コードは変更しないポリシーに合わせる）
	private static final String SCREEN_ID = ScreenManagement.SHUNO_RENKEI;

	@GetMapping("/list")
	public String index(Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		return "renkei/shunoRenkei";
	}

	@GetMapping("/api/search")
	@ResponseBody
	public List<ShunoDto> apiSearch(
			@RequestParam(required = false) String shinkokuFrom,
			@RequestParam(required = false) String shinkokuTo,
			@RequestParam(required = false) String taishoMonth,
			@RequestParam(required = false) String shiteiNo,
			@RequestParam(required = false) String name) {

		accessChecker.checkAccess(SCREEN_ID);
		java.time.LocalDate from = shinkokuFrom == null || shinkokuFrom.isEmpty() ? null
				: java.time.LocalDate.parse(shinkokuFrom);
		java.time.LocalDate to = shinkokuTo == null || shinkokuTo.isEmpty() ? null
				: java.time.LocalDate.parse(shinkokuTo);
		String jichitaiCd = System.getProperty("app.jichitai.code", "00000");
		return shunoRenkeiService.search(jichitaiCd, from, to, taishoMonth, shiteiNo, name);
	}

	@PostMapping("/download/csv")
	public ResponseEntity<byte[]> downloadCsv(@RequestBody List<ShunoDto.Key> keys) {
		accessChecker.checkAccess(SCREEN_ID);
		String jichitaiCd = System.getProperty("app.jichitai.code", "00000");
		List<ShunoDto> rows = shunoRenkeiService.findByKeys(jichitaiCd, keys);

		String[] headers = { "宛名番号", "賦課年度", "期別", "登録年月日", "申告年月日", "対象年月", "合計税額", "市区町村税額", "都道府県税額", "加算金額区分",
				"加算割合", "加算金額", "納期限" };
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < headers.length; i++) {
			if (i > 0)
				sb.append(',');
			sb.append('"').append(headers[i].replace("\"", "\"\"")).append('"');
		}
		sb.append('\n');

		for (ShunoDto r : rows) {
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
					r.getKasanKbn() != null ? r.getKasanKbn() : "",
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
		HttpHeaders headersResp = new HttpHeaders();
		headersResp.setContentType(MediaType.parseMediaType("text/csv;charset=utf-8"));
		headersResp.setContentDisposition(ContentDisposition.attachment().filename("shuno_renkei.csv").build());
		return ResponseEntity.ok().headers(headersResp).body(body);
	}

	@PostMapping("/kakunin")
	public String kakunin(@RequestParam("keysJson") String keysJson, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		try {
			com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
			List<ShunoDto.Key> keys = om.readValue(keysJson,
					om.getTypeFactory().constructCollectionType(List.class, ShunoDto.Key.class));
			String jichitaiCd = System.getProperty("app.jichitai.code", "00000");
			List<ShunoDto> rows = shunoRenkeiService.findByKeys(jichitaiCd, keys);
			model.addAttribute("rows", rows);
			return "shunoRenkei/shunoRenkeiKakunin";
		} catch (Exception e) {
			model.addAttribute("rows", java.util.Collections.emptyList());
			return "shunoRenkei/shunoRenkeiKakunin";
		}
	}
}
