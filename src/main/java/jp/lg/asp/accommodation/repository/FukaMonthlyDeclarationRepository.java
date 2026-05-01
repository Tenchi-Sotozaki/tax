package jp.lg.asp.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// 👇 古い MonthlyDeclaration の import があれば消して、これにする！
import jp.lg.asp.accommodation.entity.FukaMonthlyDeclaration;

@Repository
// 👇 < > の中身を新しいエンティティ名に書き換える！
public interface FukaMonthlyDeclarationRepository extends JpaRepository<FukaMonthlyDeclaration, Long> {
}