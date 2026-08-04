function validateForm() {
    const hakkoYmd = document.getElementById('hakkoYmd');
    if (!hakkoYmd || !hakkoYmd.value.trim()) {
        alert('発行日を入力してください。');
        if (hakkoYmd) hakkoYmd.focus();
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
