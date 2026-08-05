package jp.lg.asp.accommodation.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.config.JichitaiContext;
import jp.lg.asp.accommodation.dto.TopPageConfigForm;
import jp.lg.asp.accommodation.entity.TopPageContent;
import jp.lg.asp.accommodation.repository.TopPageContentRepository;
import jp.lg.asp.accommodation.service.TopPageService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TopPageServiceImpl implements TopPageService {

	/** 全自治体共有コンテンツの jichitai_cd 固定値 */
	private static final String SHARED_JICHITAI_CD = "00000";
	private static final String KBN_SHARED = "0";
	private static final String KBN_CUSTOM = "1";

	private final TopPageContentRepository repository;
	private final JichitaiContext jichitaiContext;

	@Override
	@Transactional(readOnly = true)
	public TopPageContent findShared() {
		TopPageContent content = new TopPageContent();
		content.setHtmlContent("<h1> 共通の情報 </h1>");
		return content;
		//        return repository.findByKbnAndJichitaiCd(KBN_SHARED, SHARED_JICHITAI_CD).orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public TopPageContent findCustom(String jichitaiCd) {
		TopPageContent content = new TopPageContent();
		content.setHtmlContent("<h1> 自治体ごとの情報 </h1>");
		return content;
		//		return repository.findByKbnAndJichitaiCd(KBN_CUSTOM, jichitaiCd).orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public TopPageConfigForm loadForm(String kbn, String jichitaiCd) {
		String targetJichitaiCd = KBN_SHARED.equals(kbn) ? SHARED_JICHITAI_CD : jichitaiCd;
		//TopPageContent content = repository.findByKbnAndJichitaiCd(kbn, targetJichitaiCd).orElse(null);
		TopPageConfigForm form = new TopPageConfigForm();
		form.setKbn(kbn);
		form.setJichitaiCd(jichitaiCd);
		form.setHtmlContent("");
		/*		if (content != null) {
					form.setHtmlContent(content.getHtmlContent());
		 
		}*/
		return form;

	}

	@Override
	@Transactional
	public void save(TopPageConfigForm form) {
		String targetJichitaiCd = KBN_SHARED.equals(form.getKbn()) ? SHARED_JICHITAI_CD
				: (form.getJichitaiCd() != null ? form.getJichitaiCd() : jichitaiContext.getJichitaiCd());

		TopPageContent content = repository.findByKbnAndJichitaiCd(form.getKbn(), targetJichitaiCd)
				.orElseGet(() -> {
					TopPageContent c = new TopPageContent();
					c.setKbn(form.getKbn());
					c.setJichitaiCd(targetJichitaiCd);
					return c;
				});
		content.setHtmlContent(form.getHtmlContent());
		repository.save(content);
	}
}
