/**
 * 特別徴収義務者申請受理通知書 JavaScript
 */

/**
 * フォームバリデーション
 */
function validateForm() {
    const hakkoYmd = document.getElementById('hakkoYmd').value;

    if (!hakkoYmd) {
        alert('発行日を入力してください。');
        document.getElementById('hakkoYmd').focus();
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