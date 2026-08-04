/**
 * 納入申告書の提出期限等の特例適用者指定取消通知書 JavaScript
 */

/**
 * フォームバリデーション
 */
function validateForm() {
	
	// エラーメッセージをクリア
	window.ReportError.hide();
	
    const hakkoYmd = document.getElementById('hakkoYmd').value;
    const tekiyoYmd = document.getElementById('tekiyoYmd').value;
    const riyu = document.getElementById('riyu').value;

    if (!hakkoYmd) {
        window.ReportError.show('発行年月日を入力してください。');
        document.getElementById('hakkoYmd').focus();
        return false;
    }

    if (!tekiyoYmd) {
        window.ReportError.show('適用年月を入力してください。');
        document.getElementById('tekiyoYmd').focus();
        return false;
    }

    if (!riyu.trim()) {
        window.ReportError.show('取消理由を入力してください。');
        document.getElementById('riyu').focus();
        return false;
    }

    return true;
}

document.addEventListener('DOMContentLoaded', function() {
    const hakkoYmdInput = document.getElementById('hakkoYmd');
    if (!hakkoYmdInput.value) {
        const today = new Date();
        const formattedDate = today.getFullYear() + '-' +
            String(today.getMonth() + 1).padStart(2, '0') + '-' +
            String(today.getDate()).padStart(2, '0');
        hakkoYmdInput.value = formattedDate;
    }
});
