// トップページ編集
document.addEventListener('DOMContentLoaded', function () {

	// ---------- 登録画面：プレビュー ----------
	const contents = document.getElementById('contents');
	const preview = document.getElementById('preview');

	function renderPreview() {
		if (!contents || !preview) return;
		preview.innerHTML = contents.value;
	}

	document.getElementById('btnPreview')?.addEventListener('click', renderPreview);

	// 入力済みの状態で画面が開かれた場合に備え、初期表示でも反映する
	renderPreview();

	// ---------- 一覧画面：削除 ----------
	document.querySelectorAll('.delete-btn').forEach(function (btn) {
		btn.addEventListener('click', function () {
			const modal = document.getElementById('deleteModal');
			if (!modal) {
				console.error('削除モーダルが見つかりません');
				return;
			}
			const confirmButton = modal.querySelector('[data-form-id]');
			if (confirmButton) {
				confirmButton.dataset.formId = 'deleteForm-' + this.dataset.seq;
			}
			new bootstrap.Modal(modal).show();
		});
	});
});
