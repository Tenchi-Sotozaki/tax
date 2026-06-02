/**
 * 納税管理人承認(不承認)通知書 JavaScript
 */

/**
 * PDF生成
 */
function generatePdf() {
    const form = document.getElementById('tsuchiForm');
    form.action = '/reports/nozeiKanrininShoninTsuchi/pdf';
    form.target = '_blank';
    form.submit();
}

/**
 * プレビュー
 */
function preview() {
    const form = document.getElementById('tsuchiForm');
    form.action = '/reports/nozeiKanrininShoninTsuchi/preview';
    form.target = '_blank';
    form.submit();
}

/**
 * 印刷
 */
function print() {
    const form = document.getElementById('tsuchiForm');
    form.action = '/reports/nozeiKanrininShoninTsuchi/print';
    form.target = '_blank';
    form.submit();
}

/**
 * DOM読み込み完了後の初期化処理
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('納税管理人承認通知書ページ読み込み完了');
    
    // フォームバリデーション
    const form = document.getElementById('tsuchiForm');
    const hakkoYmd = document.getElementById('hakkoYmd');
    const riyu = document.getElementById('riyu');
    
    // 発行日の入力チェック
    if (hakkoYmd) {
        hakkoYmd.addEventListener('change', function() {
            if (this.value) {
                this.classList.remove('is-invalid');
                this.classList.add('is-valid');
            } else {
                this.classList.remove('is-valid');
                this.classList.add('is-invalid');
            }
        });
    }
    
    // 理由の入力チェック
    if (riyu) {
        riyu.addEventListener('input', function() {
            if (this.value.trim()) {
                this.classList.remove('is-invalid');
                this.classList.add('is-valid');
            } else {
                this.classList.remove('is-valid');
                this.classList.add('is-invalid');
            }
        });
    }
});