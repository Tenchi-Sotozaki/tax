// 特別徴収義務者台帳
// 指定番号はパラメータで渡さず、common.js の SessionManager でセッションに保存してから遷移する
const SG_SELECT_API = '/accommodation-tax/api/shitei-gassan/select';

document.addEventListener('DOMContentLoaded', function () {

	/**
	 * 選択中の特別徴収義務者をセッションへ保存する。
	 * @param {HTMLElement} el data属性を持つ要素（チェックボックスまたは詳細リンク）
	 * @param {string} shiteiNo 指定番号
	 * @returns {Promise<boolean>} 保存に成功した場合 true
	 */
	async function saveSelected(el, shiteiNo) {
		try {
			await SessionManager.save(SG_SELECT_API, {
				atenaNo: el?.dataset.atenaNo || null,
				shiteiNo: shiteiNo,
				gassanShiteiNo: null,
				name: el?.dataset.name || null,
				shisetsuName: el?.dataset.shisetsuName || null
			});
			return true;
		} catch (err) {
			console.error(err);
			alert('選択した特別徴収義務者の保持に失敗しました。画面を再読み込みして再度お試しください。');
			return false;
		}
	}

	// 一覧の「詳細」。セッションに保存してから照会画面へ遷移する
	document.querySelectorAll('.detail-link').forEach(function (link) {
		link.addEventListener('click', async function (e) {
			e.preventDefault();
			e.stopPropagation();
			if (await saveSelected(this, this.dataset.shiteiNo)) {
				location.href = '/accommodation-tax/tokugimu/view';
			}
		});
	});

});
