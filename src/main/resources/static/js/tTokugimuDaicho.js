// DOMの読み込み待ち
document.addEventListener('DOMContentLoaded', function() {

    function requireSelected(msg) {
        const cb = document.querySelector('.row-select:checked');
        if (!cb) { 
            alert(msg || 'レコードを選択してください。'); 
            return null; 
        }
        return cb.dataset.shiteiNo;
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

    // 行内削除ボタン
	document.querySelectorAll('.delete-btn').forEach(function (btn) {
	        btn.addEventListener('click', function (e) {
	            e.stopPropagation();
	            const id = this.dataset.shiteiNo, name = this.dataset.name;
	            const modal = document.getElementById('deleteModal');
            modal.querySelector('.modal-body p').textContent =
                '「' + name + '」を削除します。この操作は取り消せません。よろしいですか？';
            modal.querySelector('[data-form-id]').dataset.formId = 'deleteForm-' + id;
            new bootstrap.Modal(modal).show();
        });
    });

    // 選択した特別徴収義務者をセッションに保存してから遷移する
    const saveSessionThenGo = (dto, url) => {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
        const headers = { 'Content-Type': 'application/json' };
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
        fetch('/accommodation-tax/api/shitei-gassan/select', {
            method: 'POST', headers, body: JSON.stringify(dto)
        }).then(res => {
            if (!res.ok) throw new Error('セッション保存に失敗しました');
            location.href = url;
        }).catch(err => {
            console.error(err);
            alert('遷移に失敗しました。画面を再読み込みして再度お試しください。');
        });
    };

    // 一覧の「詳細」。指定番号はパラメータで渡さずセッション経由で照会画面へ遷移する
    document.querySelectorAll('.detail-link').forEach(function (link) {
        link.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            saveSessionThenGo({
                atenaNo: this.dataset.atenaNo || null,
                shiteiNo: this.dataset.shiteiNo,
                gassanShiteiNo: null,
                name: this.dataset.name || null,
                shisetsuName: this.dataset.shisetsuName || null
            }, '/accommodation-tax/tokugimu/view');
        });
    });

    // セッションに指定番号を保存してから遷移するヘルパー
	const navWithSession = (btnId, msg, url) =>
	    document.getElementById(btnId)?.addEventListener('click', () => {
	        const id = requireSelected(msg);
	        if (!id) return;
	        const cb = document.querySelector('.row-select:checked');
	        const dto = {
	            atenaNo: cb?.dataset.atenaNo || null,
	            shiteiNo: id,
	            gassanShiteiNo: null,
	            name: cb?.dataset.name || null,
	            shisetsuName: cb?.dataset.shisetsuName || null
	        };
	        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
	        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
	        const headers = { 'Content-Type': 'application/json' };
	        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
	        fetch('/accommodation-tax/api/shitei-gassan/select', {
	            method: 'POST', headers, body: JSON.stringify(dto)
	        }).then(res => {
	            if (!res.ok) throw new Error('セッション保存に失敗しました');
	            location.href = url;
	        }).catch(err => {
	            console.error(err);
	            alert('遷移に失敗しました。画面を再読み込みして再度お試しください。');
	        });
	    });

	const nav = (btnId, msg, url) =>
	    document.getElementById(btnId)?.addEventListener('click', () => {
	        const id = requireSelected(msg);
	        if (id) location.href = url.replace('{id}', id);
	    });

    navWithSession('btnView',          '照会する特別徴収義務者を選択してください。',
                                       '/accommodation-tax/tokugimu/view');
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

    document.getElementById('btnDelete')?.addEventListener('click', () => {
        const id = requireSelected('削除するレコードを選択してください。');
        if (!id) return;
        
        const modal = document.getElementById('deleteModal');
        if (!modal) {
            console.error('削除モーダルが見つかりません');
            return;
        }
        
        const formElement = document.getElementById('deleteForm-' + id);
        if (!formElement) {
            console.error('削除フォームが見つかりません: deleteForm-' + id);
            return;
        }
        
        const confirmButton = modal.querySelector('[data-form-id]');
        if (confirmButton) {
            confirmButton.dataset.formId = 'deleteForm-' + id;
        }
        
        new bootstrap.Modal(modal).show();
    });
    document.getElementById('btnCorrection')?.addEventListener('click', () => {
        const id = requireSelected('特別徴収義務者を選択してください。');
        if (id) alert('更生請求画面は未実装です。');
    });
});
