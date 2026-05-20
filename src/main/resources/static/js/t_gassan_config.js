'use strict';

document.addEventListener('DOMContentLoaded', () => {

    // 適用時期セレクト変更時の遷移
    const tekiyoSelect = document.getElementById('tekiyoSelect');
    const fromShiteiNoVal = document.getElementById('fromShiteiNoVal');
    if (tekiyoSelect && fromShiteiNoVal) {
        tekiyoSelect.addEventListener('change', function () {
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
    document.querySelectorAll('.facility-check').forEach(cb => {
        cb.addEventListener('change', function () {
            this.closest('tr').classList.toggle('table-primary', this.checked);
        });
    });

    // 確認モーダルを開く前にチェック済み施設が1件以上あるか検証
    document.getElementById('openConfirmModalBtn')?.addEventListener('click', () => {
        const checked = document.querySelectorAll('.facility-check:checked');
        if (checked.length === 0) {
            alert('合算対象施設を1件以上選択してください。');
            return;
        }
        const modal = document.getElementById('registerModal');
        if (modal) new bootstrap.Modal(modal).show();
    });

    // 確認モーダルの実行ボタンでフォーム送信
    document.querySelector('#registerModal .btn-confirm')?.addEventListener('click', () => {
        document.getElementById('gassanForm')?.submit();
    });
});
