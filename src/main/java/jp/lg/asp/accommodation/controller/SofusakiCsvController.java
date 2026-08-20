package jp.lg.asp.accommodation.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.dto.SofusakiCsvDto;
import jp.lg.asp.accommodation.service.SofusakiCsvService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/renkei/sofusaki-csv")
@RequiredArgsConstructor
public class SofusakiCsvController {

	private final SofusakiCsvService sofusakiCsvService;
	private final ScreenAccessChecker accessChecker;
	private static final String SCREEN_ID = ScreenManagement.SOFUSAKI_CSV;

	@GetMapping
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String init(Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		model.addAttribute("items", sofusakiCsvService.findAll());
		return "renkei/sofusakiCsv";
	}

	@PostMapping("/download")
	@OpeLog(screenId = SCREEN_ID, operation = "ダウンロード")
	public ResponseEntity<byte[]> download(
			@RequestParam(value = "selectedIndexes", required = false) List<Integer> selectedIndexes)
			throws IOException {
		accessChecker.checkAccess(SCREEN_ID);
		List<SofusakiCsvDto> allItems = sofusakiCsvService.findAll();

		List<SofusakiCsvDto> targets;
		if (selectedIndexes == null || selectedIndexes.isEmpty()) {
			targets = allItems;
		} else {
			targets = selectedIndexes.stream()
					.filter(i -> i >= 0 && i < allItems.size())
					.map(allItems::get)
					.collect(Collectors.toList());
		}

		// BOM付きUTF-8でExcelでも文字化けしないように出力
		byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
		byte[] csvBytes = sofusakiCsvService.toCsvString(targets).getBytes(StandardCharsets.UTF_8);
		byte[] output = new byte[bom.length + csvBytes.length];
		System.arraycopy(bom, 0, output, 0, bom.length);
		System.arraycopy(csvBytes, 0, output, bom.length, csvBytes.length);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sofusaki.csv\"")
				.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
				.body(output);
	}
}
