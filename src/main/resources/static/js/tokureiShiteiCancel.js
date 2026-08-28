/**
 * 納入申告書の提出期限等の特例適用者指定取消通知書 JavaScript
 */

/**
 * フォームバリデーション
 */
function validateForm() {
    const hakkoYmd = document.getElementById('hakkoYmd');
    const tekiyoYmd = document.getElementById('tekiyoYmd');
    const riyu = document.getElementById('riyu');
    let hasError = false;

    [hakkoYmd, tekiyoYmd, riyu].forEach(el => {
        el.classList.remove('is-invalid');
        document.getElementById(el.id + 'Error').textContent = '';
    });

    if (!hakkoYmd.value) {
        hakkoYmd.classList.add('is-invalid');
        document.getElementById('hakkoYmdError').textContent = '発行年月日を入力してください。';
        hasError = true;
    }
    if (!tekiyoYmd.value) {
        tekiyoYmd.classList.add('is-invalid');
        document.getElementById('tekiyoYmdError').textContent = '適用年月を入力してください。';
        hasError = true;
    }
    if (!riyu.value.trim()) {
        riyu.classList.add('is-invalid');
        document.getElementById('riyuError').textContent = '取消理由を入力してください。';
        hasError = true;
    }

    if (hasError) {
        [hakkoYmd, tekiyoYmd, riyu].find(el => el.classList.contains('is-invalid')).focus();
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
