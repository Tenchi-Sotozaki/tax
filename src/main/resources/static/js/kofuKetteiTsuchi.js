/**
 * 宿泊税特別徴収事務交付金交付決定通知書 JavaScript
 */

/**
 * ページ読み込み時の初期化
 */
document.addEventListener('DOMContentLoaded', function() {
    // 発行年月日が空の場合、当日を設定
    const hakkoYmdInput = document.querySelector('input[name="hakkoYmd"]');
    if (hakkoYmdInput && (!hakkoYmdInput.value || hakkoYmdInput.value === '')) {
        const today = new Date();
        const year = today.getFullYear();
        const month = String(today.getMonth() + 1).padStart(2, '0');
        const day = String(today.getDate()).padStart(2, '0');
        const todayStr = `${year}-${month}-${day}`;
        hakkoYmdInput.value = todayStr;
    }
    
    // 初期表示更新
    updateDisplayDate();
});

/**
 * 日付表示を更新
 */
function updateDisplayDate() {
    const hakkoYmdInput = document.querySelector('input[name="hakkoYmd"]');
    const displayDate = document.getElementById('displayDate');
    
    if (hakkoYmdInput && displayDate) {
        const dateValue = hakkoYmdInput.value;
        if (dateValue) {
            const date = new Date(dateValue);
            const year = date.getFullYear();
            const month = date.getMonth() + 1;
            const day = date.getDate();
            displayDate.textContent = `${year}年${month}月${day}日`;
        } else {
            displayDate.textContent = '年　　月　　日';
        }
    }
}

/**
 * フォームバリデーション
 */
function validateForm() {
    const shiteiNo = document.querySelector('input[name="shiteiNo"]').value;

    if (!shiteiNo) {
        alert('指定番号が取得できません。');
        return false;
    }

    return true;
}