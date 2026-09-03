package jp.lg.asp.accommodation.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.lg.asp.accommodation.dto.TopPageConfigForm;
import jp.lg.asp.accommodation.entity.TopPageContent;
import jp.lg.asp.accommodation.entity.TopPageContentId;
import jp.lg.asp.accommodation.repository.TopPageContentRepository;
import jp.lg.asp.accommodation.service.TopPageService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TopPageServiceImpl implements TopPageService {

	/** 全自治体共有コンテンツの jichitai_cd 固定値 */
	private static final String SHARED_JICHITAI_CD = "99999";
	private static final String KBN_SHARED = "0";
	private static final String KBN_CUSTOM = "1";

	private final TopPageContentRepository repository;

	@Override
	@Transactional(readOnly = true)
	public List<TopPageContent> findShared() {
		return repository.findByJichitaiCdAndPostingStartDateLessThanEqualAndPostingEndDateGreaterThanEqual(SHARED_JICHITAI_CD,LocalDate.now(), LocalDate.now());			
	}

	@Override
	@Transactional(readOnly = true)
	public TopPageConfigForm loadForm() {
		TopPageConfigForm form = new TopPageConfigForm();
		form.setTitle("");
		form.setHtmlContent("");
		return form;
	}

	@Override
	@Transactional
	public void save(TopPageConfigForm form) {

	    TopPageContent content;

	    if (form.getSeq() != null) {

	        // 更新
	        TopPageContentId id =
	                new TopPageContentId("99999", form.getSeq());

	        content = repository.findById(id)
	                .orElseThrow(() ->
	                        new IllegalArgumentException("データが存在しません。"));

	    } else {

	        // 新規
	        content = new TopPageContent();

	        content.setJichitaiCd("99999");
	        content.setSeq(repository.getNextSeq("99999"));
	    }

	    content.setTitle(form.getTitle());
	    content.setHtmlContent(form.getHtmlContent());
	    content.setPostingStartDate(form.getPostingStartDate());
	    content.setPostingEndDate(form.getPostingEndDate());

	    repository.save(content);
	}
	
	@Override
	public List<TopPageContent> findAll() {
		return repository.findAll();
	}
	
	@Override
	public TopPageContent findBySeq(Integer seq) {
		return repository
			.findById(
				new TopPageContentId(
					"99999",
					seq))
			.orElseThrow(() ->
				new IllegalArgumentException("データが存在しません。"));
	}
	
	@Override
	@Transactional

	public void delete(Integer seq) {
		TopPageContent entity =	
		findBySeq(seq);	
		repository.delete(entity);
	}
}
