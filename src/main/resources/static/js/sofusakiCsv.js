'use strict';

document.addEventListener('DOMContentLoaded', () => {

    const rows = Array.from(document.querySelectorAll('.csv-row'));
    const pageSizeSelect = document.getElementById('pageSizeSelect');
    const pager = new Pagination(rows, pageSizeSelect, document.getElementById('pagination'));

    if (rows.length > 0) {
        pager.render(1);
        pageSizeSelect?.addEventListener('change', () => pager.render(1));
    }

    // 全選択/解除
    document.getElementById('checkAll')?.addEventListener('change', function () {
        document.querySelectorAll('.row-check').forEach(cb => cb.checked = this.checked);
    });

    // 個別チェック変更時に全選択チェックを同期
    document.querySelectorAll('.row-check').forEach(cb => {
        cb.addEventListener('change', () => {
            const all = document.querySelectorAll('.row-check');
            const checked = document.querySelectorAll('.row-check:checked');
            document.getElementById('checkAll').checked = all.length === checked.length;
        });
    });
});
