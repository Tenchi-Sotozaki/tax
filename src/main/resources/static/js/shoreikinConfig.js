/**
 * 特別徴収事務交付金照会／登録／編集画面用JavaScript
 */

function createShoreikin() {
    if (!confirm('交付金情報を登録しますか？')) {
        return;
    }

    // バリデーションチェック
    if (!validateForm()) {
        return;
    }

    // メインフォームをそのまま送信
    const form = document.querySelector('form[th\\:object]');
    if (form) {
        form.submit();
    }
}

function editMode() {
    // 編集モードに切り替え
    document.getElementById('editForm').submit();
}

function updateShoreikin() {
    if (!confirm('交付金情報を更新しますか？')) {
        return;
    }

    // バリデーションチェック
    if (!validateForm()) {
        return;
    }

    // メインフォームの値を更新用フォームにコピー
    const updateForm = document.getElementById('updateForm');

    // 各フィールドの値をコピー
    updateForm.querySelector('input[name="nendo"]').value =
        document.getElementById('nendo').value;
    updateForm.querySelector('input[name="kofuZeigaku"]').value =
        document.getElementById('kofuZeigaku').value;
    updateForm.querySelector('input[name="kofuRitsu"]').value =
        document.getElementById('kofuRitsu').value;
    updateForm.querySelector('input[name="kofuGaku"]').value =
        document.getElementById('kofuGaku').value;
    updateForm.querySelector('input[name="kofuYmd"]').value =
        document.getElementById('kofuYmd').value;

    // 更新フォームを送信
    updateForm.submit();
}

function calculateShoreikin() {
    // 既存の算出用フォームを使用
    const calculateForm = document.getElementById('calculateForm');
    if (!calculateForm) {
        alert('算出フォームが見つかりません');
        return;
    }

    // メインフォームの値を算出用フォームにコピー
    const nendoValue = document.getElementById('nendo').value;
    const kofuRitsuValue = document.getElementById('kofuRitsu').value;
    const kofuYmdValue = document.getElementById('kofuYmd').value;

    // 各フィールドの値を更新
    const nendoInput = calculateForm.querySelector('input[name="nendo"]');
    if (nendoInput) nendoInput.value = nendoValue;

    const kofuRitsuInput = calculateForm.querySelector('input[name="kofuRitsu"]');
    if (kofuRitsuInput) kofuRitsuInput.value = kofuRitsuValue;

    const kofuYmdInput = calculateForm.querySelector('input[name="kofuYmd"]');
    if (kofuYmdInput) kofuYmdInput.value = kofuYmdValue;

    // 算出フォームを送信
    calculateForm.submit();
}

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

    // 納入税額バリデーション
    const kofuZeigaku = document.getElementById('kofuZeigaku').value.trim();
    if (!kofuZeigaku) {
        errors.push('納入税額は必須入力です');
        isValid = false;
    } else {
        const zeigaku = parseInt(kofuZeigaku);
        if (isNaN(zeigaku) || zeigaku < 0) {
            errors.push('納入税額は0以上の数字で入力してください');
            isValid = false;
        } else if (zeigaku > 99999999999999) {
            errors.push('納入税額は14桁以内で入力してください');
            isValid = false;
        }
    }

    // 交付率バリデーション
    const kofuRitsu = document.getElementById('kofuRitsu').value.trim();
    if (!kofuRitsu) {
        showLinkAlert(
            '交付率が設定されていません。<br>' +
            '<a href="/accommodation-tax/admin/kofu-ritsu" class="alert-link">「交付率登録」</a>から設定してください。'
        );
        return false;
    }
	
//    if (!kofuRitsu) {
//        errors.push('交付率は必須入力です。「交付率登録」から設定してください。');
//        isValid = false;
//    } else {
//        const ritsu = parseFloat(kofuRitsu);
//        if (isNaN(ritsu) || ritsu < 0) {
//            errors.push('交付率は0.00以上の数字で入力してください');
//            isValid = false;
//        } else if (ritsu > 99999.99) {
//            errors.push('交付率は整数部5桁、小数部2桁以内で入力してください');
//            isValid = false;
//        }
//    }

    // 交付額バリデーション
    const kofuGaku = document.getElementById('kofuGaku').value.trim();
    if (!kofuGaku) {
        errors.push('交付額は必須入力です');
        isValid = false;
    } else {
        const gaku = parseInt(kofuGaku);
        if (isNaN(gaku) || gaku < 0) {
            errors.push('交付額は0以上の数字で入力してください');
            isValid = false;
        } else if (gaku > 9999999999999) {
            errors.push('交付額は13桁以内で入力してください');
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
            // 登録モードのみバリデーション実行
            const modeInput = form.querySelector('input[name="mode"]');
            if (modeInput && modeInput.value === 'create') {
                if (!validateForm()) {
                    event.preventDefault();
                }
            }
        });
    }

    // 編集モードでなければ処理を行わない
    const contentContainer = document.querySelector('[data-is-edit]');
    const isEdit = contentContainer ? contentContainer.getAttribute('data-is-edit') === 'true' : false;

    if (!isEdit) return;

    // 編集変更チェック
    function checkValue(input) {

        // チェックボックスとラジオボタンは対象外
        if (input.type === 'checkbox' || input.type === 'radio') return;

        //        // 宛名検索のモーダルは処理しない
        //        if (input.closest('.modal')) return;

        const initialValue = input.getAttribute('data-initial-value') || '';

        // nullという文字列になってしまうのを防ぐ
        let initialStr = (initialValue === null || initialValue === 'null') ? '' : String(initialValue).trim();
        let currentStr = String(input.value).trim();

        // 変更があったか判定
        const isChanged = (currentStr !== initialStr);

		// 変更があれば黄色い枠を付与
        if (isChanged) {
            input.style.border = '3px solid #ffeb3b';
        } else {
            input.style.border = '';
        }
    }

    // 画面表示時に最初から値が変わっているものを検知、および各イベントへの登録
    const inputs = document.querySelectorAll('.form-control, .form-select');
    inputs.forEach(input => {
		
        // 画面を開いた瞬間にズレがあるかチェック
        checkValue(input);

        // イベント登録
        input.addEventListener('input', () => checkValue(input));
        input.addEventListener('change', () => checkValue(input));
        input.addEventListener('blur', () => checkValue(input));
    });
});

// 交付率エラーアラート
function showLinkAlert(htmlMessage) {
    const bodyElement = document.getElementById('customAlertBody');
    bodyElement.innerHTML = htmlMessage; // HTMLをそのまま反映させてリンクを有効にする

    const modalElement = document.getElementById('customAlertModal');
    const modal = new bootstrap.Modal(modalElement);
    modal.show();
}
