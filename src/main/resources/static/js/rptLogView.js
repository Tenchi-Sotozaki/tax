'use strict';

document.addEventListener('DOMContentLoaded', () => {

    const searchForm = document.getElementById('searchForm');

    document.getElementById('resetBtn')?.addEventListener('click', () => {
        searchForm?.reset();
    });

    // ページネーション
    const rows = Array.from(document.querySelectorAll('.log-row'));
    const pageSizeSelect = document.getElementById('pageSizeSelect');
    const pager = new Pagination(rows, pageSizeSelect, document.getElementById('pagination'));

    if (rows.length > 0) {
        pager.render(1);
        pageSizeSelect?.addEventListener('change', () => pager.render(1));
    }
});
