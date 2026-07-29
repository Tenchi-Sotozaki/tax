'use strict';

const ADDR_API = '/accommodation-tax/api/address/search';
const FACILITIES_API = '/accommodation-tax/gassan/facilities-by-atena';

document.addEventListener('DOMContentLoaded', () => {

    // 宛名検索モーダル初期化
    initAddressSearchModal();

    // showAddressModalフラグがtrueの場合、モーダルを自動オープン
    const container = document.querySelector('[data-show-address-modal]');
    if (container && container.dataset.showAddressModal === 'true') {
        const modal = document.getElementById('addressSearchModal');
        if (modal) new bootstrap.Modal(modal).show();
    }

    // 代表施設ラジオボタン制御
    document.querySelectorAll('.daihyo-radio').forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.checked) {
                const checkbox = this.closest('tr').querySelector('.facility-check');
                if (checkbox && !checkbox.disabled) {
                    checkbox.checked = true;
                    checkbox.closest('tr').classList.add('table-primary');
                }
            }
        });
    });

    // チェックされた行をハイライト
    setupFacilityEventListeners();

    // 確認モーダルを開く前にチェック済み施設が2件以上あるか検証
    document.getElementById('openConfirmModalBtn')?.addEventListener('click', () => {
        const checked = document.querySelectorAll('.facility-check:checked');
        if (checked.length === 0) {
            alert('合算対象施設を2件以上選択してください。');
            return;
        }
        const modal = document.getElementById('registerModal');
        if (modal) new bootstrap.Modal(modal).show();
    });

    // 確認モーダルの実行ボタンでフォーム送信
    document.querySelector('#registerModal .btn-confirm')?.addEventListener('click', () => {
        document.getElementById('gassanForm')?.submit();
    });

    // 編集モードでなければ変更検知処理を行わない
    const contentContainer = document.querySelector('[data-is-edit]');
    const isEdit = contentContainer ? contentContainer.getAttribute('data-is-edit') === 'true' : false;
    if (!isEdit) return;

    // 編集変更チェック
    function checkValue(input) {
        if (input.type === 'checkbox' || input.type === 'radio') return;
        if (input.closest('.modal')) return;
        const initialValue = input.getAttribute('data-initial-value') || '';
        let initialStr = (initialValue === null || initialValue === 'null') ? '' : String(initialValue).trim();
        let currentStr = String(input.value).trim();
        if (currentStr !== initialStr) {
            input.style.border = '3px solid #ffeb3b';
        } else {
            input.style.border = '';
        }
    }

    const inputs = document.querySelectorAll('.form-control, .form-select');
    inputs.forEach(input => {
        checkValue(input);
        input.addEventListener('input', () => checkValue(input));
        input.addEventListener('change', () => checkValue(input));
        input.addEventListener('blur', () => checkValue(input));
    });
});

// 宛名検索モーダル
function initAddressSearchModal() {
    const searchBtn = document.getElementById('addrSearchBtn');
    if (!searchBtn) return;

    searchBtn.addEventListener('click', async () => {
        const params = new URLSearchParams();
        const val = (id) => document.getElementById(id)?.value.trim() || '';
        if (val('addrSearchNo')) params.set('addressNumber', val('addrSearchNo'));
        if (val('addrSearchName')) params.set('name', val('addrSearchName'));
        if (val('addrSearchAddress')) params.set('address', val('addrSearchAddress'));
        if (val('addrSearchPhone')) params.set('phone', val('addrSearchPhone'));
        if (val('addrSearchKojinNo')) params.set('kojinNo', val('addrSearchKojinNo'));
        if (val('addrSearchHojinNo')) params.set('hojinNo', val('addrSearchHojinNo'));

        try {
            const res = await fetch(`${ADDR_API}?${params}`);
            const data = await res.json();
            renderAddressResults(data);
        } catch (err) {
            document.getElementById('addrSearchResult').innerHTML =
                '<p class="text-danger small">通信エラーが発生しました。</p>';
        }
    });

    ['addrSearchNo', 'addrSearchName', 'addrSearchAddress', 'addrSearchPhone', 'addrSearchKojinNo', 'addrSearchHojinNo'].forEach(id => {
        document.getElementById(id)?.addEventListener('keydown', e => {
            if (e.key === 'Enter') { e.preventDefault(); searchBtn.click(); }
        });
    });
}

