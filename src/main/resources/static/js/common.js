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
 */
document.addEventListener('DOMContentLoaded', function () {
    const today = new Date().toLocaleDateString('sv-SE'); // YYYY-MM-DD
    document.querySelectorAll('input[type="date"]').forEach(function (input) {
        if (!input.value) input.value = today;
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
