package jp.lg.asp.accommodation.service;

import org.springframework.web.multipart.MultipartFile;

import jp.lg.asp.accommodation.dto.BankImportResultDto;

/**
 * 金融機関コード取込 Service インターフェース
 */
public interface BankImportService {

	/**
	 * ZenginCode のzipファイルから金融機関マスタ・支店マスタを取り込む。
	 * 既存データはすべて置き換える。
	 *
	 * @param file ZenginCode（zengin-code/source-data）のzipファイル
	 * @return 取込結果
	 */
	BankImportResultDto importFromZip(MultipartFile file);
}