function renderAddressResults(data) {
    const container = document.getElementById('addrSearchResult');
    if (!data.length) {
        container.innerHTML = '<p class="text-muted text-center small">該当する宛名が見つかりませんでした。</p>';
        return;
    }
    const rows = data.map((d, i) => `
        <tr style="cursor:pointer" data-idx="${i}">
            <td>${d.addressNumber ?? ''}</td>
            <td>${d.name ?? ''}</td>
            <td>${d.nameKana ?? ''}</td>
            <td>${d.address ?? ''}</td>
        </tr>`).join('');
    container.innerHTML = `
        <p class="small text-muted mb-1">行をクリックすると選択されます。</p>
        <div class="table-responsive">
            <table class="table table-sm table-hover table-bordered mb-0">
                <thead class="table-primary">
                    <tr><th>宛名番号</th><th>氏名</th><th>ふりがな</th><th>住所</th></tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
        </div>`;
    container.querySelectorAll('tbody tr').forEach(tr => {
        tr.addEventListener('click', () => selectAddress(data[+tr.dataset.idx]));
    });
}

async function selectAddress(d) {
    // 宛名番号と宛名名をフォームにセット
    const atenaNoInput = document.getElementById('atenaNo');
    const atenaNameDisplay = document.getElementById('atenaNameDisplay');
    if (atenaNoInput) atenaNoInput.value = d.addressNumber;
    if (atenaNameDisplay) atenaNameDisplay.value = d.name ?? '';

    // モーダルを閉じる
    bootstrap.Modal.getInstance(document.getElementById('addressSearchModal')).hide();

    // 宛名に紐づく特別徴収義務者一覧を取得
    await loadFacilitiesByAtena(d.addressNumber);
}

async function loadFacilitiesByAtena(atenaNo) {
    try {
        const facilities = await SessionManager.save(FACILITIES_API, { atenaNo: atenaNo });
        renderFacilityTable(facilities);
    } catch (err) {
        console.error('施設一覧取得エラー:', err);
    }
}

function renderFacilityTable(facilities) {
    const tbody = document.querySelector('#gassanForm table tbody');
    if (!tbody) return;

    if (!facilities.length) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">
            <i class="bi bi-inbox fs-2 d-block mb-2 text-secondary"></i>対象施設が見つかりませんでした。</td></tr>`;
        return;
    }

    tbody.innerHTML = facilities.map(f => `
        <tr>
            <td class="text-center">
                <input type="checkbox" class="form-check-input facility-check"
                    name="shiteiNoList" value="${f.shiteiNo}">
            </td>
            <td class="text-center">
                <input type="radio" class="form-check-input daihyo-radio"
                    name="daihyoShiteiNo" value="${f.shiteiNo}">
            </td>
            <td>${f.choshuGimushaName ?? ''}</td>
            <td>${f.shisetsuName ?? ''}</td>
        </tr>`).join('');

    setupFacilityEventListeners();
}

// 施設チェックボックスとラジオボタンのイベントリスナーを設定
function setupFacilityEventListeners() {
    document.querySelectorAll('.facility-check').forEach(cb => {
        cb.addEventListener('change', function() {
            this.closest('tr').classList.toggle('table-primary', this.checked);
            if (!this.checked) {
                const radio = this.closest('tr').querySelector('.daihyo-radio');
                if (radio) radio.checked = false;
            }
        });
    });

    document.querySelectorAll('.daihyo-radio').forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.checked) {
                const checkbox = this.closest('tr').querySelector('.facility-check');
                if (checkbox && !checkbox.disabled) {
                    checkbox.checked = true;
                    checkbox.closest('tr').classList.add('table-primary');
                }
            }
        });
    });
}
