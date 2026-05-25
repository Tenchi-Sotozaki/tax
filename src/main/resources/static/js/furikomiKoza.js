/**
 * 振込先口座照会／登録／編集画面用JavaScript
 */

function editMode() {
    // 編集モードに切り替え
    document.getElementById('editForm').submit();
}

function updateKoza() {
    if (!confirm('振込先口座情報を更新しますか？')) {
        return;
    }

    // バリデーションチェック
    if (!validateForm()) {
        return;
    }

    // メインフォームの値を更新用フォームにコピー
    const updateForm = document.getElementById('updateForm');

    // 各フィールドの値をコピー
    updateForm.querySelector('input[name="bankCd"]').value =
        document.getElementById('bankCd').value;
    updateForm.querySelector('input[name="bankName"]').value =
        document.getElementById('bankName').value;
    updateForm.querySelector('input[name="branchCd"]').value =
        document.getElementById('branchCd').value;
    updateForm.querySelector('input[name="branchName"]').value =
        document.getElementById('branchName').value;
    updateForm.querySelector('input[name="shumoku"]').value =
        document.getElementById('shumoku').value;
    updateForm.querySelector('input[name="kozaNo"]').value =
        document.getElementById('kozaNo').value;
    updateForm.querySelector('input[name="meigi"]').value =
        document.getElementById('meigi').value;

    // 更新フォームを送信
    updateForm.submit();
}

/**
 * フォームバリデーション
 */
function validateForm() {
    let isValid = true;
    const errors = [];

    // 金融機関コードバリデーション
    const bankCd = document.getElementById('bankCd').value.trim();
    if (!bankCd) {
        errors.push('金融機関コードは必須入力です');
        isValid = false;
    } else if (!/^[0-9]{4}$/.test(bankCd)) {
        errors.push('金融機関コードは4桁の数字で入力してください');
        isValid = false;
    }

    // 金融機関名バリデーション
    const bankName = document.getElementById('bankName').value.trim();
    if (!bankName) {
        errors.push('金融機関名は必須入力です');
        isValid = false;
    } else if (bankName.length > 30) {
        errors.push('金融機関名は30文字以内で入力してください');
        isValid = false;
    }

    // 支店コードバリデーション
    const branchCd = document.getElementById('branchCd').value.trim();
    if (!branchCd) {
        errors.push('支店コードは必須入力です');
        isValid = false;
    } else if (!/^[0-9]{3}$/.test(branchCd)) {
        errors.push('支店コードは3桁の数字で入力してください');
        isValid = false;
    }

    // 支店名バリデーション
    const branchName = document.getElementById('branchName').value.trim();
    if (!branchName) {
        errors.push('支店名は必須入力です');
        isValid = false;
    } else if (branchName.length > 30) {
        errors.push('支店名は30文字以内で入力してください');
        isValid = false;
    }

    // 預金種目バリデーション
    const shumoku = document.getElementById('shumoku').value;
    if (!shumoku) {
        errors.push('預金種目は必須選択です');
        isValid = false;
    }

    // 口座番号バリデーション
    const kozaNo = document.getElementById('kozaNo').value.trim();
    if (!kozaNo) {
        errors.push('口座番号は必須入力です');
        isValid = false;
    } else if (!/^[0-9]{7}$/.test(kozaNo)) {
        errors.push('口座番号は7桁の数字で入力してください');
        isValid = false;
    }

    // 口座名義バリデーション
    const meigi = document.getElementById('meigi').value.trim();
    if (!meigi) {
        errors.push('口座名義は必須入力です');
        isValid = false;
    } else if (meigi.length > 30) {
        errors.push('口座名義は30文字以内で入力してください');
        isValid = false;
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
            // 登録モードのみバリデーション実行
            const modeInput = form.querySelector('input[name="mode"]');
            if (modeInput && modeInput.value === 'create') {
                if (!validateForm()) {
                    event.preventDefault();
                }
            }
        });
    }
});