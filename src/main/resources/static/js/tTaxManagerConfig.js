'use strict';

// -----------------------------------------------------------------------
// 初期化
// -----------------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    bindEvents();
    initAddressSearchModal();
    initSamePersonCheck();
    initChangeHighlight();
});

// -----------------------------------------------------------------------
// イベントバインド
// -----------------------------------------------------------------------
function bindEvents() {
    const kbn = document.getElementById('kbn');
    if (kbn) {
        toggleKbnAreas(kbn.value);
        kbn.addEventListener('change', () => toggleKbnAreas(kbn.value));
    }
}

function toggleKbnAreas(val) {
    const reasonArea = document.getElementById('reasonArea');
    if (reasonArea) {
        reasonArea.style.display = (val === '2' || val === '3') ? 'block' : 'none';
    }
    const addrSearchBtn = document.querySelector('[data-bs-target="#addressSearchModal"]');
    if (addrSearchBtn) addrSearchBtn.disabled = (val === '3');
    if (val === '3') {
        ['managerYubinNo', 'managerAddress', 'managerName', 'managerNameKana', 'managerPhone'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = '';
        });
        const atenaNoField = document.querySelector('input[name="atenaNo"]');
        if (atenaNoField) atenaNoField.value = '';
        hideCheckMessage();
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
        const nameMatchType = document.querySelector('input[name="addrSearchNameMatchType"]:checked')?.value ?? 'partial';
        const address = document.getElementById('addrSearchAddress').value.trim();
        const addressMatchType = document.querySelector('input[name="addrSearchAddressMatchType"]:checked')?.value ?? 'partial';
        const phone = document.getElementById('addrSearchPhone').value.trim();
        const kojinNo = document.getElementById('addrSearchKojinNo').value.trim();
        const hojinNo = document.getElementById('addrSearchHojinNo').value.trim();

        const params = new URLSearchParams();
        if (no) params.set('addressNumber', no);
        if (name) { params.set('name', name); params.set('nameMatchType', nameMatchType); }
        if (address) { params.set('address', address); params.set('addressMatchType', addressMatchType); }
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
            <td>${d.yubinNo ?? ''}</td>
            <td>${d.address ?? ''}</td>
            <td>${d.phone ?? ''}</td>
        </tr>`).join('');
    container.innerHTML = `
        <p class="small text-muted mb-1">行をクリックすると自動入力されます。</p>
        <div class="table-responsive">
            <table class="table table-sm table-hover table-bordered mb-0">
                <thead class="table-primary">
                    <tr>
                        <th>宛名番号</th><th>氏名</th><th>ふりがな</th><th>郵便番号</th><th>住所</th><th>電話番号</th>
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
    // 納税管理人情報エリアに自動入力
    const set = (id, val) => { const el = document.getElementById(id); if (el) el.value = val ?? ''; };
    const setByName = (name, val) => { const el = document.querySelector(`input[name="${name}"]`); if (el) el.value = val ?? ''; };
    
    set('managerYubinNo', d.yubinNo);
    set('managerAddress', d.address);
    set('managerName', d.name);
    set('managerNameKana', d.nameKana);
    set('managerPhone', d.phone);
    
    // 宛名番号をhiddenフィールドに設定
    setByName('atenaNo', d.addressNumber);
    
    // 前回のエラーメッセージをクリア
    hideCheckMessage();
    
    // モーダルを閉じる（即座に）
    const modal = bootstrap.Modal.getInstance(document.getElementById('addressSearchModal'));
    if (modal) {
        modal.hide();
    }
    
    // モーダルが完全に閉じた後に同一人物チェックを実行
    setTimeout(() => {
        checkSamePerson(d.addressNumber);
    }, 300); // モーダルのアニメーション完了を待つ
}

// -----------------------------------------------------------------------
// 同一人物チェック
// -----------------------------------------------------------------------
function initSamePersonCheck() {
    // 画面初期表示時にチェック実行（既存データがある場合）
    const atenaNo = document.querySelector('input[name="atenaNo"]')?.value;
    const sessionObligorAtenaNo = document.getElementById('sessionObligorAtenaNo')?.value;

    if (atenaNo && sessionObligorAtenaNo) {
        checkSamePerson(atenaNo);
    }
}

function checkSamePerson(selectedAtenaNo) {
    const sessionObligorAtenaNo = document.getElementById('sessionObligorAtenaNo')?.value?.trim();

    if (!selectedAtenaNo || !sessionObligorAtenaNo) {
        hideCheckMessage();
        return;
    }

    if (selectedAtenaNo.trim() === sessionObligorAtenaNo) {
        showCheckMessage('特別徴収義務者と同一人物のため、納税管理人として登録できません。', true);
    } else {
        showCheckMessage('登録可能です。', false);
    }
}

function showCheckMessage(message, isError) {
    const messageDiv = document.getElementById('atenaCheckMessage');
    if (!messageDiv) return;
    
    messageDiv.style.display = 'block';
    messageDiv.className = `alert alert-sm p-2 mb-0 ${isError ? 'alert-danger' : 'alert-success'}`;
    messageDiv.innerHTML = `<i class="bi ${isError ? 'bi-exclamation-triangle' : 'bi-check-circle'}"></i> ${message}`;
    
    // 登録ボタンの制御
    const submitBtns = document.querySelectorAll('button[type="submit"], input[type="submit"]');
    submitBtns.forEach(btn => {
        btn.disabled = isError;
    });
}

function hideCheckMessage() {
    const messageDiv = document.getElementById('atenaCheckMessage');
    if (messageDiv) {
        messageDiv.style.display = 'none';
    }
    
    // 登録ボタンを有効化
    const submitBtns = document.querySelectorAll('button[type="submit"], input[type="submit"]');
    submitBtns.forEach(btn => {
        btn.disabled = false;
    });
}

// -----------------------------------------------------------------------
// 値の変更を監視して色を変える処理
// -----------------------------------------------------------------------
function initChangeHighlight() {
    const inputs = document.querySelectorAll('.form-control, .form-check-input, .form-select');

    function checkValue(input) {
        const initialValue = input.getAttribute('data-initial-value');
        if (initialValue === null) return;
        const isChanged = input.type === 'checkbox'
            ? (input.checked ? 'true' : 'false') !== initialValue
            : input.value !== initialValue;
        if (isChanged) {
            input.classList.add('form-control-edited');
        } else {
			input.classList.remove('form-control-edited');
            input.style.border = '';
        }
    }

    inputs.forEach(input => {
        input.addEventListener('input', () => checkValue(input));
        input.addEventListener('change', () => checkValue(input));
    });

    const readonlyInputs = document.querySelectorAll('input[readonly], input[disabled], textarea[readonly]');
    document.addEventListener('click', () => readonlyInputs.forEach(checkValue));
    document.addEventListener('change', () => readonlyInputs.forEach(checkValue));
}