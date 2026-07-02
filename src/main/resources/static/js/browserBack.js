/**
 * ブラウザバック使用禁止警告のアラートを表示するJavaScript
 */

// 現在のURLをブラウザの履歴スタックに新しいエントリとして追加（ブラウザバック検知用）
history.pushState(null, '', location.href);

// アラート二重表示防止フラグ
let isHandling = false;

/**
 * ブラウザバック時
 */
window.addEventListener('popstate', function() {

    // 処理中の場合は二重実行を防止
    if (isHandling) return;
    isHandling = true;

    // 前に進めて遷移をキャンセル
    history.go(1);

    // go(1)による再発火を受け取った後にアラートを表示
    setTimeout(function() {
        alert('ブラウザバックは使用できません。');
        isHandling = false;
    }, 100);
});
