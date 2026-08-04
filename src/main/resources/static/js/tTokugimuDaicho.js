// 特別徴収義務者台帳
// 指定番号はパラメータで渡さず、common.js の SessionManager でセッションに保存してから遷移する
const SG_SELECT_API = '/accommodation-tax/api/shitei-gassan/select';

document.addEventListener('DOMContentLoaded', function () {

	const rows = Array.from(document.querySelectorAll('.row-select')).map(cb => cb.closest('tr'));
	const pageSizeSelect = document.getElementById('pageSizeSelect');
	const pager = new Pagination(rows, pageSizeSelect, document.getElementById('pagination'));
	if (rows.length > 0) {
		pager.render(1);
		pageSizeSelect?.addEventListener('change', () => pager.render(1));
	}

	function requireSelected(msg) {
		const cb = document.querySelector('.row-select:checked');
		if (!cb) {
			alert(msg || 'レコードを選択してください。');
			return null;
		}
		return cb.dataset.shiteiNo;
	}

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

	// 行クリックでチェックボックスをトグル＋ハイライト
	document.querySelectorAll('tbody tr').forEach(function (row) {
		row.style.cursor = 'pointer';
		row.addEventListener('click', function (e) {
			if (e.target.closest('.btn, .detail-link, input[type="checkbox"]')) return;
			const cb = row.querySelector('.row-select');
			if (!cb) return;
			const next = !cb.checked;
			document.querySelectorAll('.row-select').forEach(o => {
				o.checked = false;
				o.closest('tr')?.classList.remove('row-selected');
			});
			cb.checked = next;
			row.classList.toggle('row-selected', next);
		});
	});

	document.querySelectorAll('.row-select').forEach(function (cb) {
		cb.addEventListener('click', function (e) {
			e.stopPropagation();
			document.querySelectorAll('.row-select').forEach(o => {
				if (o !== cb) {
					o.checked = false;
					o.closest('tr')?.classList.remove('row-selected');
				}
			});
			cb.closest('tr')?.classList.toggle('row-selected', cb.checked);
		});
	});

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

	// セッションに指定番号を保存してから遷移するヘルパー
	const navWithSession = (btnId, msg, url) =>
		document.getElementById(btnId)?.addEventListener('click', async () => {
			const id = requireSelected(msg);
			if (!id) return;
			const cb = document.querySelector('.row-select:checked');
			if (await saveSelected(cb, id)) {
				location.href = url;
			}
		});

	const nav = (btnId, msg, url) =>
		document.getElementById(btnId)?.addEventListener('click', () => {
			const id = requireSelected(msg);
			if (id) location.href = url.replace('{id}', id);
		});

	navWithSession('btnReport',        '特別徴収義務者を選択してください。',
	                                   '/accommodation-tax/tokugimu/report');
	navWithSession('btnPaymentLedger', '事業者を選択してください。',
	                                   '/accommodation-tax/declaration/payment-ledger');
	nav('btnTaxManager',      '特別徴収義務者を選択してください。',
	                          '/accommodation-tax/tax-manager/edit/{id}?from=register');
	nav('btnTaxManagerView',  '特別徴収義務者を選択してください。',
	                          '/accommodation-tax/tax-manager/view/{id}');
	nav('btnNozeiShuki',      '特別徴収義務者を選択してください。',
	                          '/accommodation-tax/tekiyo-nozei-shuki/edit/{id}?from=register');
	nav('btnNozeiShukiView',  '特別徴収義務者を選択してください。',
	                          '/accommodation-tax/tekiyo-nozei-shuki/view/{id}');

	document.getElementById('btnCorrection')?.addEventListener('click', () => {
		const id = requireSelected('特別徴収義務者を選択してください。');
		if (id) alert('更生請求画面は未実装です。');
	});
});
