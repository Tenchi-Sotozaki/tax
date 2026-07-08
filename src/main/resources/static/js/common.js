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
        if (!input.value && !input.disabled && !input.closest('#businessStatusBody')) {
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
 * 全画面共通：テーブルセル・入力項目のオーバーフロー時にtitle属性（ツールチップ）を自動付与
 */
document.addEventListener('DOMContentLoaded', function () {
    function applyTooltips(root) {
        root.querySelectorAll('.table td, .table th, .form-control, .form-select').forEach(function (el) {
            if (el.scrollWidth > el.clientWidth) {
                el.title = el.textContent.trim() || el.value || '';
            } else {
                el.removeAttribute('title');
            }
        });
    }
    applyTooltips(document);

    // 動的に追加される要素にも対応
    const observer = new MutationObserver(function (mutations) {
        mutations.forEach(function (m) {
            m.addedNodes.forEach(function (node) {
                if (node.nodeType === 1) applyTooltips(node);
            });
        });
    });
    observer.observe(document.body, { childList: true, subtree: true });
});
