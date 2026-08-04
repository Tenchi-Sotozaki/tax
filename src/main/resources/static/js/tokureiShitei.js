/**
 * 納入申告書の提出期限等の特例適用者指定通知書 JavaScript
 */

/**
 * フォームバリデーション
 */
function validateForm() {
	
	// 処理開始時に古いエラーをクリアする
	hideErrorMessage();
	
    const hakkoYmd = document.getElementById('hakkoYmd').value;
    const tekiyoYmd = document.getElementById('tekiyoYmd').value;

    if (!hakkoYmd) {
        showErrorMessage('発行日を入力してください。')
		return false;
    }

    if (!tekiyoYmd) {
        showErrorMessage('適用年月日を入力してください。');
        return false;
    }

    return true;
}

// 区分（認定/不認定）の変更に応じて「不認定の理由」の有効/無効を切り替える
function toggleBikoState() {
    const shonin = document.getElementById('shonin');
    const riyu = document.getElementById('riyu');

    if (shonin.value === '承認') { // 承認
		// 承認に変更された場合は理由をクリア
        riyu.value = '';
        riyu.disabled = true;
    } else { // 不承認
        riyu.disabled = false;
    }
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

	// 区分に応じて「不承認の理由」の有効/無効を切り替える
	toggleBikoState();
});

/**
 * 画面上部にエラーメッセージを表示する
 */
function showErrorMessage(message) {
    const errorAlert = document.getElementById('errorAlert');
    const errorMessageText = document.getElementById('errorMessageText');
    
    if (errorAlert && errorMessageText) {
        errorMessageText.textContent = message;
        errorAlert.style.display = 'block';
        errorAlert.classList.add('show');
    }
}

/**
 * 画面上部のエラーメッセージを非表示にする
 */
function hideErrorMessage() {
    const errorAlert = document.getElementById('errorAlert');
    const errorMessageText = document.getElementById('errorMessageText');
    
    if (errorAlert && errorMessageText) {
        errorMessageText.textContent = '';
        errorAlert.style.display = 'none';
        errorAlert.classList.remove('show');
    }
}
