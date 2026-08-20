'use strict';

document.addEventListener('DOMContentLoaded', () => {

    const searchForm = document.getElementById('searchForm');

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

    const paginationEl = document.getElementById('pagination');
    if (paginationEl?.dataset.paginationRows) {
        const rows = Array.from(document.querySelectorAll(paginationEl.dataset.paginationRows));
        if (rows.length > 0) {
            bootstrap.Collapse.getOrCreateInstance(document.getElementById('searchPanel')).hide();
        }
    }
});
