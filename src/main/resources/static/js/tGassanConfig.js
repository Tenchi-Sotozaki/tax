'use strict';

document.addEventListener('DOMContentLoaded', () => {

    // 代表施設ラジオボタン制御
    document.querySelectorAll('.daihyo-radio').forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.checked) {
                // 対応するチェックボックスも自動でチェック
                const checkbox = this.closest('tr').querySelector('.facility-check');
                if (checkbox && !checkbox.disabled) {
                    checkbox.checked = true;
                    checkbox.closest('tr').classList.add('table-primary');
                }
            }
        });
    });

    // 適用時期セレクト変更時の遷移
    const tekiyoSelect = document.getElementById('tekiyoSelect');
    const fromShiteiNoVal = document.getElementById('fromShiteiNoVal');
    if (tekiyoSelect && fromShiteiNoVal) {
        tekiyoSelect.addEventListener('change', function() {
            location.href = '/accommodation-tax/gassan/view-by-shitei/'
                + fromShiteiNoVal.value + '?gassanShiteiNo=' + this.value;
        });
    }

    // 適用時期（年月）選択時に hidden inputへ yyyy-MM-01 をセット
    function syncMonthToDate(monthInputId, hiddenInputId) {
        const monthInput = document.getElementById(monthInputId);
        const hiddenInput = document.getElementById(hiddenInputId);
        if (!monthInput || !hiddenInput) return;
        const sync = () => {
            hiddenInput.value = monthInput.value ? monthInput.value + '-01' : '';
        };
        sync();
        monthInput.addEventListener('change', sync);
    }
    syncMonthToDate('tekiyoStYmdMonth', 'tekiyoStYmd');

    // チェックされた行をハイライト
    setupFacilityEventListeners();

    // 確認モーダルを開く前にチェック済み施設が2件以上あるか検証
    document.getElementById('openConfirmModalBtn')?.addEventListener('click', () => {
        const checked = document.querySelectorAll('.facility-check:checked');
        if (checked.length === 0) {
            alert('合算対象施設を2件以上選択してください。');
            return;
        }
        const modal = document.getElementById('registerModal');
        if (modal) new bootstrap.Modal(modal).show();
    });

    // 確認モーダルの実行ボタンでフォーム送信
    document.querySelector('#registerModal .btn-confirm')?.addEventListener('click', () => {
        document.getElementById('gassanForm')?.submit();
    });

    // 編集モードでなければ処理を行わない
    const contentContainer = document.querySelector('[data-is-edit]');
    const isEdit = contentContainer ? contentContainer.getAttribute('data-is-edit') === 'true' : false;
    if (!isEdit) return;

    // 編集変更チェック
    function checkValue(input) {

        // チェックボックスとラジオボタンは対象外
        if (input.type === 'checkbox' || input.type === 'radio') return;

		// モーダル内は処理しない
        if (input.closest('.modal')) return;

        const initialValue = input.getAttribute('data-initial-value') || '';

        // nullという文字列になってしまうのを防ぐ
        let initialStr = (initialValue === null || initialValue === 'null') ? '' : String(initialValue).trim();
        let currentStr = String(input.value).trim();

        // 変更があったか判定
        const isChanged = (currentStr !== initialStr);

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

// 施設チェックボックスとラジオボタンのイベントリスナーを設定
function setupFacilityEventListeners() {
    document.querySelectorAll('.facility-check').forEach(cb => {
        cb.addEventListener('change', function() {
            this.closest('tr').classList.toggle('table-primary', this.checked);

            // チェックボックスが外された時、代表施設ラジオボタンもクリア
            if (!this.checked) {
                const radio = this.closest('tr').querySelector('.daihyo-radio');
                if (radio) {
                    radio.checked = false;
                }
            }
        });
    });

    // 代表施設ラジオボタン制御
    document.querySelectorAll('.daihyo-radio').forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.checked) {
                const checkbox = this.closest('tr').querySelector('.facility-check');
                if (checkbox && !checkbox.disabled) {
                    checkbox.checked = true;
                    checkbox.closest('tr').classList.add('table-primary');
                }
            }
        });
    });
}
