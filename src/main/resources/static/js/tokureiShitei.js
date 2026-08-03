/**
 * 納入申告書の提出期限等の特例適用者指定通知書 JavaScript
 */

/**
 * フォームバリデーション
 */
function validateForm() {
    const hakkoYmd = document.getElementById('hakkoYmd').value;
    const tekiyoYmd = document.getElementById('tekiyoYmd').value;
    const shonin = document.querySelector('input[name="shonin"]:checked');
    const riyu = document.getElementById('riyu').value;

    if (!hakkoYmd) {
        alert('発行日を入力してください。');
        document.getElementById('hakkoYmd').focus();
        return false;
    }

    if (!tekiyoYmd) {
        alert('適用年月日を入力してください。');
        document.getElementById('tekiyoYmd').focus();
        return false;
    }

    if (!shonin) {
        alert('承認を選択してください。');
        return false;
    }

    if (shonin.value === '不承認' && !riyu.trim()) {
        alert('不承認理由を入力してください。');
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

    // 不承認選択時のみ理由欄を表示
    const shoninRadios = document.querySelectorAll('input[name="shonin"]');
    const riyuArea = document.getElementById('riyuArea');
    shoninRadios.forEach(radio => {
        radio.addEventListener('change', function() {
            riyuArea.style.display = this.value === '不承認' ? '' : 'none';
            if (this.value !== '不承認') {
                document.getElementById('riyu').value = '';
            }
        });
    });

    // 初期表示時に不承認が選択済みの場合は表示
    const checkedShonin = document.querySelector('input[name="shonin"]:checked');
    if (checkedShonin && checkedShonin.value === '不承認') {
        riyuArea.style.display = '';
    }
});
