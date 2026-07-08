/**
 * ユーザー情報編集画面用JavaScript
 */

document.addEventListener('DOMContentLoaded', function() {

    // 編集モードでなければ処理を行わない
    const contentContainer = document.querySelector('[data-is-edit]');
    const isEdit = contentContainer ? contentContainer.getAttribute('data-is-edit') === 'true' : false;
    if (!isEdit) return;

    // 編集変更チェック
    function checkValue(input) {

        // チェックボックスとラジオボタンは対象外
        if (input.type === 'checkbox' || input.type === 'radio') return;

        // data-initial-value がないものは適用しない
        if (!input.hasAttribute('data-initial-value')) return;

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