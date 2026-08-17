/**
 * 納入申告書の提出期限等の特例適用者指定取消通知書 JavaScript
 */

/**
 * フォームバリデーション
 */
function validateForm() {

	window.ReportError.hide();
	clearInvalid();

    const hakkoYmd = document.getElementById('hakkoYmd').value;
    const tekiyoYmd = document.getElementById('tekiyoYmd').value;
    const riyu = document.getElementById('riyu').value;

    if (!hakkoYmd) {
        setInvalid('hakkoYmd');
        window.ReportError.show('発行年月日を入力してください。');
        return false;
    }

    if (!tekiyoYmd) {
        setInvalid('tekiyoYmd');
        window.ReportError.show('適用年月を入力してください。');
        return false;
    }

    if (!riyu.trim()) {
        setInvalid('riyu');
        window.ReportError.show('取消理由を入力してください。');
        return false;
    }

    return true;
}

function setInvalid(id) {
    const el = document.getElementById(id);
    el.classList.add('is-invalid');
    el.addEventListener('input', () => el.classList.remove('is-invalid'), { once: true });
}

function clearInvalid() {
    document.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
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
