document.addEventListener('DOMContentLoaded', function () {

	// 一覧の「詳細」は指定番号をパラメータで渡さず、
	// セッションに選択中の特別徴収義務者として保存してから照会画面へ遷移する。
	document.querySelectorAll('.detail-btn').forEach(function (btn) {
		btn.addEventListener('click', function () {
			const dto = {
				atenaNo: this.dataset.atenaNo || null,
				shiteiNo: this.dataset.shiteiNo,
				gassanShiteiNo: null,
				name: this.dataset.name || null,
				shisetsuName: this.dataset.shisetsuName || null
			};

			const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
			const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
			const headers = { 'Content-Type': 'application/json' };
			if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

			fetch('/accommodation-tax/api/shitei-gassan/select', {
				method: 'POST',
				headers: headers,
				body: JSON.stringify(dto)
			}).then(function (res) {
				if (!res.ok) throw new Error('セッション保存に失敗しました');
				location.href = '/accommodation-tax/tokugimu/view';
			}).catch(function (err) {
				console.error(err);
				alert('遷移に失敗しました。画面を再読み込みして再度お試しください。');
			});
		});
	});
});
