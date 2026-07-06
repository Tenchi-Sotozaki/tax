'use strict';

document.addEventListener('DOMContentLoaded', () => {

    // datetime-local の値は "yyyy-MM-ddTHH:mm" 形式で送信されるが
    // サーバー側は "yyyy-MM-dd HH:mm" を期待するため、submit前に変換する
    const searchForm = document.getElementById('searchForm');
    if (searchForm) {
        searchForm.addEventListener('submit', () => {
            ['opeDtFrom', 'opeDtTo'].forEach(id => {
                const el = document.getElementById(id);
                if (el && el.value) {
                    el.value = el.value.replace('T', ' ');
                }
            });
        });
    }

    // リセットボタン：検索条件フォームを初期化
    document.getElementById('resetBtn')?.addEventListener('click', () => {
        searchForm?.reset();
    });

    // パラメータセルのJSON整形（キー：値 形式で表示）
    document.querySelectorAll('.param-cell').forEach(cell => {
        const raw = cell.textContent?.trim();
        if (!raw) return;
        try {
            const obj = JSON.parse(raw);
            cell.textContent = formatAsMap(obj);
        } catch {
            // JSON以外はそのまま表示
        }
    });

    function formatAsMap(obj, indent) {
        const prefix = indent ?? '';
        return Object.entries(obj).map(([key, val]) => {
            if (val !== null && typeof val === 'object' && !Array.isArray(val)) {
                return `${prefix}${key}：\n${formatAsMap(val, prefix + '  ')}`;
            }
            const display = Array.isArray(val) ? val.join(', ') : String(val ?? '');
            return `${prefix}${key}：${display}`;
        }).join('\n');
    }

    // ページネーション
    const rows = Array.from(document.querySelectorAll('.log-row'));
    const pageSizeSelect = document.getElementById('pageSizeSelect');
    const pagination = document.getElementById('pagination');
    let currentPage = 1;

    function getPageSize() {
        return parseInt(pageSizeSelect?.value ?? '10', 10);
    }

    function renderPage(page) {
        const size = getPageSize();
        const totalPages = Math.max(1, Math.ceil(rows.length / size));
        currentPage = Math.min(page, totalPages);
        const start = (currentPage - 1) * size;
        const end = start + size;

        rows.forEach((row, i) => {
            row.style.display = (i >= start && i < end) ? '' : 'none';
        });

        renderPagination(totalPages);
    }

    function renderPagination(totalPages) {
        if (!pagination) return;
        pagination.innerHTML = '';

        const addItem = (label, page, disabled, active) => {
            const li = document.createElement('li');
            li.className = 'page-item' + (disabled ? ' disabled' : '') + (active ? ' active' : '');
            const a = document.createElement('a');
            a.className = 'page-link';
            a.href = '#';
            a.textContent = label;
            if (!disabled) {
                a.addEventListener('click', e => { e.preventDefault(); renderPage(page); });
            }
            li.appendChild(a);
            pagination.appendChild(li);
        };

        addItem('前へ', currentPage - 1, currentPage === 1, false);
        for (let i = 1;i <= totalPages;i++) {
            addItem(String(i), i, false, i === currentPage);
        }
        addItem('次へ', currentPage + 1, currentPage === totalPages, false);
    }

    if (rows.length > 0) {
        renderPage(1);
        pageSizeSelect?.addEventListener('change', () => renderPage(1));
    }
});
