'use strict';

const SG_SEARCH_API = '/accommodation-tax/api/shitei-gassan/search';
const SG_SELECT_API = '/accommodation-tax/api/shitei-gassan/select';

let _sgSearchResults = [];
let _sgSelectCallback = null;

/**
 * 特別徴収義務者指定モーダルを初期化する
 * @param {Function} onSelect - 行選択時のコールバック関数。引数: { atenaNo, shiteiNo, gassanShiteiNo, name, shisetsuName }
 */
function initShiteiGassanSearchModal(onSelect) {
    _sgSelectCallback = onSelect;

    const searchBtn = document.getElementById('sgSearchBtn');
    if (!searchBtn) return;

    searchBtn.addEventListener('click', executeShiteiGassanSearch);

    ['sgSearchShiteiNo', 'sgSearchGassanShiteiNo', 'sgSearchName', 'sgSearchShisetsuName', 'sgSearchKojinNo', 'sgSearchHojinNo'].forEach(id => {
        document.getElementById(id)?.addEventListener('keydown', e => {
            if (e.key === 'Enter') { e.preventDefault(); searchBtn.click(); }
        });
    });
}

// 自動初期化
document.addEventListener('DOMContentLoaded', () => {
    initShiteiGassanSearchModal(null);

    // showShiteiModalフラグがtrueの場合、モーダルを自動オープン
    const container = document.querySelector('[data-show-shitei-modal]');
    if (container && container.dataset.showShiteiModal === 'true') {
        const modal = document.getElementById('shiteiGassanSearchModal');
        if (modal) {
            bootstrap.Modal.getOrCreateInstance(modal).show();
            modal.addEventListener('hidden.bs.modal', () => {
                document.body.classList.remove('modal-open');
                document.body.style.removeProperty('overflow');
                document.body.style.removeProperty('padding-right');
                document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
            }, { once: true });
        }
    }
});

async function executeShiteiGassanSearch() {
    const params = new URLSearchParams();
    const val = (id) => document.getElementById(id)?.value.trim() || '';

    if (val('sgSearchShiteiNo')) params.set('shiteiNo', val('sgSearchShiteiNo'));
    if (val('sgSearchGassanShiteiNo')) params.set('gassanShiteiNo', val('sgSearchGassanShiteiNo'));
    if (val('sgSearchName')) {
        params.set('name', val('sgSearchName'));
        params.set('nameMatchType', document.querySelector('input[name="sgSearchNameMatchType"]:checked')?.value || 'partial');
    }
    if (val('sgSearchShisetsuName')) {
        params.set('shisetsuName', val('sgSearchShisetsuName'));
        params.set('shisetsuNameMatchType', document.querySelector('input[name="sgSearchShisetsuNameMatchType"]:checked')?.value || 'partial');
    }
    if (val('sgSearchKojinNo')) params.set('kojinNo', val('sgSearchKojinNo'));
    if (val('sgSearchHojinNo')) params.set('hojinNo', val('sgSearchHojinNo'));

    try {
        const res = await fetch(`${SG_SEARCH_API}?${params}`);
        const data = await res.json();
        data.sort((a, b) => (a.shiteiNo ?? '').localeCompare(b.shiteiNo ?? ''));
        renderShiteiGassanResults(data);
    } catch (err) {
        document.getElementById('sgSearchResult').innerHTML =
            '<p class="text-danger small">通信エラーが発生しました。</p>';
    }
}

function renderShiteiGassanResults(data) {
    const container = document.getElementById('sgSearchResult');
    if (!data.length) {
        container.innerHTML = '<p class="text-muted text-center small">該当する特別徴収義務者が見つかりませんでした。</p>';
        return;
    }
    _sgSearchResults = data;
    const rows = data.map((d, i) => `
        <tr style="cursor:pointer" data-idx="${i}">
            <td>${d.gassanShiteiNo ? '' : (d.shiteiNo ?? '')}</td>
            <td>${d.gassanShiteiNo ?? ''}</td>
            <td>${d.name ?? ''}</td>
            <td>${d.shisetsuName ?? ''}</td>
        </tr>`).join('');
    container.innerHTML = `
        <p class="small text-muted mb-1">行をクリックすると選択されます。</p>
        <div class="table-responsive">
            <table class="table table-sm table-hover table-bordered mb-0">
                <thead class="table-primary">
                    <tr>
                        <th>指定番号</th><th>合算指定番号</th><th>氏名/名称</th><th>施設名称</th>
                    </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
        </div>`;
    container.querySelectorAll('tbody tr').forEach(tr => {
        tr.addEventListener('click', () => selectShiteiGassan(_sgSearchResults[+tr.dataset.idx]));
    });
}

async function selectShiteiGassan(d) {
    // 合算指定番号がある場合は shiteiNo を保存しない
    const payload = d.gassanShiteiNo
        ? { gassanShiteiNo: d.gassanShiteiNo, name: d.name, shisetsuName: d.shisetsuName }
        : d;

    // セッションに保存
    try {
        await SessionManager.save(SG_SELECT_API, payload);
    } catch (err) {
        console.error('セッション保存エラー:', err);
    }

    if (_sgSelectCallback) {
        _sgSelectCallback(d);
    }
    bootstrap.Modal.getInstance(document.getElementById('shiteiGassanSearchModal')).hide();
    // ページをリロードしてセッション情報を反映
    location.reload();
}
