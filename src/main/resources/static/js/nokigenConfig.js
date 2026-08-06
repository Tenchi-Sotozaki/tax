function applyDayColor(el) {
    if (!el.value) { el.style.color = ''; return; }
    const day = new Date(el.value).getDay();
    el.style.color = day === 6 ? 'blue' : day === 0 ? 'red' : '';
}

function changeNendo(select) {
    window.location.href = select.dataset.baseUrl + select.value;
}

function getShiftMode() {
    const checked = document.querySelector('input[name="shiftWeekend"]:checked');
    return checked ? checked.value : 'none';
}

function copyPrevYear(btn) {
    const nendo = document.getElementById('nendo').value;
    if (!nendo) { alert('年度を入力してください。'); return; }
    const existsUrl = btn.dataset.existsUrl;
    const shiftMode = getShiftMode();
    fetch(existsUrl + nendo)
        .then(res => res.json())
        .then(data => {
            if (data.exists) {
                alert('既に登録済みです。編集画面から修正してください。');
                return;
            }
            // サーバー側に shiftMode をクエリパラメータとして渡す
            const url = `${btn.dataset.url}${nendo}?shiftMode=${shiftMode}`;
            return fetch(url);
        })
        .then(res => {
            if (!res) return;
            if (!res.ok) { alert('前年度のデータが見つかりません。'); return; }
            return res.json();
        })
        .then(data => {
            if (!data) return;
            ['1st', '2nd', '3rd', '4th', '5th', '6th', '7th', '8th', '9th', '10th', '11th', '12th'].forEach(k => {
                const el = document.getElementById('nokigen' + k);
                if (el) {
                    // サーバー側で休業日考慮済みの日付が返却される想定
                    el.value = data['nokigen' + k] || '';
                    applyDayColor(el);
                }
            });
        });
}

document.addEventListener('DOMContentLoaded', function() {
    // 全日付入力に色適用 + changeイベント
    document.querySelectorAll('input[type="date"]').forEach(el => {
        applyDayColor(el);
        el.addEventListener('change', function() { applyDayColor(this); });
    });

    // チェックボックス排他制御（ラジオボタン風）
    document.querySelectorAll('input[name="shiftWeekend"]').forEach(cb => {
        cb.addEventListener('change', function() {
            if (this.checked) {
                document.querySelectorAll('input[name="shiftWeekend"]').forEach(other => {
                    if (other !== this) other.checked = false;
                });
            }
        });
    });

    // 登録モード時のみsubmitで重複チェック
    const modeInput = document.querySelector('input[name="mode"]');
    if (modeInput && modeInput.value === 'register') {
        const existsBtn = document.querySelector('[data-exists-url]');
        if (existsBtn) {
            const existsUrl = existsBtn.dataset.existsUrl;
            document.querySelector('form').addEventListener('submit', function(e) {
                if (!validateForm()) { e.preventDefault(); return; }
                const nendo = document.getElementById('nendo').value;
                e.preventDefault();
                const form = this;
                fetch(existsUrl + nendo)
                    .then(res => res.json())
                    .then(data => {
                        if (data.exists) {
                            alert('既に登録済みです。編集画面から修正してください。');
                        } else {
                            form.submit();
                        }
                    });
            });
        }
    }

    // editモード時のsubmitバリデーション
    if (modeInput && modeInput.value === 'edit') {
        document.querySelector('form').addEventListener('submit', function(e) {
            if (!validateForm()) e.preventDefault();
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

        // data-initial-value がないものは適用しない
        if (!input.hasAttribute('data-initial-value')) return;

        const initialValue = input.getAttribute('data-initial-value') || '';

        // nullという文字列になってしまうのを防ぐ
        let initialStr = (initialValue === null || initialValue === 'null') ? '' : String(initialValue).trim();
        let currentStr = String(input.value).trim();

        // inputがdate型の場合の変換
        if (input.type === 'date' && initialStr.length === 8 && !initialStr.includes('-')) {
            initialStr = initialStr.substring(0, 4) + '-' + initialStr.substring(4, 6) + '-' + initialStr.substring(6, 8);
        }
        // inputがmonth型の場合の変換
        else if (input.type === 'month' && initialStr.length === 6 && !initialStr.includes('-')) {
            initialStr = initialStr.substring(0, 4) + '-' + initialStr.substring(4, 6);
        }

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