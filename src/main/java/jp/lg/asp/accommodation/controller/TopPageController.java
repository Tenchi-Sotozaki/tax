package jp.lg.asp.accommodation.controller;

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
 * 表示内容はDBに登録されたタグ付きテキストを差し込む。
 * 全自治体共有の内容のみを扱う（自治体ごとの情報は画面設計書の書き込みにより対象外）。
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
		model.addAttribute("commonHtml", findContents(TopPage.KBN_COMMON, TopPage.COMMON_JICHITAI_CD));
		return VIEW;
	}

	/**
	 * トップページに差し込むタグ付きテキストを取得する。
	 * 未登録の場合でも画面自体は表示できるよう、空文字を返す。
	 *
	 * @param kbn 表示区分
	 * @param jichitaiCd 自治体コード
	 * @return タグ付きテキスト
	 */
	private String findContents(String kbn, String jichitaiCd) {
		try {
			return topPageRepository.findByKbnAndJichitaiCd(kbn, jichitaiCd)
					.map(TopPage::getContents)
					.orElse("");
		} catch (Exception e) {
			log.error("トップページの内容取得に失敗しました: kbn={}, jichitaiCd={}", kbn, jichitaiCd, e);
			return "";
		}
	}
}
