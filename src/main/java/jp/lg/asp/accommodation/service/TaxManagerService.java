package jp.lg.asp.accommodation.service;

import jp.lg.asp.accommodation.dto.TaxManagerForm;

public interface TaxManagerService {

	boolean isSamePerson(String taxManagerAtenaNo, String obligorAtenaNo);

	TaxManagerForm getByShiteiNoAndRno(String shiteiNo, Integer rno);

	TaxManagerForm getByShiteiNo(String shiteiNo);

	void saveByShiteiNo(String shiteiNo, TaxManagerForm form);

	boolean deleteByShiteiNo(String shiteiNo);
}