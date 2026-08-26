function validateForm() {
    const hakkoYmd = document.getElementById('hakkoYmd');
    hakkoYmd.classList.remove('is-invalid');
    document.getElementById('hakkoYmdError').textContent = '';

    if (!hakkoYmd || !hakkoYmd.value.trim()) {
        hakkoYmd.classList.add('is-invalid');
        document.getElementById('hakkoYmdError').textContent = '発行年月日を入力してください。';
        hakkoYmd.focus();
        return false;
    }
    return true;
}

// 区分（認定/不認定）の変更に応じて「不認定の理由」の有効/無効を切り替える
function toggleBikoState() {
    const ninteiSelect = document.getElementById('nintei');
    const bikoTextarea = document.getElementById('biko');

    if (ninteiSelect.value === '認定') { // 認定
		// 認定に変更された場合は理由をクリア
        bikoTextarea.value = '';
        bikoTextarea.disabled = true;
    } else { // 不認定
        bikoTextarea.disabled = false;
    }
}

document.addEventListener('DOMContentLoaded', function() {
	
    toggleBikoState();
});
