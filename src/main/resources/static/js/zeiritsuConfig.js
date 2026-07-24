// 全角英数字・スペースを半角に変換する関数
function toHalfWidth(str) {
    if (!str) return '';
    return str.replace(/[！-～]/g, function(s) {
        return String.fromCharCode(s.charCodeAt(0) - 0xFEE0);
    }).replace(/ /g, ' '); // 全角スペースも半角スペースに変換
}

document.addEventListener('DOMContentLoaded', function() {
    const isView = document.getElementById('zeiritsuConfigRoot').dataset.isView === 'true';

    // 自動更新確認モーダル制御
    const autoUpdateModal = document.getElementById('autoUpdateConfirmModal');
    if (autoUpdateModal) {
        document.getElementById('autoUpdateConfirmBtn').addEventListener('click', function() {
            document.getElementById('confirmAutoUpdate').value = 'true';
            document.getElementById('zeiritsuForm').submit();
        });
    }

    // 登録ボタン（type=button）からのsubmit
    const registerBtn = document.getElementById('registerBtn');
    if (registerBtn) {
        registerBtn.addEventListener('click', function() {
            if (autoUpdateModal) {
                new bootstrap.Modal(autoUpdateModal, { backdrop: 'static' }).show();
            } else {
                document.getElementById('zeiritsuForm').submit();
            }
        });
    }

    const displaySt = document.querySelector('input[name="tekiyoStYmDisplay"]');
    const hiddenSt = document.getElementById('tekiyoStYmHidden');
    const displayEd = document.querySelector('input[name="tekiyoEdYmDisplay"]');
    const hiddenEd = document.getElementById('tekiyoEdYmHidden');

    if (hiddenSt && hiddenSt.value && hiddenSt.value.length === 6) {
        displaySt.value = hiddenSt.value.substring(0, 4) + '-' + hiddenSt.value.substring(4, 6);
    }
    if (hiddenEd && hiddenEd.value && hiddenEd.value.length === 6) {
        displayEd.value = hiddenEd.value.substring(0, 4) + '-' + hiddenEd.value.substring(4, 6);
    }

    if (displaySt && !isView) {
        displaySt.addEventListener('change', function() {
            hiddenSt.value = displaySt.value ? displaySt.value.replace('-', '') : '';
        });
    }
    if (displayEd && !isView) {
        displayEd.addEventListener('change', function() {
            hiddenEd.value = displayEd.value ? displayEd.value.replace('-', '') : '';
        });
    }

    const zeiValueLabel = document.getElementById('zeiValueLabel');
    const conditionLabel = document.getElementById('conditionLabel');

    function updateLabel() {
        const checked = document.querySelector('input[name="fukaKbn"]:checked');
        const isTeiritsu = checked && checked.value === '2';
        zeiValueLabel.textContent = (checked && checked.value === '1') ? '税額(円)' : '税率(%)';
        conditionLabel.textContent = isTeiritsu ? '区分名(更生・決定通知書等の区分に相当)' : '条件';
        zeiValueHint.classList.toggle('d-none', !isTeiritsu);
        document.querySelectorAll('.teigaku-condition').forEach(el => el.style.display = isTeiritsu ? 'none' : '');
        document.querySelectorAll('.teiritsu-condition').forEach(el => el.style.display = isTeiritsu ? '' : 'none');
        document.querySelectorAll('[id^="details"][id$=".zeiValue"]').forEach(el => {
            el.placeholder = isTeiritsu ? '0.00～99.99' : '';
        });
    }

    document.querySelectorAll('input[name="fukaKbn"]').forEach(r => r.addEventListener('change', updateLabel));
    updateLabel();

    // 編集モードでなければ処理を行わない
    const contentContainer = document.querySelector('[data-is-edit]');
    const isEdit = contentContainer ? contentContainer.getAttribute('data-is-edit') === 'true' : false;
    if (!isEdit) return;

    // 編集変更チェック
    function checkValue(input) {

        // チェックボックスとラジオボタンは対象外
        if (input.type === 'checkbox' || input.type === 'radio') return;

        const initialValue = input.getAttribute('data-initial-value') || '';

        // nullという文字列になってしまうのを防ぐ
        let initialStr = (initialValue === null || initialValue === 'null') ? '' : String(initialValue).trim();
        let currentStr = String(input.value).trim();

        // input type="month" の場合、YYYYMM を YYYY-MM に変換する
        if (input.type === 'month' && initialStr.length === 6) {
            // 最初の4文字 + '-' + 後半の2文字
            initialStr = initialStr.substring(0, 4) + '-' + initialStr.substring(4, 6);
        }

        // data-ignore-width="true" が指定されているかチェック
        const shouldIgnoreWidth = input.getAttribute('data-ignore-width') === 'true';
        let isChanged;

        if (shouldIgnoreWidth) {
            // 全角・半角を無視して比較
            isChanged = (toHalfWidth(currentStr) !== toHalfWidth(initialStr));
        } else {
            // 通常の比較
            isChanged = (currentStr !== initialStr);
        }

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
