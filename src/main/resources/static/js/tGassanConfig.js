'use strict';

const ADDR_API = '/accommodation-tax/api/address/search';
const FACILITIES_API = '/accommodation-tax/gassan/facilities-by-atena';

document.addEventListener('DOMContentLoaded', () => {
	
    // チェックされた行をハイライト
    setupFacilityEventListeners();
	

    // 画面読み込み時に初期の削除ボタン状態を判定
    updateDeleteButtonState();

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
    const searchModalEl = document.getElementById('addressSearchModal');
    const searchModal = bootstrap.Modal.getInstance(searchModalEl) || new bootstrap.Modal(searchModalEl);

    if (d.alreadyRegistered) {
        // 宛名検索モーダルを閉じる
        searchModal.hide();

        // モーダルが完全に閉じたタイミングで、確認用モーダルを表示する
        searchModalEl.addEventListener('hidden.bs.modal', function handler() {
			
            // 多重イベント発火を防止
            searchModalEl.removeEventListener('hidden.bs.modal', handler);

            const confirmModalEl = document.getElementById('alreadyRegisteredModal');
            const confirmModal = new bootstrap.Modal(confirmModalEl);
            confirmModal.show();

            // 「はい」の処理
            const yesBtn = document.getElementById('alreadyRegisteredYesBtn');
			
            // 既存のイベント重複防止で一度クローンして置き換え
            const newYesBtn = yesBtn.cloneNode(true);
            yesBtn.parentNode.replaceChild(newYesBtn, yesBtn);
            newYesBtn.addEventListener('click', () => {
                window.location.href = `/accommodation-tax/gassan/view-form/${d.gassanShiteiNo}`;
            });

            // 「いいえ」の処理
            const noBtn = document.getElementById('alreadyRegisteredNoBtn');
            const newNoBtn = noBtn.cloneNode(true);
            noBtn.parentNode.replaceChild(newNoBtn, noBtn);
            newNoBtn.addEventListener('click', () => {
                confirmModal.hide();
                resetAtenaSelection();

                // 確認用モーダルが完全に閉じた後、宛名検索モーダルを再表示する
                confirmModalEl.addEventListener('hidden.bs.modal', function searchModalHandler() {
                    confirmModalEl.removeEventListener('hidden.bs.modal', searchModalHandler);

                    const addressSearchModalEl = document.getElementById('addressSearchModal');
                    if (addressSearchModalEl) {
                        new bootstrap.Modal(addressSearchModalEl).show();
                    }
                }, { once: true });
            });
        }, { once: true });

        return;
    }

    // 未登録の場合の通常の処理
    searchModal.hide();

    const atenaNoInput = document.getElementById('atenaNo');
    const atenaNameDisplay = document.getElementById('atenaNameDisplay');
    if (atenaNoInput) atenaNoInput.value = d.addressNumber;
    if (atenaNameDisplay) atenaNameDisplay.value = d.name ?? '';

    // 宛名に紐づく特別徴収義務者一覧を取得
    await loadFacilitiesByAtena(d.addressNumber);
}

// 宛名選択をリセットし、関連する入力やボタンを非活性にする関数
function resetAtenaSelection() {
    const atenaNoInput = document.getElementById('atenaNo');
    const atenaNameDisplay = document.getElementById('atenaNameDisplay');
    if (atenaNoInput) atenaNoInput.value = '';
    if (atenaNameDisplay) atenaNameDisplay.value = '';

    // 施設一覧テーブルをクリア
    const tbody = document.querySelector('#gassanForm table tbody');
    if (tbody) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-4">
            <i class="bi bi-inbox fs-2 d-block mb-2 text-secondary"></i>対象施設が見つかりませんでした</td></tr>`;
    }

    // 登録ボタンを非活性化
    const registerBtn = document.getElementById('openConfirmModalBtn');
    if (registerBtn) {
        registerBtn.setAttribute('disabled', 'disabled');
        registerBtn.classList.add('disabled');
    }
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
        tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-4">
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
            <td>${f.shiteiNo ?? ''}</td>
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
			
			updateDeleteButtonState();
        });
    });

    document.querySelectorAll('.daihyo-radio').forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.checked) {
                const checkbox = this.closest('tr').querySelector('.facility-check');
                if (checkbox && !checkbox.disabled) {
                    checkbox.checked = true;
                    checkbox.closest('tr').classList.add('table-primary');
					
					updateDeleteButtonState();
                }
            }
        });
    });
}

// 削除モーダルを開く
document.getElementById('openDeleteModalBtn')?.addEventListener('click', () => {
    const modal = document.getElementById('deleteModal');
    if (modal) new bootstrap.Modal(modal).show();
});

// 削除確認モーダルの実行ボタンで削除フォーム送信
document.querySelector('#deleteModal .btn-confirm')?.addEventListener('click', () => {
    document.getElementById('deleteForm')?.submit();
});

// 削除ボタンの活性/非活性を制御する関数
function updateDeleteButtonState() {
    const deleteBtn = document.getElementById('openDeleteModalBtn');
    if (!deleteBtn) return;

    // チェックされている施設の数をカウント
    const checkedCount = document.querySelectorAll('.facility-check:checked').length;

    // 0件の場合は削除ボタンを無効化（disabled属性の付与とクラス調整）
    if (checkedCount <= 1) {
        deleteBtn.setAttribute('disabled', 'disabled');
        deleteBtn.classList.add('disabled');
    } else {
        deleteBtn.removeAttribute('disabled');
        deleteBtn.classList.remove('disabled');
    }
}
