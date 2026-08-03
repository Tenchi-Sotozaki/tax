package jp.lg.asp.accommodation.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jp.lg.asp.accommodation.annotation.OpeLog;
import jp.lg.asp.accommodation.config.ScreenManagement;
import jp.lg.asp.accommodation.entity.TopPage;
import jp.lg.asp.accommodation.repository.TopPageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * トップページ（ms00000029）
 * <p>
 * DBに登録された掲載項目のうち、掲載期間内のものを差し込んで表示する。
 * 自治体ごとのカスタマイズは画面設計書の書き込みにより対象外。
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TopPageController {

	private static final String VIEW = "topPage";

	private final TopPageRepository topPageRepository;

	@GetMapping({ "/", "/top-page" })
	@OpeLog(screenId = ScreenManagement.TOP_PAGE, operation = "初期表示")
	public String index(Model model) {
		model.addAttribute("items", findPublished());
		return VIEW;
	}

	/**
	 * 掲載中の項目を取得する。
	 * 取得に失敗した場合でも画面自体は表示できるよう、空リストを返す。
	 *
	 * @return 掲載中の項目
	 */
	private List<TopPage> findPublished() {
		try {
			return topPageRepository.findPublished(TopPage.COMMON_JICHITAI_CD, LocalDate.now());
		} catch (Exception e) {
			log.error("トップページの掲載項目の取得に失敗しました", e);
			return List.of();
		}
	}
}
