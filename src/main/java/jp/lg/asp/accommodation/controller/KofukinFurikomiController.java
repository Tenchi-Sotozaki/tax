package jp.lg.asp.accommodation.controller;

import java.util.ArrayList;
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
import jp.lg.asp.accommodation.dto.ShoreikinRenkeiDto;
import jp.lg.asp.accommodation.service.ShoreikinRenkeiService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/kofukinFurikomi")
@RequiredArgsConstructor
public class KofukinFurikomiController {

	private final ScreenAccessChecker accessChecker;
	private final ShoreikinRenkeiService shoreikinRenkeiService;

	@Value("${app.jichitai.code}")
	private String jichitaiCd;

	private static final String SCREEN_ID = ScreenManagement.KOFUKIN_FURIKOMI;

	@GetMapping("/list")
	public String index(
			@RequestParam(required = false) String nendo,
			@RequestParam(required = false) String shiteiNo,
			@RequestParam(required = false) String name,
			Model model) {

		accessChecker.checkAccess(SCREEN_ID);

		try {
			// 奨励金連携サービスを使用（交付金振込情報として表示）
			List<ShoreikinRenkeiDto> items = shoreikinRenkeiService.search(jichitaiCd, nendo, shiteiNo, name);
			model.addAttribute("items", items);
		} catch (Exception e) {
			System.out.println("Error in service call: " + e.getMessage());
			e.printStackTrace();
			model.addAttribute("items", new ArrayList<ShoreikinRenkeiDto>());
		}

		Map<String, Object> searchForm = new HashMap<>();
		searchForm.put("nendo", nendo);
		searchForm.put("shiteiNo", shiteiNo);
		searchForm.put("name", name);
		model.addAttribute("searchForm", searchForm);

		return "renkei/kofukinFurikomi";
	}

	@GetMapping("/search")
	@ResponseBody
	public List<ShoreikinRenkeiDto> search(
			@RequestParam(required = false) String nendo,
			@RequestParam(required = false) String shiteiNo,
			@RequestParam(required = false) String name) {

		accessChecker.checkAccess(SCREEN_ID);
		return shoreikinRenkeiService.search(jichitaiCd, nendo, shiteiNo, name);
	}

	@PostMapping("/download")
	public ResponseEntity<byte[]> downloadCsv(@RequestBody List<ShoreikinRenkeiDto.Key> keys) {
		accessChecker.checkAccess(SCREEN_ID);
		List<ShoreikinRenkeiDto> rows = shoreikinRenkeiService.findByKeys(jichitaiCd, keys);

		String[] csvHeaders = { "指定番号", "宛名番号", "氏名/名称", "奨励金年度", "交付年月日", "納入税額", "交付率", "交付額",
				"金融機関コード", "金融機関名", "支店コード", "支店名", "預金種目", "口座番号", "口座名義" };
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < csvHeaders.length; i++) {
			if (i > 0)
				sb.append(',');
			sb.append('"').append(csvHeaders[i].replace("\"", "\"\"")).append('"');
		}
		sb.append('\n');

		for (ShoreikinRenkeiDto r : rows) {
			String shumokuName = convertShumoku(r.getShumoku());

			String[] cols = new String[] {
					r.getShiteiNo() != null ? r.getShiteiNo() : "",
					r.getAtenaNo() != null ? r.getAtenaNo() : "",
					r.getName() != null ? r.getName() : "",
					r.getNendo() != null ? r.getNendo() : "",
					r.getKofuYmd() != null ? r.getKofuYmd().toString() : "",
					r.getKofuZeigaku() != null ? String.valueOf(r.getKofuZeigaku()) : "",
					r.getKofuRitsu() != null ? r.getKofuRitsu().toString() : "",
					r.getKofuGaku() != null ? String.valueOf(r.getKofuGaku()) : "",
					r.getBankCd() != null ? r.getBankCd() : "",
					r.getBankName() != null ? r.getBankName() : "",
					r.getBranchCd() != null ? r.getBranchCd() : "",
					r.getBranchName() != null ? r.getBranchName() : "",
					shumokuName,
					r.getKozaNo() != null ? r.getKozaNo() : "",
					r.getMeigi() != null ? r.getMeigi() : ""
			};
			for (int i = 0; i < cols.length; i++) {
				if (i > 0)
					sb.append(',');
				sb.append('"').append(cols[i].replace("\"", "\"\"")).append('"');
			}
			sb.append('\n');
		}

		byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}; // UTF-8 BOM
		byte[] csvData = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
		byte[] body = new byte[bom.length + csvData.length];
		System.arraycopy(bom, 0, body, 0, bom.length);
		System.arraycopy(csvData, 0, body, bom.length, csvData.length);
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.parseMediaType("text/csv;charset=utf-8"));
		httpHeaders.setContentDisposition(ContentDisposition.attachment().filename("kofukin_furikomi.csv").build());
		return ResponseEntity.ok().headers(httpHeaders).body(body);
	}

	@PostMapping("/kakunin")
	public String kakunin(@RequestParam("keysJson") String keysJson, Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		try {
			com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
			List<ShoreikinRenkeiDto.Key> keys = om.readValue(keysJson,
					om.getTypeFactory().constructCollectionType(List.class, ShoreikinRenkeiDto.Key.class));
			List<ShoreikinRenkeiDto> rows = shoreikinRenkeiService.findByKeys(jichitaiCd, keys);
			model.addAttribute("rows", rows);
			return "renkei/kofukinFurikomiKakunin";
		} catch (Exception e) {
			model.addAttribute("rows", java.util.Collections.emptyList());
			return "renkei/kofukinFurikomiKakunin";
		}
	}

	private String convertShumoku(String shumoku) {
		if (shumoku == null) {
			return "";
		}
		switch (shumoku) {
		case "1":
			return "普通";
		case "2":
			return "当座";
		default:
			return shumoku;
		}
	}
}