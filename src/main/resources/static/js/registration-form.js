'use strict';

// -----------------------------------------------------------------------
// 定数
// -----------------------------------------------------------------------
const API_BASE = '/accommodation-tax/api/collectors';

// -----------------------------------------------------------------------
// 状態
// -----------------------------------------------------------------------
let editingCollectorId = null; // 編集時にセット
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
        editingCollectorId = id;
        loadCollector(id);
        switchToEditMode();
    }
});

// -----------------------------------------------------------------------
// イベントバインド
// -----------------------------------------------------------------------
function bindEvents() {
    document.getElementById('registrationForm').addEventListener('submit', onSubmit);
    
    // 情報検索ボタン
    document.querySelector('.btn-outline-primary').addEventListener('click', onSearchInfo);
    
    // 申告区分の変更で関連フィールドの表示制御
    document.querySelectorAll('input[name="declarationType"]').forEach(radio => {
        radio.addEventListener('change', onDeclarationTypeChange);
    });
    
    // 未定チェックボックスで終了日の制御
    document.getElementById('undecidedPeriod').addEventListener('change', onUndecidedChange);
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
    const isEdit = !!editingCollectorId;
    const url = isEdit ? `${API_BASE}/${editingCollectorId}` : API_BASE;
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
            `特別徴収義務者を${isEdit ? '更新' : '登録'}しました。`
        );
        
        if (!isEdit) {
            editingCollectorId = json.collectorId;
            switchToEditMode();
            history.replaceState(null, '', `?id=${json.collectorId}`);
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
// 既存データの読み込み（編集モード）
// -----------------------------------------------------------------------
async function loadCollector(id) {
    try {
        const res = await fetch(`${API_BASE}/${id}`, { headers: buildHeaders() });
        if (!res.ok) {
            showAlert('danger', `特別徴収義務者ID: ${id} の読み込みに失敗しました。`);
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
    // 基本情報
    setFieldValue('registrationDate', data.registrationDate);
    setFieldValue('obligorAddress', data.obligorAddress);
    setFieldValue('obligorName', data.obligorName);
    setFieldValue('personalCorporateNumber', data.personalCorporateNumber);
    setFieldValue('obligorPhone', data.obligorPhone);
    
    // 宿泊施設情報
    setFieldValue('facilityAddress', data.facilityAddress);
    setFieldValue('facilityNameKana', data.facilityNameKana);
    setFieldValue('facilityPhone', data.facilityPhone);
    setFieldValue('floorArea', data.floorArea);
    setFieldValue('floors', data.floors);
    setFieldValue('roomCount', data.roomCount);
    setFieldValue('capacity', data.capacity);
    setFieldValue('businessStartDate', data.businessStartDate);
    
    // 営業許可等情報
    setFieldValue('licenseAddress', data.licenseAddress);
    setFieldValue('licenseNameKana', data.licenseNameKana);
    setFieldValue('licensePhone', data.licensePhone);
    setFieldValue('businessType', data.businessType);
    setFieldValue('licenseNumber', data.licenseNumber);
    
    // 施設所有者情報
    setFieldValue('ownerAddress', data.ownerAddress);
    setFieldValue('ownerNameKana', data.ownerNameKana);
    setFieldValue('ownerPhone', data.ownerPhone);
    
    // 書類送付先情報
    setFieldValue('mailAddress', data.mailAddress);
    setFieldValue('mailNameKana', data.mailNameKana);
    setFieldValue('mailPhone', data.mailPhone);
    
    // その他の情報
    setRadioValue('eltaxApplication', data.eltaxApplication);
    setFieldValue('taxCycle', data.taxCycle);
    setFieldValue('remarks', data.remarks);
    
    // 休止/再開/廃止情報
    setRadioValue('declarationType', data.declarationType);
    setFieldValue('suspendStartDate', data.suspendStartDate);
    setFieldValue('suspendEndDate', data.suspendEndDate);
    setFieldValue('undecidedPeriod', data.undecidedPeriod);
    setFieldValue('resumeOrCloseDate', data.resumeOrCloseDate);
    setFieldValue('suspendOrCloseReason', data.suspendOrCloseReason);
}

// -----------------------------------------------------------------------
// 編集モードに切り替え
// -----------------------------------------------------------------------
function switchToEditMode() {
    document.getElementById('registerBtn').style.display = 'none';
    document.getElementById('updateBtn').style.display = 'inline-block';
    
    const label = document.getElementById('modeLabel');
    if (label) {
        label.innerHTML = '<i class="bi bi-pencil me-1"></i>編集中';
        label.className = 'badge bg-warning-subtle text-warning border border-warning-subtle px-3 py-2 fs-6';
    }
}

// -----------------------------------------------------------------------
// 情報検索
// -----------------------------------------------------------------------
function onSearchInfo() {
    // モック実装
    alert('情報検索機能は実装中です。');
}

// -----------------------------------------------------------------------
// 申告区分変更時の制御
// -----------------------------------------------------------------------
function onDeclarationTypeChange(e) {
    const value = e.target.value;
    const suspendFields = ['suspendStartDate', 'suspendEndDate', 'undecidedPeriod'];
    const resumeCloseField = 'resumeOrCloseDate';
    
    // すべてのフィールドを一旦無効化
    suspendFields.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.disabled = true;
    });
    document.getElementById(resumeCloseField).disabled = true;
    
    // 選択された区分に応じて有効化
    if (value === 'suspend') {
        suspendFields.forEach(id => {
            const el = document.getElementById(id);
            if (el) el.disabled = false;
        });
    } else if (value === 'resume' || value === 'close') {
        document.getElementById(resumeCloseField).disabled = false;
    }
}

// -----------------------------------------------------------------------
// 未定チェックボックス変更時の制御
// -----------------------------------------------------------------------
function onUndecidedChange(e) {
    const endDateField = document.getElementById('suspendEndDate');
    if (e.target.checked) {
        endDateField.disabled = true;
        endDateField.value = '';
    } else {
        endDateField.disabled = false;
    }
}

// -----------------------------------------------------------------------
// リクエストボディ組み立て
// -----------------------------------------------------------------------
function buildRequestBody() {
    return {
        registrationDate: getFieldValue('registrationDate'),
        obligorAddress: getFieldValue('obligorAddress'),
        obligorName: getFieldValue('obligorName'),
        personalCorporateNumber: getFieldValue('personalCorporateNumber'),
        obligorPhone: getFieldValue('obligorPhone'),
        
        facilityAddress: getFieldValue('facilityAddress'),
        facilityNameKana: getFieldValue('facilityNameKana'),
        facilityPhone: getFieldValue('facilityPhone'),
        floorArea: parseFloat(getFieldValue('floorArea')) || null,
        floors: getFieldValue('floors'),
        roomCount: parseInt(getFieldValue('roomCount')) || null,
        capacity: parseInt(getFieldValue('capacity')) || null,
        businessStartDate: getFieldValue('businessStartDate'),
        
        licenseAddress: getFieldValue('licenseAddress'),
        licenseNameKana: getFieldValue('licenseNameKana'),
        licensePhone: getFieldValue('licensePhone'),
        businessType: getFieldValue('businessType'),
        licenseNumber: getFieldValue('licenseNumber'),
        
        ownerAddress: getFieldValue('ownerAddress'),
        ownerNameKana: getFieldValue('ownerNameKana'),
        ownerPhone: getFieldValue('ownerPhone'),
        
        mailAddress: getFieldValue('mailAddress'),
        mailNameKana: getFieldValue('mailNameKana'),
        mailPhone: getFieldValue('mailPhone'),
        
        eltaxApplication: getRadioValue('eltaxApplication'),
        taxCycle: getFieldValue('taxCycle'),
        remarks: getFieldValue('remarks'),
        
        declarationType: getRadioValue('declarationType'),
        suspendStartDate: getFieldValue('suspendStartDate'),
        suspendEndDate: getFieldValue('suspendEndDate'),
        undecidedPeriod: document.getElementById('undecidedPeriod').checked,
        resumeOrCloseDate: getFieldValue('resumeOrCloseDate'),
        suspendOrCloseReason: getFieldValue('suspendOrCloseReason')
    };
}

// -----------------------------------------------------------------------
// フロントバリデーション
// -----------------------------------------------------------------------
function validateForm() {
    let valid = true;

    // 必須項目チェック
    const required = [
        'registrationDate', 'obligorName', 'obligorPhone',
        'facilityAddress', 'facilityNameKana', 'facilityPhone', 'roomCount', 'capacity', 'businessStartDate',
        'businessType', 'licenseNumber',
        'mailAddress', 'mailNameKana', 'mailPhone'
    ];

    required.forEach(id => {
        const el = document.getElementById(id);
        if (!el.value.trim()) {
            setFieldError(el, '必須項目です');
            valid = false;
        }
    });

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

function getRadioValue(name) {
    const el = document.querySelector(`input[name="${name}"]:checked`);
    return el ? el.value : '';
}

function setRadioValue(name, value) {
    if (value) {
        const el = document.querySelector(`input[name="${name}"][value="${value}"]`);
        if (el) el.checked = true;
    }
}