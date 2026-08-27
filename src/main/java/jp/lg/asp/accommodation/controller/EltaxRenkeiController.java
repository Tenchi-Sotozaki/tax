package jp.lg.asp.accommodation.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenAccessChecker;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.constant.EltaxConstants;
import jp.lg.asp.accommodation.entity.EltaxRenkei;
import jp.lg.asp.accommodation.service.EltaxRenkeiService;
import jp.lg.asp.accommodation.service.NokigenService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/eltax-renkei")
@RequiredArgsConstructor
public class EltaxRenkeiController {

	private final EltaxRenkeiService eltaxRenkeiService;
	private final ScreenAccessChecker accessChecker;
	private final NokigenService nokigenService;

	private static final String SCREEN_ID = ScreenManagement.ELTAX_RENKEI;

	@GetMapping
	@OpeLog(screenId = SCREEN_ID, operation = "初期表示")
	public String index(Model model) {
		accessChecker.checkAccess(SCREEN_ID);
		
		// 納入期限が未登録の場合
		if (nokigenService.findAll().isEmpty()) {
			model.addAttribute("errorMessage", "納入期限が登録されていません。");
		}
		
		model.addAttribute("eltaxRenkeiList", eltaxRenkeiService.findAll());
		model.addAttribute("shubetsuNameMap", EltaxConstants.SHUBETSU_NAME_MAP);
		return "eltaxRenkei/eltaxRenkei";
	}

	@GetMapping("/download/{seq}")
	@OpeLog(screenId = SCREEN_ID, operation = "取込確認")
	public ResponseEntity<byte[]> download(@PathVariable BigDecimal seq) {
		accessChecker.checkAccess(SCREEN_ID);

		EltaxRenkei entity = eltaxRenkeiService.findBySeq(seq);
		if (entity == null || entity.getLog() == null) {
			return ResponseEntity.notFound().build();
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentDisposition(
				ContentDisposition.attachment().filename(entity.getFileName(), StandardCharsets.UTF_8).build());
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		return ResponseEntity.ok().headers(headers).body(entity.getLog());
	}
}
