/**
 * 全画面共通：Enterキーはボタンまたはセレクトにフォーカスがある場合のみ有効
 */
document.addEventListener('keydown', function (e) {
    if (e.key !== 'Enter') return;
    const tag = document.activeElement?.tagName?.toLowerCase();
    if (tag === 'button' || tag === 'select') return;
    e.preventDefault();
}, true);
