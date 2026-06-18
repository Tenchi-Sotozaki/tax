'use strict';

document.addEventListener('DOMContentLoaded', () => {

    // 宛名検索モーダル初期化
    initAddressSearchModal();

    // 代表施設ラジオボタン制御
    document.querySelectorAll('.daihyo-radio').forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.checked) {
                // 対応するチェックボックスも自動でチェック
                const checkbox = this.closest('tr').querySelector('.facility-check');
                if (checkbox) {
                    checkbox.checked = true;
                    checkbox.closest('tr').classList.add('table-primary');
                }
            }
        });
    });

    // 適用時期セレクト変更時の遷移
    const tekiyoSelect = document.getElementById('tekiyoSelect');
    const fromShiteiNoVal = document.getElementById('fromShiteiNoVal');
    if (tekiyoSelect && fromShiteiNoVal) {
        tekiyoSelect.addEventListener('change', function () {
            location.href = '/accommodation-tax/gassan/view-by-shitei/'
                + fromShiteiNoVal.value + '?gassanShiteiNo=' + this.value;
        });
    }

    // 適用時期（年月）選択時に hidden inputへ yyyy-MM-01 をセット
    function syncMonthToDate(monthInputId, hiddenInputId) {
        const monthInput = document.getElementById(monthInputId);
        const hiddenInput = document.getElementById(hiddenInputId);
        if (!monthInput || !hiddenInput) return;
        const sync = () => {
            hiddenInput.value = monthInput.value ? monthInput.value + '-01' : '';
        };
        sync();
        monthInput.addEventListener('change', sync);
    }
    syncMonthToDate('tekiyoStYmdMonth', 'tekiyoStYmd');

    // チェックされた行をハイライト
    setupFacilityEventListeners();

    // 確認モーダルを開く前にチェック済み施設が1件以上あるか検証
    document.getElementById('openConfirmModalBtn')?.addEventListener('click', () => {
        const checked = document.querySelectorAll('.facility-check:checked');
        if (checked.length === 0) {
            alert('合算対象施設を1件以上選択してください。');
            return;
        }
        const modal = document.getElementById('registerModal');
        if (modal) new bootstrap.Modal(modal).show();
    });

    // 確認モーダルの実行ボタンでフォーム送信
    document.querySelector('#registerModal .btn-confirm')?.addEventListener('click', () => {
        document.getElementById('gassanForm')?.submit();
    });
});

// -----------------------------------------------------------------------
// 宛名検索モーダル
// -----------------------------------------------------------------------
const ADDR_API = '/accommodation-tax/api/address/search';

function initAddressSearchModal() {
    // 宛名検索ボタンをクリックした時にモーダルを開く
    document.getElementById('atenaSearchBtn')?.addEventListener('click', () => {
        const modal = document.getElementById('addressSearchModal');
        if (modal) {
            new bootstrap.Modal(modal).show();
        }
    });

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
    ['addrSearchNo', 'addrSearchName', 'addrSearchAddress', 'addrSearchPhone', 'addrSearchKojinNo', 'addrSearchHojinNo'].forEach(id => {
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
        <p class="small text-muted mb-1">行をクリックすると選択されます。</p>
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
    // 選択された宛名に紐づく施設一覧を取得
    loadFacilitiesByAtena(d.addressNumber, d.name);
    
    // モーダルを閉じる
    bootstrap.Modal.getInstance(document.getElementById('addressSearchModal')).hide();
}

// 宛名に紐づく施設一覧を取得する関数
function loadFacilitiesByAtena(atenaNo, atenaName) {
    // 宛名情報をフォームに設定
    const atenaNoInput = document.querySelector('input[name="atenaNo"]');
    if (atenaNoInput) {
        atenaNoInput.value = atenaNo;
    }
    
    // 宛名表示を更新
    const atenaDisplay = document.querySelector('#atenaSearchBtn').parentNode.querySelector('span');
    if (atenaDisplay) {
        atenaDisplay.textContent = '選択中: ' + atenaName;
    } else {
        // spanが存在しない場合は作成
        const span = document.createElement('span');
        span.className = 'ms-3 text-muted';
        span.textContent = '選択中: ' + atenaName;
        document.querySelector('#atenaSearchBtn').parentNode.appendChild(span);
    }

    // 施設一覧をサーバーから取得
    fetch('/accommodation-tax/gassan/facilities-by-atena', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest',
            [document.querySelector('meta[name="_csrf_header"]').content]: document.querySelector('meta[name="_csrf"]').content
        },
        body: JSON.stringify({ atenaNo: atenaNo })
    })
    .then(response => response.json())
    .then(facilities => {
        updateFacilityTable(facilities);
    })
    .catch(error => {
        console.error('施設一覧取得エラー:', error);
        alert('施設一覧の取得に失敗しました。');
    });
}

// 施設一覧テーブルを更新する関数
function updateFacilityTable(facilities) {
    const tbody = document.querySelector('.table tbody');
    if (!tbody) return;

    // 既存の行をクリア
    tbody.innerHTML = '';

    if (facilities.length === 0) {
        // 施設がない場合
        const row = document.createElement('tr');
        row.innerHTML = `
            <td colspan="4" class="text-center text-muted py-4">
                <i class="bi bi-inbox fs-2 d-block mb-2 text-secondary"></i>
                対象施設が見つかりませんでした。
            </td>
        `;
        tbody.appendChild(row);
        return;
    }

    // 施設一覧を表示
    facilities.forEach(facility => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td class="text-center">
                <input type="checkbox" class="form-check-input facility-check" 
                       name="shiteiNoList" value="${facility.shiteiNo}">
            </td>
            <td class="text-center">
                <input type="radio" class="form-check-input daihyo-radio" 
                       name="daihyoShiteiNo" value="${facility.shiteiNo}">
            </td>
            <td>${facility.choshuGimushaName || ''}</td>
            <td>${facility.shisetsuName || ''}</td>
        `;
        tbody.appendChild(row);
    });

    // 新しく作成されたチェックボックスとラジオボタンにイベントリスナーを追加
    setupFacilityEventListeners();
}

// 施設チェックボックスとラジオボタンのイベントリスナーを設定
function setupFacilityEventListeners() {
    // チェックされた行をハイライト
    document.querySelectorAll('.facility-check').forEach(cb => {
        cb.addEventListener('change', function() {
            this.closest('tr').classList.toggle('table-primary', this.checked);
            
            // チェックボックスが外された時、代表施設ラジオボタンもクリア
            if (!this.checked) {
                const radio = this.closest('tr').querySelector('.daihyo-radio');
                if (radio) {
                    radio.checked = false;
                }
            }
        });
    });

    // 代表施設ラジオボタン制御
    document.querySelectorAll('.daihyo-radio').forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.checked) {
                // 対応するチェックボックスも自動でチェック
                const checkbox = this.closest('tr').querySelector('.facility-check');
                if (checkbox) {
                    checkbox.checked = true;
                    checkbox.closest('tr').classList.add('table-primary');
                }
            }
        });
    });
}