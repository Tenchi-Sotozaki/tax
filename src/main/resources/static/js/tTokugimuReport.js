/**
 * 特別徴収義務者帳票出力 JavaScript
 */

// 指定番号をグローバル変数として保持
let shiteiNo = '';

/**
 * DOM読み込み完了後の初期化処理
 */
document.addEventListener('DOMContentLoaded', function() {
    // Thymeleafから渡された指定番号を取得
    const shiteiNoElement = document.getElementById('shiteiNoData');
    if (shiteiNoElement) {
        shiteiNo = shiteiNoElement.textContent || shiteiNoElement.innerText || '';
    }

    console.log('ページ読み込み完了。指定番号:', shiteiNo);

    // 特別徴収義務者指定通知書ボタンのクリックイベント
    const btn = document.getElementById('btnReportTokugimuShiteiTsuchi');
    if (btn) {
        console.log('ボタンが見つかりました。イベントリスナーを設定します。');
        btn.addEventListener('click', function() {
            console.log('ボタンがクリックされました。指定番号:', shiteiNo);
            if (shiteiNo) {
                const url = '/accommodation-tax/reports/tokugimuShiteiTsuchi?shiteiNo=' + encodeURIComponent(shiteiNo);
                console.log('開くURL:', url);
                window.location.href = url;
            } else {
                alert('指定番号が取得できませんでした。');
            }
        });
    } else {
        console.error('btnReportTokugimuShiteiTsuchiボタンが見つかりません');
    }
});