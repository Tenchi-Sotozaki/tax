/**
 * 宛名管理台帳 画面用JavaScript
 */

// 表示件数セレクトボックスの変更時に、選択した件数でページ先頭へ遷移する
document.getElementById('pageSizeSelect')?.addEventListener('change', function () {
    const baseUrl = this.dataset.baseUrl;
    window.location.href = baseUrl + '&pageSize=' + this.value + '&page=0';
});
