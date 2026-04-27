document.addEventListener('DOMContentLoaded', function () {

    function requireSelected(msg) {
        const cb = document.querySelector('.row-select:checked');
        if (!cb) { alert(msg || 'レコードを選択してください。'); return null; }
        return cb.dataset.id;
    }

    // 行クリックでチェックボックスをトグル＋ハイライト
    document.querySelectorAll('tbody tr').forEach(function (row) {
        row.style.cursor = 'pointer';
        row.addEventListener('click', function (e) {
            if (e.target.closest('.btn, input[type="checkbox"]')) return;
            const cb = row.querySelector('.row-select');
            if (!cb) return;
            const next = !cb.checked;
            document.querySelectorAll('.row-select').forEach(o => {
                o.checked = false;
                o.closest('tr')?.classList.remove('row-selected');
            });
            cb.checked = next;
            row.classList.toggle('row-selected', next);
        });
    });

    document.querySelectorAll('.row-select').forEach(function (cb) {
        cb.addEventListener('click', function (e) {
            e.stopPropagation();
            document.querySelectorAll('.row-select').forEach(o => {
                if (o !== cb) {
                    o.checked = false;
                    o.closest('tr')?.classList.remove('row-selected');
                }
            });
            cb.closest('tr')?.classList.toggle('row-selected', cb.checked);
        });
    });

    // 行内削除ボタン
    document.querySelectorAll('.delete-btn').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            e.stopPropagation();
            const id = this.dataset.id, name = this.dataset.name;
            const modal = document.getElementById('deleteModal');
            modal.querySelector('.modal-body p').textContent =
                '「' + name + '」を削除します。この操作は取り消せません。よろしいですか？';
            modal.querySelector('[data-form-id]').dataset.formId = 'deleteForm-' + id;
            new bootstrap.Modal(modal).show();
        });
    });

    // ナビゲーションヘルパー
    const nav = (btnId, msg, url) =>
        document.getElementById(btnId)?.addEventListener('click', () => {
            const id = requireSelected(msg);
            if (id) location.href = url.replace('{id}', id);
        });

    nav('btnView',            '照会する特別徴収義務者を選択してください。',
                              '/accommodation-tax/tokugimu/edit/{id}');
    nav('btnTaxManager',      '特別徴収義務者を選択してください。',
                              '/accommodation-tax/tax-manager/edit/{id}');
    nav('btnTaxManagerView',  '特別徴収義務者を選択してください。',
                              '/accommodation-tax/tax-manager/edit/{id}');
    nav('btnPaymentLedger',   '事業者を選択してください。',
                              '/accommodation-tax/declaration/payment-ledger/{id}');
    nav('btnConsolidated',    '特別徴収義務者を選択してください。',
                              '/accommodation-tax/consolidated/register/{id}');
    nav('btnConsolidatedView','特別徴収義務者を選択してください。',
                              '/accommodation-tax/consolidated/register/{id}');

    document.getElementById('btnDelete')?.addEventListener('click', () => {
        const id = requireSelected('削除するレコードを選択してください。');
        if (id) document.querySelector('.delete-btn[data-id="' + id + '"]')?.click();
    });
    document.getElementById('btnCorrection')?.addEventListener('click', () => {
        const id = requireSelected('特別徴収義務者を選択してください。');
        if (id) alert('更生請求画面は未実装です。');
    });
    document.getElementById('btnReport')?.addEventListener('click', () => {
        const id = requireSelected('特別徴収義務者を選択してください。');
        if (id) alert('帳票発行画面は未実装です。');
    });
});
