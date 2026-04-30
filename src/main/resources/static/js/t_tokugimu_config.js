'use strict';

// -----------------------------------------------------------------------
// 初期化
// -----------------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    bindEvents();
});

// -----------------------------------------------------------------------
// イベントバインド
// -----------------------------------------------------------------------
function bindEvents() {
    // 確認モーダルを開くボタン
    const openModalBtn = document.getElementById('openConfirmModalBtn');
    if (openModalBtn) {
        openModalBtn.addEventListener('click', () => {
            const form = document.getElementById('registerForm');
            if (!form.checkValidity()) {
                form.reportValidity();
                return;
            }
            new bootstrap.Modal(document.getElementById('registerModal')).show();
        });
    }

    // 宛名検索モーダル初期化
    initAddressSearchModal();

    // 申告区分ラジオボタン
    document.querySelectorAll('input[name="declarationCategory"]').forEach(radio => {
        radio.addEventListener('change', onDeclarationTypeChange);
    });

    // 未定チェックボックス
    const undecided = document.getElementById('suspensionEndDateUndecided');
    if (undecided) {
        undecided.addEventListener('change', onUndecidedChange);
    }
}


// -----------------------------------------------------------------------
// 宛名検索モーダル
// -----------------------------------------------------------------------
const ADDR_API = '/accommodation-tax/api/address/search';

function initAddressSearchModal() {
    const searchBtn = document.getElementById('addrSearchBtn');
    if (!searchBtn) return;

    searchBtn.addEventListener('click', async () => {
        const no = document.getElementById('addrSearchNo').value.trim();
        const name = document.getElementById('addrSearchName').value.trim();
        const address = document.getElementById('addrSearchAddress').value.trim();
        const phone = document.getElementById('addrSearchPhone').value.trim();
        const kojinNo = document.getElementById('addrSearchKojinNo').value.trim();
        const hojinNo = document.getElementById('addrSearchHojinNo').value.trim();

        const params = new URLSearchParams();
        if (no) params.set('addressNumber', no);
        if (name) params.set('name', name);
        if (address) params.set('address', address);
        if (phone) params.set('phone', phone);
        if (kojinNo) params.set('kojinNo', kojinNo);
        if (hojinNo) params.set('hojinNo', hojinNo);

        try {
            const res = await fetch(`${ADDR_API}?${params}`);
            const data = await res.json();
            renderAddressResults(data);
        } catch (err) {
            document.getElementById('addrSearchResult').innerHTML =
                '<p class="text-danger small">通信エラーが発生しました。</p>';
        }
    });

    // Enterキーで検索
    ['addrSearchNo', 'addrSearchName', 'addrSearchAddress'].forEach(id => {
        document.getElementById(id)?.addEventListener('keydown', e => {
            if (e.key === 'Enter') { e.preventDefault(); searchBtn.click(); }
        });
    });
}

let _addrSearchResults = [];

function renderAddressResults(data) {
    const container = document.getElementById('addrSearchResult');
    if (!data.length) {
        container.innerHTML = '<p class="text-muted text-center small">該当する宛名が見つかりませんでした。</p>';
        return;
    }
    _addrSearchResults = data;
    const rows = data.map((d, i) => `
        <tr style="cursor:pointer" data-idx="${i}">
            <td>${d.addressNumber ?? ''}</td>
            <td>${d.name ?? ''}</td>
            <td>${d.nameKana ?? ''}</td>
            <td>${d.address ?? ''}</td>
            <td>${d.phone ?? ''}</td>
        </tr>`).join('');
    container.innerHTML = `
        <p class="small text-muted mb-1">行をクリックすると自動入力されます。</p>
        <div class="table-responsive">
            <table class="table table-sm table-hover table-bordered mb-0">
                <thead class="table-primary">
                    <tr>
                        <th>宛名番号</th><th>氏名</th><th>ふりがな</th><th>住所</th><th>電話番号</th>
                    </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
        </div>`;
    container.querySelectorAll('tbody tr').forEach(tr => {
        tr.addEventListener('click', () => selectAddress(_addrSearchResults[+tr.dataset.idx]));
    });
}

function selectAddress(d) {
    // 特別徴収義務者情報エリアに自動入力
    const set = (id, val) => { const el = document.getElementById(id); if (el) el.value = val ?? ''; };
    set('atenaNo', d.addressNumber);
    set('tokugimuAddress', d.address);
    set('name', d.name);
    set('tokugimuPhone', d.phone);
	set('personalNumber', d.kojinNo);
	set('corporateNumber', d.hojinNo);

    // モーダルを閉じる
    bootstrap.Modal.getInstance(document.getElementById('addressSearchModal')).hide();
}

// -----------------------------------------------------------------------
// 申告区分変更時の制御
// -----------------------------------------------------------------------
function onDeclarationTypeChange(e) {
    const value = e.target.value;
    const suspendStart = document.getElementById('suspensionStartDate');
    const suspendEnd = document.getElementById('suspensionEndDate');
    const undecided = document.getElementById('suspensionEndDateUndecided');
    const resumeClose = document.getElementById('resumptionOrAbolitionDate');

    [suspendStart, suspendEnd, undecided, resumeClose].forEach(el => {
        if (el) el.disabled = true;
    });

    if (value === '休止') {
        [suspendStart, suspendEnd, undecided].forEach(el => {
            if (el) el.disabled = false;
        });
    } else if (value === '再開' || value === '廃止') {
        if (resumeClose) resumeClose.disabled = false;
    }
}

// -----------------------------------------------------------------------
// 未定チェックボックス変更時の制御
// -----------------------------------------------------------------------
function onUndecidedChange(e) {
    const endDate = document.getElementById('suspensionEndDate');
    if (!endDate) return;
    if (e.target.checked) {
        endDate.disabled = true;
        endDate.value = '';
    } else {
        endDate.disabled = false;
    }
}
