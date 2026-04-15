'use strict';

// -----------------------------------------------------------------------
// 定数
// -----------------------------------------------------------------------
const API_BASE = '/accommodation-tax/api/tax-managers';

// -----------------------------------------------------------------------
// 状態
// -----------------------------------------------------------------------
let editingTaxManagerId = null; // 編集時にセット
let selectedAddressData = null; // 宛名検索で選択されたデータ
const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

// -----------------------------------------------------------------------
// 初期化
// -----------------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    bindEvents();
    
    // URLパラメータに id があれば編集モードで読み込む
    const params = new URLSearchParams(location.search);
    const id = params.get('id');
    if (id) {
        editingTaxManagerId = id;
        loadTaxManager(id);
        switchToEditMode();
    }
});

// -----------------------------------------------------------------------
// イベントバインド
// -----------------------------------------------------------------------
function bindEvents() {
    // フォーム送信
    document.getElementById('taxManagerForm').addEventListener('submit', onSubmit);
    
    // 削除ボタン
    document.getElementById('deleteBtn').addEventListener('click', onDelete);
    
    // 選任免除チェックボックス
    document.getElementById('exemptionFlag').addEventListener('change', onExemptionFlagChange);
    
    // 宛名検索フォーム
    document.getElementById('addressSearchForm').addEventListener('submit', onAddressSearch);
    
    // 宛名選択ボタン
    document.getElementById('selectAddressBtn').addEventListener('click', onSelectAddress);
    
    // 削除確認
    document.getElementById('confirmDeleteBtn').addEventListener('click', onConfirmDelete);
}

// -----------------------------------------------------------------------
// フォーム送信
// -----------------------------------------------------------------------
function onSubmit(e) {
    e.preventDefault();
    clearAllErrors();

    if (!validateForm()) return;

    const data = buildRequestBody();
    submitData(data);
}

// -----------------------------------------------------------------------
// データ送信
// -----------------------------------------------------------------------
async function submitData(data) {
    const isEdit = !!editingTaxManagerId;
    const url = isEdit ? `${API_BASE}/${editingTaxManagerId}` : API_BASE;
    const method = isEdit ? 'PUT' : 'POST';
    
    const btn = isEdit ? document.getElementById('updateBtn') : document.getElementById('registerBtn');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>送信中...';

    try {
        const res = await fetch(url, {
            method,
            headers: buildHeaders(),
            body: JSON.stringify(data),
        });

        const json = await res.json();

        if (!res.ok) {
            handleApiError(json);
            return;
        }

        showAlert('success',
            `<i class="bi bi-check-circle me-2"></i>` +
            `納税管理人を${isEdit ? '更新' : '登録'}しました。`
        );
        
        if (!isEdit) {
            editingTaxManagerId = json.taxManagerId;
            switchToEditMode();
            history.replaceState(null, '', `?id=${json.taxManagerId}`);
        }

    } catch (err) {
        showAlert('danger', '<i class="bi bi-exclamation-triangle me-2"></i>通信エラーが発生しました。');
        console.error(err);
    } finally {
        btn.disabled = false;
        btn.innerHTML = isEdit 
            ? '<i class="bi bi-arrow-repeat me-2"></i>更新する'
            : '<i class="bi bi-save me-2"></i>登録する';
    }
}

// -----------------------------------------------------------------------
// 削除処理
// -----------------------------------------------------------------------
function onDelete() {
    bootstrap.Modal.getOrCreateInstance(document.getElementById('deleteConfirmModal')).show();
}

async function onConfirmDelete() {
    bootstrap.Modal.getInstance(document.getElementById('deleteConfirmModal')).hide();
    
    if (!editingTaxManagerId) return;

    try {
        const res = await fetch(`${API_BASE}/${editingTaxManagerId}`, {
            method: 'DELETE',
            headers: buildHeaders(),
        });

        if (!res.ok) {
            const json = await res.json();
            handleApiError(json);
            return;
        }

        showAlert('success', '<i class="bi bi-check-circle me-2"></i>納税管理人を削除しました。');
        
        // 一覧画面に戻る
        setTimeout(() => {
            history.back();
        }, 2000);

    } catch (err) {
        showAlert('danger', '<i class="bi bi-exclamation-triangle me-2"></i>通信エラーが発生しました。');
        console.error(err);
    }
}

