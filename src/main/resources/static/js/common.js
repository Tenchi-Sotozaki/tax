/**
 * クライアントサイドページネーション共通部品
 *
 * 使い方:
 *   const pager = new Pagination(rows, pageSizeSelect, paginationUl, { half: 2 });
 *   pager.render(1);
 *   pageSizeSelect?.addEventListener('change', () => pager.render(1));
 */
class Pagination {
    #rows;
    #pageSizeSelect;
    #pagination;
    #half;
    #currentPage = 1;

    constructor(rows, pageSizeSelect, pagination, { half = 2 } = {}) {
        this.#rows = rows;
        this.#pageSizeSelect = pageSizeSelect;
        this.#pagination = pagination;
        this.#half = half;
    }

    render(page) {
        const size = parseInt(this.#pageSizeSelect?.value ?? '10', 10);
        const totalPages = Math.max(1, Math.ceil(this.#rows.length / size));
        this.#currentPage = Math.min(page, totalPages);
        const start = (this.#currentPage - 1) * size;
        const end = start + size;
        this.#rows.forEach((row, i) => {
            row.style.display = (i >= start && i < end) ? '' : 'none';
        });
        this.#renderPagination(totalPages);
    }

    #renderPagination(totalPages) {
        if (!this.#pagination) return;
        this.#pagination.innerHTML = '';
        const cur = this.#currentPage;
        const half = this.#half;

        const addBtn = (label, page, active) => {
            const li = document.createElement('li');
            li.className = 'page-item' + (active ? ' active' : '');
            const a = document.createElement('a');
            a.className = 'page-link';
            a.href = '#';
            a.textContent = label;
            a.addEventListener('click', e => { e.preventDefault(); this.render(page); });
            li.appendChild(a);
            this.#pagination.appendChild(li);
        };

        const addDisabled = (label, visible = true) => {
            const li = document.createElement('li');
            li.className = 'page-item disabled';
            if (!visible) li.style.visibility = 'hidden';
            li.innerHTML = `<span class="page-link">${label}</span>`;
            this.#pagination.appendChild(li);
        };

        // 前へ
        if (cur > 1) addBtn('前へ', cur - 1, false);
        else addDisabled('前へ');

        const winStart = cur - half;
        const winEnd   = cur + half;
        const leftDots  = winStart > 2;
        const rightDots = winEnd < totalPages - 1;

        if (winStart > 1) addBtn('1', 1, cur === 1);
        else              addDisabled('1', false);

        if (leftDots) addDisabled('…');
        else          addDisabled('…', false);

        for (let offset = -half; offset <= half; offset++) {
            const p = cur + offset;
            if (p >= 1 && p <= totalPages) addBtn(String(p), p, p === cur);
            else                           addDisabled('0', false);
        }

        if (rightDots) addDisabled('…');
        else           addDisabled('…', false);

        if (totalPages > 1 && winEnd < totalPages) addBtn(String(totalPages), totalPages, cur === totalPages);
        else                                       addDisabled(String(totalPages), false);

        // 次へ
        if (cur < totalPages) addBtn('次へ', cur + 1, false);
        else addDisabled('次へ');
    }
}

/**
 * セッション情報の取得・保存を管理するクラス
 */
class SessionManager {
    static #csrfHeaders() {
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const headers = { 'Content-Type': 'application/json' };
        if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
        return headers;
    }

    static async save(url, data) {
        const res = await fetch(url, {
            method: 'POST',
            headers: this.#csrfHeaders(),
            body: JSON.stringify(data)
        });
        if (res.headers.get('content-type')?.includes('application/json')) return res.json();
    }

    static async get(url) {
        const res = await fetch(url, { headers: this.#csrfHeaders() });
        return res.json();
    }
}

/**
 * 全画面共通：#pagination[data-pagination-rows] があれば自動でページネーションを初期化
 * data-pagination-rows  : 行要素のCSSセレクタ
 * data-pagination-parent: (省略可) セレクタが行要素を直接指さない場合の親要素タグ名 (例: "tr")
 */
document.addEventListener('DOMContentLoaded', function () {
    const paginationEl = document.getElementById('pagination');
    const rowSelector = paginationEl?.dataset.paginationRows;
    if (!rowSelector) return;

    const parentTag = paginationEl.dataset.paginationParent;
    let rows = Array.from(document.querySelectorAll(rowSelector));
    if (parentTag) rows = rows.map(el => el.closest(parentTag)).filter(Boolean);
    rows = [...new Set(rows)];

    const pageSizeSelect = document.getElementById('pageSizeSelect');
    const pager = new Pagination(rows, pageSizeSelect, paginationEl);
    pager.render(1);
    pageSizeSelect?.addEventListener('change', () => pager.render(1));
});

/**
 * 全画面共通：モーダルを閉じる前にフォーカスをモーダル外へ移動（aria-hidden警告対策）
 */
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.modal').forEach(function (modal) {
        modal.addEventListener('hide.bs.modal', function () {
            if (modal.contains(document.activeElement)) {
                document.activeElement.blur();
            }
        });
    });
});

