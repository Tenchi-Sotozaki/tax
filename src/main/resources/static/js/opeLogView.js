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

	        const addBtn = (label, page, active) => {
	            const li = document.createElement('li');
	            li.className = 'page-item' + (active ? ' active' : '');
	            const a = document.createElement('a');
	            a.className = 'page-link';
	            a.href = '#';
	            a.textContent = label;
	            a.addEventListener('click', e => { e.preventDefault(); renderPage(page); });
	            li.appendChild(a);
	            pagination.appendChild(li);
	        };

	        const addDisabled = (label, visible = true) => {
	            const li = document.createElement('li');
	            li.className = 'page-item disabled';
	            if (!visible) li.style.visibility = 'hidden';
	            li.innerHTML = `<span class="page-link">${label}</span>`;
	            pagination.appendChild(li);
	        };

	        // 前へ
	        if (currentPage > 1) addBtn('前へ', currentPage - 1, false);
	        else addDisabled('前へ');

	        // 9スロット固定: [1][左…][c-2][c-1][c][c+1][c+2][右…][last]
	        const half = 2;
	        const leftDots  = currentPage - half > 2;
	        const rightDots = currentPage + half < totalPages - 1;

	        const winStart = currentPage - half;
	        const winEnd   = currentPage + half;

	        // スロット0: 1（中央ウィンドウに含まれない場合のみ表示）
	        if (winStart > 1) addBtn('1', 1, currentPage === 1);
	        else              addDisabled('1', false);

	        // スロット1: 左…
	        if (leftDots) addDisabled('…');
	        else          addDisabled('…', false);

	        // スロット2〜6: c-2〜c+2
	        for (let offset = -half; offset <= half; offset++) {
	            const p = currentPage + offset;
	            if (p >= 1 && p <= totalPages)
	                addBtn(String(p), p, p === currentPage);
	            else
	                addDisabled('0', false);
	        }

	        // スロット7: 右…
	        if (rightDots) addDisabled('…');
	        else           addDisabled('…', false);

	        // スロット8: last（中央ウィンドウに含まれない場合のみ表示）
	        if (totalPages > 1 && winEnd < totalPages)
	            addBtn(String(totalPages), totalPages, currentPage === totalPages);
	        else
	            addDisabled(String(totalPages), false);

	        // 次へ
	        if (currentPage < totalPages) addBtn('次へ', currentPage + 1, false);
	        else addDisabled('次へ');
	    }

	    if (rows.length > 0) {
	        renderPage(1);
	        pageSizeSelect?.addEventListener('change', () => renderPage(1));
	        bootstrap.Collapse.getOrCreateInstance(document.getElementById('searchPanel')).hide();
	    }
	});
