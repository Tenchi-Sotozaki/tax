function validateForm() {
    const hakkoYmd = document.getElementById('hakkoYmd');
    if (!hakkoYmd || !hakkoYmd.value.trim()) {
        alert('発行日を入力してください。');
        if (hakkoYmd) hakkoYmd.focus();
        return false;
    }
    return true;
}
