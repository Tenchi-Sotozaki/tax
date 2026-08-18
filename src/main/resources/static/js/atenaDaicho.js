/**
 * 宛名管理台帳 画面用JavaScript
 */

document.addEventListener('DOMContentLoaded', function () {

    const paginationEl = document.getElementById('pagination');
    const pageSizeSelect = document.getElementById('pageSizeSelect');

    // ページネーションを動的生成する
    if (paginationEl) {
        const currentPage = parseInt(paginationEl.dataset.currentPage ?? '0', 10);
        const totalPages  = parseInt(paginationEl.dataset.totalPages  ?? '0', 10);
        renderServerPagination(paginationEl, currentPage, totalPages);
    }

    // 表示件数変更時にページ先頭へ遷移する
    pageSizeSelect?.addEventListener('change', () => {
        const url = new URL(location.href);
        url.searchParams.set('pageSize', pageSizeSelect.value);
        url.searchParams.set('page', '0');
        location.href = url.toString();
    });

    /**
     * サーバーサイドページングのページネーションを生成する
     * @param {HTMLElement} ul ページネーションの ul 要素
     * @param {number} currentPage 現在のページ番号（0始まり）
     * @param {number} totalPages 総ページ数
     */
    function renderServerPagination(ul, currentPage, totalPages) {
        if (totalPages === 0) return;
        const half = 2;

        function addBtn(label, page, active, disabled) {
            const li = document.createElement('li');
            li.className = 'page-item' + (active ? ' active' : '') + (disabled ? ' disabled' : '');
            const a = document.createElement('a');
            a.className = 'page-link';
            a.href = '#';
            a.textContent = label;
            if (!disabled && !active) {
                a.addEventListener('click', e => {
                    e.preventDefault();
                    const url = new URL(location.href);
                    url.searchParams.set('page', page);
                    location.href = url.toString();
                });
            }
            li.appendChild(a);
            ul.appendChild(li);
        }

        addBtn('前へ', currentPage - 1, false, currentPage === 0);

        const winStart = currentPage - half;
        const winEnd   = currentPage + half;

        if (winStart > 0) {
            addBtn('1', 0, false, false);
            if (winStart > 1) addBtn('…', null, false, true);
        }

        for (let p = Math.max(0, winStart); p <= Math.min(totalPages - 1, winEnd); p++) {
            addBtn(String(p + 1), p, p === currentPage, false);
        }

        if (winEnd < totalPages - 1) {
            if (winEnd < totalPages - 2) addBtn('…', null, false, true);
            addBtn(String(totalPages), totalPages - 1, false, false);
        }

        addBtn('次へ', currentPage + 1, false, currentPage === totalPages - 1);
    }
});
