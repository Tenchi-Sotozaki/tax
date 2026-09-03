/**
 * 合算申告納入承認通知書 JavaScript
 */

/**
 * フォームバリデーション
 */
function validateForm() {
    const hakkoYmd = document.getElementById('hakkoYmd');
    hakkoYmd.classList.remove('is-invalid');
    document.getElementById('hakkoYmdError').textContent = '';

    if (!hakkoYmd.value) {
        hakkoYmd.classList.add('is-invalid');
        document.getElementById('hakkoYmdError').textContent = '発行年月日を入力してください。';
        hakkoYmd.focus();
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