/**
 * 全画面共通：Enterキーはボタンまたはセレクトにフォーカスがある場合のみ有効
 */
document.addEventListener('keydown', function (e) {
    if (e.key !== 'Enter') return;
    const tag = document.activeElement?.tagName?.toLowerCase();
    if (tag === 'button' || tag === 'select' || tag == 'textarea') return;
    e.preventDefault();
}, true);

/**
 * 全画面共通：type="date" の入力欄が空の場合、当日をデフォルト値としてセット
 * ※ disabled または businessStatusBody 内の項目は除外
 */
document.addEventListener('DOMContentLoaded', function () {
    const today = new Date().toLocaleDateString('sv-SE'); // YYYY-MM-DD
    document.querySelectorAll('input[type="date"]').forEach(function (input) {
        if (!input.value && !input.disabled && !input.closest('#collapseBusinessStatus') && !input.hasAttribute('data-no-today')) {
            input.value = today;
        }
    });
});

/**
 * 全画面共通：編集・登録画面（照会モード除く）で最初の入力可能項目にフォーカス
 * readonly・disabled・hidden でない最初の input/select/textarea を対象とする
 */
document.addEventListener('DOMContentLoaded', function () {
    const first = document.querySelector(
        'input:not([type="hidden"]):not([readonly]):not([disabled]),' +
        'select:not([disabled]),' +
        'textarea:not([readonly]):not([disabled])'
    );
    if (first) first.focus();
});

/**
 * 全画面共通：テーブルセル・入力項目のオーバーフロー時にカスタムツールチップを表示
 */
document.addEventListener('DOMContentLoaded', function () {
    // ツールチップ要素を作成
    const tooltip = document.createElement('div');
    tooltip.id = 'acomo-tooltip';
    tooltip.style.cssText = 'position:fixed;z-index:9999;padding:6px 12px;background:#1b2d57;color:#fff;border-radius:6px;font-size:13px;max-width:400px;word-break:break-all;pointer-events:none;opacity:0;transition:opacity 0.15s;white-space:pre-wrap;box-shadow:0 4px 12px rgba(0,0,0,0.3);';
    document.body.appendChild(tooltip);

    let showTimer = null;

    function showTooltip(e) {
        const el = e.currentTarget;
        const text = el.tagName === 'INPUT' || el.tagName === 'SELECT' ? (el.value || '') : el.textContent.trim();
        if (!text || el.scrollWidth <= el.clientWidth) {
            return;
        }
        showTimer = setTimeout(function () {
            tooltip.textContent = text;
            tooltip.style.opacity = '1';
            positionTooltip(e);
        }, 300);
    }

    function hideTooltip() {
        clearTimeout(showTimer);
        tooltip.style.opacity = '0';
    }

    function positionTooltip(e) {
        const x = e.clientX + 12;
        const y = e.clientY + 16;
        const maxX = window.innerWidth - tooltip.offsetWidth - 8;
        const maxY = window.innerHeight - tooltip.offsetHeight - 8;
        tooltip.style.left = Math.min(x, maxX) + 'px';
        tooltip.style.top = Math.min(y, maxY) + 'px';
    }

    function attachTooltipListeners(root) {
        root.querySelectorAll('.table td, .table th, .form-control, .form-select, .tokugimu-info-value').forEach(function (el) {
            if (el.dataset.tooltipBound) return;
            el.dataset.tooltipBound = '1';
            el.addEventListener('mouseenter', showTooltip);
            el.addEventListener('mousemove', positionTooltip);
            el.addEventListener('mouseleave', hideTooltip);
        });
    }

    attachTooltipListeners(document);

    // 動的に追加される要素にも対応
    const observer = new MutationObserver(function (mutations) {
        mutations.forEach(function (m) {
            m.addedNodes.forEach(function (node) {
                if (node.nodeType === 1) attachTooltipListeners(node);
            });
        });
    });
    observer.observe(document.body, { childList: true, subtree: true });
});
