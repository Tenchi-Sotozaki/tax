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
