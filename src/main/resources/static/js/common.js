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
 * 全画面共通：Enterキーはボタンまたはセレクトにフォーカスがある場合のみ有効
 */
document.addEventListener('keydown', function (e) {
    if (e.key !== 'Enter') return;
    const tag = document.activeElement?.tagName?.toLowerCase();
    if (tag === 'button' || tag === 'select') return;
    e.preventDefault();
}, true);

/**
 * 全画面共通：type="date" の入力欄が空の場合、当日をデフォルト値としてセット
 * ※ disabled または businessStatusBody 内の項目は除外
 */
document.addEventListener('DOMContentLoaded', function () {
    const today = new Date().toLocaleDateString('sv-SE'); // YYYY-MM-DD
    document.querySelectorAll('input[type="date"]').forEach(function (input) {
        if (!input.value && !input.disabled && !input.closest('#businessStatusBody') && !input.hasAttribute('data-no-today')) {
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