// -----------------------------------------------------------------------
// 既存データの読み込み（編集モード）
// -----------------------------------------------------------------------
async function loadTaxManager(id) {
    try {
        const res = await fetch(`${API_BASE}/${id}`, { headers: buildHeaders() });
        if (!res.ok) {
            showAlert('danger', `納税管理人ID: ${id} の読み込みに失敗しました。`);
            return;
        }
        const data = await res.json();
        fillForm(data);
    } catch (err) {
        showAlert('danger', '通信エラーが発生しました。');
        console.error(err);
    }
}

// -----------------------------------------------------------------------
// フォームにデータを設定
// -----------------------------------------------------------------------
function fillForm(data) {
    setFieldValue('registrationDate', data.registrationDate);
    setFieldValue('obligorName', data.obligorName);
    setFieldValue('facilityName', data.facilityName);
    setFieldValue('managerAddress', data.managerAddress);
    setFieldValue('managerName', data.managerName);
    setFieldValue('managerNameKana', data.managerNameKana);
    setFieldValue('managerPhone', data.managerPhone);
    
    document.getElementById('exemptionFlag').checked = data.exemptionFlag || false;
    setFieldValue('exemptionReason', data.exemptionReason);
    
    // 選任免除の表示制御
    onExemptionFlagChange();
}

// -----------------------------------------------------------------------
// 編集モードに切り替え
// -----------------------------------------------------------------------
function switchToEditMode() {
    document.getElementById('registerBtn').style.display = 'none';
    document.getElementById('updateBtn').style.display = 'inline-block';
    document.getElementById('deleteBtn').style.display = 'inline-block';
    
    const label = document.getElementById('modeLabel');
    if (label) {
        label.innerHTML = '<i class="bi bi-pencil me-1"></i>編集中';
        label.className = 'badge bg-warning-subtle text-warning border border-warning-subtle px-3 py-2 fs-6';
    }
}

// -----------------------------------------------------------------------
// 選任免除チェックボックス変更時の制御
// -----------------------------------------------------------------------
function onExemptionFlagChange() {
    const exemptionFlag = document.getElementById('exemptionFlag');
    const exemptionReasonArea = document.getElementById('exemptionReasonArea');
    const exemptionReason = document.getElementById('exemptionReason');
    
    if (exemptionFlag.checked) {
        exemptionReasonArea.style.display = 'block';
        exemptionReason.required = true;
    } else {
        exemptionReasonArea.style.display = 'none';
        exemptionReason.required = false;
        exemptionReason.value = '';
        exemptionReason.classList.remove('is-invalid');
    }
}

// -----------------------------------------------------------------------
// 宛名検索
// -----------------------------------------------------------------------
function onAddressSearch(e) {
    e.preventDefault();
    
    const searchData = {
        addressNumber: document.getElementById('searchAddressNumber').value.trim(),
        name: document.getElementById('searchName').value.trim(),
        address: document.getElementById('searchAddress').value.trim()
    };
    
    // モック検索結果
    const mockResults = [
        { id: 1, addressNumber: 'A001001', name: '田中太郎', address: '東京都新宿区西新宿1-1-1' },
        { id: 2, addressNumber: 'A001002', name: '佐藤花子', address: '東京都渋谷区渋谷2-2-2' },
        { id: 3, addressNumber: 'A001003', name: '鈴木一郎', address: '東京都港区六本木3-3-3' }
    ];
    
    displaySearchResults(mockResults);
}

