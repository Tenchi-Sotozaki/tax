/**
 * 特別徴収義務者指定通知書 JavaScript
 */

/**
 * PDF生成
 */
function generatePdf() {
    if (!validateForm()) {
        return;
    }
    
    const form = document.getElementById('tsuchiForm');
    form.action = '/accommodation-tax/reports/tokugimuShiteiTsuchi/pdf';
    form.target = '_self';
    form.submit();
}

/**
 * プレビュー
 */
function preview() {
    if (!validateForm()) {
        return;
    }
    
    const form = document.getElementById('tsuchiForm');
    const formData = new FormData(form);
    
    fetch('/accommodation-tax/reports/tokugimuShiteiTsuchi/preview', {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('プレビューの生成に失敗しました');
        }
        return response.blob();
    })
    .then(blob => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
        // メモリリークを防ぐため、少し後にURLを解放
        setTimeout(() => window.URL.revokeObjectURL(url), 1000);
    })
    .catch(error => {
        console.error('Error:', error);
        alert('プレビューの表示に失敗しました。');
    });
}

/**
 * 印刷
 */
function print() {
    if (!validateForm()) {
        return;
    }
    
    const form = document.getElementById('tsuchiForm');
    form.action = '/accommodation-tax/reports/tokugimuShiteiTsuchi/print';
    form.target = '_self';
    form.submit();
}

/**
 * フォームバリデーション
 */
function validateForm() {
    const hakkoYmd = document.getElementById('hakkoYmd').value;
    const riyu = document.getElementById('riyu').value;
    
    if (!hakkoYmd) {
        alert('発行日を入力してください。');
        document.getElementById('hakkoYmd').focus();
        return false;
    }
    
    if (!riyu.trim()) {
        alert('指定の理由を入力してください。');
        document.getElementById('riyu').focus();
        return false;
    }
    
    return true;
}

// ページ読み込み時の初期化
document.addEventListener('DOMContentLoaded', function() {
    // 発行日のデフォルト値設定（今日の日付）
    const hakkoYmdInput = document.getElementById('hakkoYmd');
    if (!hakkoYmdInput.value) {
        const today = new Date();
        const formattedDate = today.getFullYear() + '-' + 
            String(today.getMonth() + 1).padStart(2, '0') + '-' + 
            String(today.getDate()).padStart(2, '0');
        hakkoYmdInput.value = formattedDate;
    }
});