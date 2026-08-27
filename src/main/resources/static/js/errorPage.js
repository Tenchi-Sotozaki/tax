/**
 * 遷移元に戻る処理 
 */
function goBack() {
    // 履歴やリファラーがある場合は前の画面に戻り、なければトップへ遷移する
    if (document.referrer && document.referrer !== window.location.href) {
        history.back();
    } else {
        window.location.href = '/accommodation-tax/top';
    }
}