package jp.lg.asp.accommodation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.lg.asp.accommodation.entity.Jichitai;

@Repository
public interface JichitaiRepository extends JpaRepository<Jichitai, String> {

	/**
	 * クエリパラメータ文字列から自治体を取得する。
	 * param に一意制約が無いため、複数件ヒットした場合は先頭の1件を返す。
	 *
	 * @param param クエリパラメータ文字列（m_jichitai.param）
	 * @return 該当する自治体。存在しない場合は empty
	 */
	Optional<Jichitai> findFirstByParam(String param);
}