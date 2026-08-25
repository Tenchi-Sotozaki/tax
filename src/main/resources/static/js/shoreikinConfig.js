/**
 * 特別徴収事務交付金照会／登録／編集画面用JavaScript
 */

function changeMode(mode) {

    // 隠しフィールドにモードを設定
    document.getElementById('switchModeField').value = mode;

    // 共通フォームを送信
    document.getElementById('switchModeForm').submit();
}

// 編集モード
function editMode() {
    changeMode('edit');
}

// 照会モード
function viewMode() {
    changeMode('view');
}

function calculateShoreikin() {
    // 既存の算出用フォームを使用
    const calculateForm = document.getElementById('calculateForm');
    if (!calculateForm) {
        return;
    }

    // メインフォームの値を算出用フォームにコピー
    copyToHiddenForm(calculateForm, ['nendo', 'kofuRitsu', 'kofuYmd']);

    // 算出フォームを送信
    calculateForm.submit();
}

/**
 * メインフォームの入力値を、非表示フォームの同名項目へ写す
 *
 * @param {HTMLFormElement} targetForm 写し先のフォーム
 * @param {string[]} fieldNames 対象の項目名（メインフォームのid＝項目名）
 */
function copyToHiddenForm(targetForm, fieldNames) {
    fieldNames.forEach(function (name) {
        const source = document.getElementById(name);
        const target = targetForm.querySelector('input[name="' + name + '"]');
        if (source && target) {
            target.value = source.value;
        }
    });
}

document.addEventListener('DOMContentLoaded', function() {

    const contentContainer = document.querySelector('[data-is-edit]');
    const isEdit = contentContainer ? contentContainer.dataset.isEdit === 'true' : false;

    // -------------------------------------------------------------------
    // 交付率が未設定のときは交付率設定画面へ誘導する。
    // 表示要否はサーバ側（テンプレートの th:if）で判定しているため、
    // モーダルが描画されていれば開くだけでよい。
    // -------------------------------------------------------------------
    const kofuRitsuGuideModal = document.getElementById('kofuRitsuGuideModal');
    if (kofuRitsuGuideModal) {
        bootstrap.Modal.getOrCreateInstance(kofuRitsuGuideModal).show();
    }

    // 更新は非表示の updateForm を送信するため、確認モーダルを開く時点で値を写す
    const updateConfirmModal = document.getElementById('updateConfirmModal');
    const updateForm = document.getElementById('updateForm');
    if (updateConfirmModal && updateForm) {
        updateConfirmModal.addEventListener('show.bs.modal', function () {
            copyToHiddenForm(updateForm,
                    ['nendo', 'kofuZeigaku', 'kofuRitsu', 'kofuGaku', 'kofuYmd']);
        });
    }

    // 編集モードでなければ処理を行わない
    if (!isEdit) return;

    // 編集変更チェック
    function checkValue(input) {

        // チェックボックスとラジオボタンは対象外
        if (input.type === 'checkbox' || input.type === 'radio') return;

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
