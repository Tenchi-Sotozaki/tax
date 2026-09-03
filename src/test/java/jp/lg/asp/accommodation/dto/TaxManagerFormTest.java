package jp.lg.asp.accommodation.dto;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class TaxManagerFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private TaxManagerForm fullForm() {
        TaxManagerForm f = new TaxManagerForm();
        f.setKbn("1");
        f.setRegistrationDate(LocalDate.of(2024, 5, 1));
        f.setDeclarationDate(LocalDate.of(2024, 5, 1));
        f.setManagerYubinNo("060-0001");
        f.setManagerAddress("札幌市中央区北1条西1丁目");
        f.setManagerName("山田太郎");
        f.setManagerNameKana("ヤマダタロウ");
        f.setManagerPhone("011-000-0000");
        return f;
    }

    // ===================================================================
    // No.46 validate - registrationDateあり・declarationDateあり
    // ===================================================================

    @Test
    void validate_registrationDateあり_declarationDateあり_エラーなし() {
        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(fullForm());

        assertThat(errors).doesNotContainKey("registrationDate");
        assertThat(errors).doesNotContainKey("declarationDate");
    }

    // ===================================================================
    // No.47 validate - registrationDateがnull
    // ===================================================================

    @Test
    void validate_kbn1_registrationDateがnull_エラーあり() {
        TaxManagerForm form = fullForm();
        form.setRegistrationDate(null);

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).containsEntry("registrationDate", "登録日は必須です");
    }

    // ===================================================================
    // No.48 validate - declarationDateがnull
    // ===================================================================

    @Test
    void validate_kbn1_declarationDateがnull_エラーあり() {
        TaxManagerForm form = fullForm();
        form.setDeclarationDate(null);

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).containsEntry("declarationDate", "申告日は必須です");
    }

    // ===================================================================
    // No.49 validate - kbn="1"・全必須項目あり
    // ===================================================================

    @Test
    void validate_kbn1_全必須項目あり_エラーなし() {
        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(fullForm());

        assertThat(errors).isEmpty();
    }

    // ===================================================================
    // No.50 validate - kbn="1"・managerYubinNoが空
    // ===================================================================

    @Test
    void validate_kbn1_managerYubinNoが空_エラーあり() {
        TaxManagerForm form = fullForm();
        form.setManagerYubinNo("");

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).containsEntry("managerYubinNo", "住所（郵便番号）は必須です");
    }

    // ===================================================================
    // No.51 validate - kbn="1"・managerAddressが空
    // ===================================================================

    @Test
    void validate_kbn1_managerAddressが空_エラーあり() {
        TaxManagerForm form = fullForm();
        form.setManagerAddress("");

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).containsEntry("managerAddress", "住所（所在地）は必須です");
    }

    // ===================================================================
    // No.52 validate - kbn="1"・managerNameが空
    // ===================================================================

    @Test
    void validate_kbn1_managerNameが空_エラーあり() {
        TaxManagerForm form = fullForm();
        form.setManagerName("");

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).containsEntry("managerName", "氏名は必須です");
    }

    // ===================================================================
    // No.53 validate - kbn="1"・managerNameKanaが空
    // ===================================================================

    @Test
    void validate_kbn1_managerNameKanaが空_エラーあり() {
        TaxManagerForm form = fullForm();
        form.setManagerNameKana("");

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).containsEntry("managerNameKana", "ふりがなは必須です");
    }

    // ===================================================================
    // No.54 validate - kbn="1"・managerPhoneが空
    // ===================================================================

    @Test
    void validate_kbn1_managerPhoneが空_エラーあり() {
        TaxManagerForm form = fullForm();
        form.setManagerPhone("");

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).containsEntry("managerPhone", "電話番号は必須です");
    }

    // ===================================================================
    // No.55 validate - kbn="1"のときreasonはnullにクリアされる
    // ===================================================================

    @Test
    void validate_kbn1_reasonがnullにクリアされる() {
        TaxManagerForm form = fullForm();
        form.setReason("何か");

        TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(form.getReason()).isNull();
    }

    // ===================================================================
    // No.56 validate - kbn="2"・reasonあり
    // ===================================================================

    @Test
    void validate_kbn2_reasonあり_エラーなし() {
        TaxManagerForm form = fullForm();
        form.setKbn("2");
        form.setReason("理由あり");

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).doesNotContainKey("reason");
    }

    // ===================================================================
    // No.57 validate - kbn="2"・reasonが空
    // ===================================================================

    @Test
    void validate_kbn2_reasonが空_エラーあり() {
        TaxManagerForm form = fullForm();
        form.setKbn("2");
        form.setReason("");

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).containsEntry("reason", "理由は必須です");
    }

    // ===================================================================
    // No.58 validate - kbn="2"・reasonがnull
    // ===================================================================

    @Test
    void validate_kbn2_reasonがnull_エラーあり() {
        TaxManagerForm form = fullForm();
        form.setKbn("2");
        form.setReason(null);

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).containsEntry("reason", "理由は必須です");
    }

    // ===================================================================
    // No.59 validate - kbn="3"・reasonあり・個人情報なし
    // ===================================================================

    @Test
    void validate_kbn3_reasonあり_個人情報なし_エラーなし() {
        TaxManagerForm form = new TaxManagerForm();
        form.setKbn("3");
        form.setRegistrationDate(LocalDate.of(2024, 5, 1));
        form.setDeclarationDate(LocalDate.of(2024, 5, 1));
        form.setReason("理由あり");

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).isEmpty();
    }

    // ===================================================================
    // No.60 validate - kbn="3"・reasonが空
    // ===================================================================

    @Test
    void validate_kbn3_reasonが空_エラーあり() {
        TaxManagerForm form = new TaxManagerForm();
        form.setKbn("3");
        form.setRegistrationDate(LocalDate.of(2024, 5, 1));
        form.setDeclarationDate(LocalDate.of(2024, 5, 1));
        form.setReason("");

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).containsEntry("reason", "理由は必須です");
    }

    // ===================================================================
    // No.61 validate - kbn="3"のとき個人情報未入力でも個人情報エラーなし
    // ===================================================================

    @Test
    void validate_kbn3_個人情報未入力_個人情報エラーなし() {
        TaxManagerForm form = new TaxManagerForm();
        form.setKbn("3");
        form.setRegistrationDate(LocalDate.of(2024, 5, 1));
        form.setDeclarationDate(LocalDate.of(2024, 5, 1));
        form.setReason("理由あり");

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).doesNotContainKey("managerName");
        assertThat(errors).doesNotContainKey("managerYubinNo");
        assertThat(errors).doesNotContainKey("managerAddress");
        assertThat(errors).doesNotContainKey("managerNameKana");
        assertThat(errors).doesNotContainKey("managerPhone");
    }

    // ===================================================================
    // No.62 validate - kbnがnull
    // ===================================================================

    @Test
    void validate_kbnがnull_個人情報必須チェックが走る_reasonはnullにクリア() {
        TaxManagerForm form = new TaxManagerForm();
        form.setKbn(null);
        form.setRegistrationDate(LocalDate.of(2024, 5, 1));
        form.setDeclarationDate(LocalDate.of(2024, 5, 1));
        form.setReason("何か");

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).containsKey("managerYubinNo");
        assertThat(errors).containsKey("managerAddress");
        assertThat(errors).containsKey("managerName");
        assertThat(errors).containsKey("managerNameKana");
        assertThat(errors).containsKey("managerPhone");
        assertThat(form.getReason()).isNull();
    }

    // ===================================================================
    // No.63 validate - kbnが"9"（想定外の値）
    // ===================================================================

    @Test
    void validate_kbnが想定外の値_個人情報必須チェックが走る() {
        TaxManagerForm form = new TaxManagerForm();
        form.setKbn("9");
        form.setRegistrationDate(LocalDate.of(2024, 5, 1));
        form.setDeclarationDate(LocalDate.of(2024, 5, 1));

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).containsKey("managerYubinNo");
        assertThat(errors).containsKey("managerAddress");
        assertThat(errors).containsKey("managerName");
        assertThat(errors).containsKey("managerNameKana");
        assertThat(errors).containsKey("managerPhone");
    }

    // ===================================================================
    // No.64 validate - 全必須項目未入力（kbn="1"）
    // ===================================================================

    @Test
    void validate_kbn1_全必須項目未入力_全エラーが返る() {
        TaxManagerForm form = new TaxManagerForm();
        form.setKbn("1");

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors).containsKey("registrationDate");
        assertThat(errors).containsKey("declarationDate");
        assertThat(errors).containsKey("managerYubinNo");
        assertThat(errors).containsKey("managerAddress");
        assertThat(errors).containsKey("managerName");
        assertThat(errors).containsKey("managerNameKana");
        assertThat(errors).containsKey("managerPhone");
        assertThat(errors).hasSize(7);
    }

    // ===================================================================
    // No.65 validate - エラーの順序がLinkedHashMapの挿入順
    // ===================================================================

    @Test
    void validate_エラーの順序がLinkedHashMapの挿入順になっている() {
        TaxManagerForm form = new TaxManagerForm();
        form.setKbn("1");

        Map<String, String> errors = TaxManagerForm.TaxManagerValidator.validate(form);

        assertThat(errors.keySet()).containsExactly(
                "registrationDate",
                "declarationDate",
                "managerYubinNo",
                "managerAddress",
                "managerName",
                "managerNameKana",
                "managerPhone"
        );
    }

    // ===================================================================
    // No.66 isValid - 全必須項目あり
    // ===================================================================

    @Test
    void isValid_全必須項目あり_バリデーションエラーなし() {
        Set<ConstraintViolation<TaxManagerForm>> violations = validator.validate(fullForm());

        assertThat(violations).isEmpty();
    }

    // ===================================================================
    // No.67 isValid - registrationDateがnull
    // ===================================================================

    @Test
    void isValid_registrationDateがnull_バリデーションエラーあり() {
        TaxManagerForm form = fullForm();
        form.setRegistrationDate(null);

        Set<ConstraintViolation<TaxManagerForm>> violations = validator.validate(form);

        assertThat(violations).anyMatch(v -> v.getMessage().equals("登録日は必須です"));
    }

    // ===================================================================
    // No.68 isValid - kbn="2"・reasonなし
    // ===================================================================

    @Test
    void isValid_kbn2_reasonなし_バリデーションエラーあり() {
        TaxManagerForm form = fullForm();
        form.setKbn("2");
        form.setReason(null);

        Set<ConstraintViolation<TaxManagerForm>> violations = validator.validate(form);

        assertThat(violations).anyMatch(v -> v.getMessage().equals("理由は必須です"));
    }
}