function displaySearchResults(results) {
    const tbody = document.getElementById('searchResultTableBody');
    const resultArea = document.getElementById('searchResultArea');
    
    tbody.innerHTML = '';
    
    if (results.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">検索結果がありません</td></tr>';
    } else {
        results.forEach(result => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-center">
                    <input type="radio" name="selectedAddress" value="${result.id}" class="form-check-input" data-result='${JSON.stringify(result)}'>
                </td>
                <td>${result.addressNumber}</td>
                <td>${result.name}</td>
                <td>${result.address}</td>
            `;
            tbody.appendChild(tr);
        });
        
        // ラジオボタン選択時の制御
        tbody.querySelectorAll('input[name="selectedAddress"]').forEach(radio => {
            radio.addEventListener('change', function() {
                document.getElementById('selectAddressBtn').style.display = 'inline-block';
                selectedAddressData = JSON.parse(this.dataset.result);
            });
        });
    }
    
    resultArea.style.display = 'block';
}

function onSelectAddress() {
    if (!selectedAddressData) return;
    
    // 選択されたデータをフォームに設定
    document.getElementById('obligorName').value = selectedAddressData.name;
    document.getElementById('facilityName').value = `${selectedAddressData.name}の施設`; // モック
    document.getElementById('managerAddress').value = selectedAddressData.address;
    document.getElementById('managerName').value = selectedAddressData.name;
    
    // モーダルを閉じる
    bootstrap.Modal.getInstance(document.getElementById('addressSearchModal')).hide();
    
    showAlert('info', '<i class="bi bi-info-circle me-2"></i>宛名情報を設定しました。');
}

// -----------------------------------------------------------------------
// リクエストボディ組み立て
// -----------------------------------------------------------------------
function buildRequestBody() {
    return {
        registrationDate: getFieldValue('registrationDate'),
        obligorName: getFieldValue('obligorName'),
        facilityName: getFieldValue('facilityName'),
        managerAddress: getFieldValue('managerAddress'),
        managerName: getFieldValue('managerName'),
        managerNameKana: getFieldValue('managerNameKana'),
        managerPhone: getFieldValue('managerPhone'),
        exemptionFlag: document.getElementById('exemptionFlag').checked,
        exemptionReason: getFieldValue('exemptionReason')
    };
}

// -----------------------------------------------------------------------
// フロントバリデーション
// -----------------------------------------------------------------------
function validateForm() {
    let valid = true;

    // 必須項目チェック
    const required = [
        'registrationDate', 'obligorName', 'facilityName', 
        'managerAddress', 'managerName', 'managerNameKana', 'managerPhone'
    ];

    required.forEach(id => {
        const el = document.getElementById(id);
        if (!el.value.trim()) {
            setFieldError(el, '必須項目です');
            valid = false;
        }
    });
    
    // 選任免除チェック時の理由必須チェック
    const exemptionFlag = document.getElementById('exemptionFlag');
    const exemptionReason = document.getElementById('exemptionReason');
    
    if (exemptionFlag.checked && !exemptionReason.value.trim()) {
        setFieldError(exemptionReason, '選任免除理由は必須です');
        valid = false;
    }

    return valid;
}

// -----------------------------------------------------------------------
// APIエラーハンドリング
// -----------------------------------------------------------------------
function handleApiError(json) {
    if (json.fieldErrors && json.fieldErrors.length > 0) {
        const list = json.fieldErrors.map(e => `<li>${e}</li>`).join('');
        showAlert('danger', `<ul class="mb-0">${list}</ul>`);
    } else {
        showAlert('danger',
            `<i class="bi bi-exclamation-triangle me-2"></i>${json.message || 'エラーが発生しました。'}`
        );
    }
}

// -----------------------------------------------------------------------
// ユーティリティ関数
// -----------------------------------------------------------------------
function buildHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
    return headers;
}

function showAlert(type, html) {
    document.getElementById('alertArea').innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${html}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>`;
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function setFieldError(el, message) {
    el.classList.add('is-invalid');
    const fb = el.parentElement.querySelector('.invalid-feedback');
    if (fb) fb.textContent = message;
}

function clearAllErrors() {
    document.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
}

function getFieldValue(id) {
    const el = document.getElementById(id);
    return el ? el.value.trim() : '';
}

function setFieldValue(id, value) {
    const el = document.getElementById(id);
    if (el && value !== undefined && value !== null) {
        el.value = value;
    }
}