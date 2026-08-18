// 特別徴収義務者台帳
// 指定番号はパラメータで渡さず、common.js の SessionManager でセッションに保存してから遷移する
const SG_SELECT_API = '/accommodation-tax/api/shitei-gassan/select';

document.addEventListener('DOMContentLoaded', function () {

	const paginationEl = document.getElementById('pagination');
	const pageSizeSelect = document.getElementById('pageSizeSelect');

	if (paginationEl) {
		const currentPage = parseInt(paginationEl.dataset.currentPage ?? '0', 10);
		const totalPages = parseInt(paginationEl.dataset.totalPages ?? '0', 10);
		renderServerPagination(paginationEl, currentPage, totalPages);
	}

	pageSizeSelect?.addEventListener('change', () => {
		const url = new URL(location.href);
		url.searchParams.set('pageSize', pageSizeSelect.value);
		url.searchParams.set('page', '0');
		url.searchParams.set('searched', 'true');
		location.href = url.toString();
	});

	function renderServerPagination(ul, currentPage, totalPages) {
		if (totalPages === 0) return;
		const half = 2;

		function addBtn(label, page, active, disabled) {
			const li = document.createElement('li');
			li.className = 'page-item' + (active ? ' active' : '') + (disabled ? ' disabled' : '');
			const a = document.createElement('a');
			a.className = 'page-link';
			a.href = '#';
			a.textContent = label;
			if (!disabled && !active) {
				a.addEventListener('click', e => {
					e.preventDefault();
					const url = new URL(location.href);
					url.searchParams.set('page', page);
					url.searchParams.set('searched', 'true');
					location.href = url.toString();
				});
			}
			li.appendChild(a);
			ul.appendChild(li);
		}

		addBtn('前へ', currentPage - 1, false, currentPage === 0);

		const winStart = currentPage - half;
		const winEnd = currentPage + half;

		if (winStart > 0) {
			addBtn('1', 0, false, false);
			if (winStart > 1) addBtn('…', null, false, true);
		}

		for (let p = Math.max(0, winStart); p <= Math.min(totalPages - 1, winEnd); p++) {
			addBtn(String(p + 1), p, p === currentPage, false);
		}

		if (winEnd < totalPages - 1) {
			if (winEnd < totalPages - 2) addBtn('…', null, false, true);
			addBtn(String(totalPages), totalPages - 1, false, false);
		}

		addBtn('次へ', currentPage + 1, false, currentPage === totalPages - 1);
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
