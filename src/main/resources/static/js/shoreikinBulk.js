/**
 * 特別徴収事務交付金一括算出画面用JavaScript
 */

/**
 * フォームバリデーション
 */
function validateForm() {
    let isValid = true;
    const errors = [];

    // 交付金年度バリデーション
    const nendo = document.getElementById('nendo').value.trim();
    if (!nendo) {
        errors.push('交付金年度は必須入力です');
        isValid = false;
    } else if (!/^[0-9]{4}$/.test(nendo)) {
        errors.push('交付金年度は4桁の数字で入力してください');
        isValid = false;
    }

    // 交付率バリデーション
    const kofuRitsu = document.getElementById('kofuRitsu').value.trim();
    if (!kofuRitsu) {
        errors.push('交付率は必須入力です');
        isValid = false;
    } else {
        const ritsu = parseFloat(kofuRitsu);
        if (isNaN(ritsu) || ritsu < 0) {
            errors.push('交付率は0.00以上の数字で入力してください');
            isValid = false;
        } else if (ritsu > 99999.99) {
            errors.push('交付率は整数部5桁、小数部2桁以内で入力してください');
            isValid = false;
        }
    }

    // エラーメッセージ表示
    if (!isValid) {
        alert('入力エラー:\n' + errors.join('\n'));
    }

    return isValid;
}

// フォーム送信時のバリデーション
document.addEventListener('DOMContentLoaded', function() {
    const form = document.querySelector('form[th\\:object]');
    if (form) {
        form.addEventListener('submit', function(event) {
            if (!validateForm()) {
                event.preventDefault();
            }
        });
    }
});