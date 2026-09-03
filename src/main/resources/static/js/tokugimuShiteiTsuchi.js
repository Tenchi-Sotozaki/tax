/**
 * 特別徴収義務者指定通知書 JavaScript
 */

/**
 * フォームバリデーション
 */
function validateForm() {
    const hakkoYmd = document.getElementById('hakkoYmd');
    const riyu = document.getElementById('riyu');
    let hasError = false;

    // エラー状態リセット
    [hakkoYmd, riyu].forEach(el => {
        el.classList.remove('is-invalid');
        el.nextElementSibling.textContent = '';
    });

    if (!hakkoYmd.value) {
        hakkoYmd.classList.add('is-invalid');
        document.getElementById('hakkoYmdError').textContent = '発行年月日を入力してください。';
        hasError = true;
    }
    if (!riyu.value.trim()) {
        riyu.classList.add('is-invalid');
        document.getElementById('riyuError').textContent = '指定の理由を入力してください。';
        hasError = true;
    }

    if (hasError) {
        (hakkoYmd.classList.contains('is-invalid') ? hakkoYmd : riyu).focus();
        return false;
    }
    return true;
}

// ページ読み込み時の初期化
document.addEventListener('DOMContentLoaded', function() {
    // 発行年月日のデフォルト値設定（今日の日付）
    const hakkoYmdInput = document.getElementById('hakkoYmd');
    if (!hakkoYmdInput.value) {
        const today = new Date();
        const formattedDate = today.getFullYear() + '-' +
            String(today.getMonth() + 1).padStart(2, '0') + '-' +
            String(today.getDate()).padStart(2, '0');
        hakkoYmdInput.value = formattedDate;
    }
});