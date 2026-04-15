'use strict';

// -----------------------------------------------------------------------
// 定数
// -----------------------------------------------------------------------
const API_BASE = '/accommodation-tax/api/declarations';

// 税区分マスタ（サーバーと同期。将来的にAPIから取得する場合はinitTaxCategories()を修正）
const TAX_CATEGORIES = [
    { code: '01', name: '税区分①（〜7,000円未満）',         taxAmount: 200 },
    { code: '02', name: '税区分②（7,000円〜15,000円未満）', taxAmount: 500 },
    { code: '03', name: '税区分③（15,000円以上）',          taxAmount: 1000 },
];

// -----------------------------------------------------------------------
// 状態
// -----------------------------------------------------------------------
let editingDeclarationId = null; // 編集時にセット
const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

// -----------------------------------------------------------------------
// 初期化
// -----------------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    renderDetailRows();
    bindEvents();

    // URLパラメータに id があれば編集モードで読み込む
    const params = new URLSearchParams(location.search);
    const id = params.get('id');
    if (id) {
        editingDeclarationId = id;
        loadDeclaration(id);
        document.getElementById('submitBtn').innerHTML = '<i class="bi bi-save me-1"></i>更新';
        const label = document.getElementById('modeLabel');
        if (label) label.innerHTML = '<i class="bi bi-pencil me-1"></i>編集中 ID: ' + id;
    }
});

// -----------------------------------------------------------------------
// 税区分明細行の描画
// -----------------------------------------------------------------------
function renderDetailRows() {
    const tbody = document.getElementById('detailTableBody');
    tbody.innerHTML = '';

    TAX_CATEGORIES.forEach(cat => {
        const tr = document.createElement('tr');
        tr.dataset.code = cat.code;
        tr.innerHTML = `
            <td>${cat.name}</td>
            <td class="text-end pe-3">${cat.taxAmount.toLocaleString()} 円</td>
            <td>
                <input type="number" class="form-control form-control-sm taxable-nights"
                       data-code="${cat.code}" data-tax="${cat.taxAmount}"
                       min="0" value="0" required>
                <div class="invalid-feedback"></div>
            </td>
            <td class="text-end pe-3 subtotal" id="subtotal-${cat.code}">0 円</td>
        `;
        tbody.appendChild(tr);
    });

    // 宿泊数変更時に小計・合計を再計算
    tbody.querySelectorAll('.taxable-nights').forEach(input => {
        input.addEventListener('input', recalculate);
    });
}

// -----------------------------------------------------------------------
// 小計・合計の再計算
// -----------------------------------------------------------------------
function recalculate() {
    let total = 0;
    document.querySelectorAll('.taxable-nights').forEach(input => {
        const nights = parseInt(input.value, 10) || 0;
        const tax    = parseInt(input.dataset.tax, 10);
        const sub    = nights * tax;
        total += sub;
        document.getElementById(`subtotal-${input.dataset.code}`).textContent =
            sub.toLocaleString() + ' 円';
    });
    document.getElementById('totalPaymentAmount').textContent =
        total.toLocaleString() + ' 円';
}

// -----------------------------------------------------------------------
// イベントバインド
// -----------------------------------------------------------------------
function bindEvents() {
    document.getElementById('declarationForm').addEventListener('submit', onSubmit);
    document.getElementById('clearBtn').addEventListener('click', onClear);
    document.getElementById('confirmSubmitBtn').addEventListener('click', onConfirmSubmit);
}

// -----------------------------------------------------------------------
// フォーム送信（確認モーダル表示）
// -----------------------------------------------------------------------
function onSubmit(e) {
    e.preventDefault();
    clearAllErrors();

    if (!validateForm()) return;

    // 確認モーダルに内容を表示
    const data = buildRequestBody();
    document.getElementById('confirmContent').innerHTML = `
        <dt class="col-sm-5">特別徴収義務者ID</dt><dd class="col-sm-7">${data.collectorId}</dd>
        <dt class="col-sm-5">宿泊施設ID</dt><dd class="col-sm-7">${data.facilityId}</dd>
        <dt class="col-sm-5">納入年月</dt><dd class="col-sm-7">${data.paymentYearMonth}</dd>
        <dt class="col-sm-5">総宿泊数</dt><dd class="col-sm-7">${data.totalNights} 泊</dd>
        <dt class="col-sm-5">課税対象外宿泊数</dt><dd class="col-sm-7">${data.exemptNights} 泊</dd>
        <dt class="col-sm-5">納入合計金額</dt>
        <dd class="col-sm-7 fw-bold text-primary">
            ${document.getElementById('totalPaymentAmount').textContent}
        </dd>
    `;
    bootstrap.Modal.getOrCreateInstance(document.getElementById('confirmModal')).show();
}

// -----------------------------------------------------------------------
// 確認モーダルで「確定」押下 → API呼び出し
// -----------------------------------------------------------------------
async function onConfirmSubmit() {
    bootstrap.Modal.getInstance(document.getElementById('confirmModal')).hide();

    const btn = document.getElementById('submitBtn');
    btn.classList.add('btn-loading');
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>送信中...';

    try {
        const body = buildRequestBody();
        const isEdit = !!editingDeclarationId;
        const url    = isEdit ? `${API_BASE}/${editingDeclarationId}` : API_BASE;
        const method = isEdit ? 'PUT' : 'POST';

        const res = await fetch(url, {
            method,
            headers: buildHeaders(),
            body: JSON.stringify(body),
        });

        const json = await res.json();

        if (!res.ok) {
            handleApiError(json);
            return;
        }

        showAlert('success',
            `<i class="bi bi-check-circle me-2"></i>` +
            `申告ID: ${json.declarationId} を${isEdit ? '更新' : '登録'}しました。`
        );
        if (!isEdit) {
            editingDeclarationId = json.declarationId;
            document.getElementById('submitBtn').textContent = '更新';
            history.replaceState(null, '', `?id=${json.declarationId}`);
        }

    } catch (err) {
        showAlert('danger', '<i class="bi bi-exclamation-triangle me-2"></i>通信エラーが発生しました。');
        console.error(err);
    } finally {
        btn.classList.remove('btn-loading');
        btn.innerHTML = editingDeclarationId
            ? '<i class="bi bi-save me-1"></i>更新'
            : '<i class="bi bi-save me-1"></i>登録';
    }
}

// -----------------------------------------------------------------------
// 既存申告の読み込み（編集モード）
// -----------------------------------------------------------------------
async function loadDeclaration(id) {
    try {
        const res = await fetch(`${API_BASE}/${id}`, { headers: buildHeaders() });
        if (!res.ok) {
            showAlert('danger', `申告ID: ${id} の読み込みに失敗しました。`);
            return;
        }
        const data = await res.json();
        fillForm(data);
    } catch (err) {
        showAlert('danger', '通信エラーが発生しました。');
        console.error(err);
    }
}

function fillForm(data) {
    document.getElementById('collectorId').value      = data.collectorId;
    document.getElementById('facilityId').value       = data.facilityId;
    document.getElementById('paymentYearMonth').value = data.paymentYearMonth;
    document.getElementById('totalNights').value      = data.totalNights;
    document.getElementById('exemptNights').value     = data.exemptNights;

    data.details.forEach(d => {
        const input = document.querySelector(`.taxable-nights[data-code="${d.taxCategoryCode}"]`);
        if (input) input.value = d.taxableNights;
    });
    recalculate();
}

// -----------------------------------------------------------------------
// クリア
// -----------------------------------------------------------------------
function onClear() {
    document.getElementById('declarationForm').reset();
    document.querySelectorAll('.taxable-nights').forEach(i => { i.value = 0; });
    recalculate();
    clearAllErrors();
    document.getElementById('alertArea').innerHTML = '';
}

// -----------------------------------------------------------------------
// リクエストボディ組み立て
// -----------------------------------------------------------------------
function buildRequestBody() {
    const details = [];
    document.querySelectorAll('.taxable-nights').forEach(input => {
        details.push({
            taxCategoryCode: input.dataset.code,
            taxableNights:   parseInt(input.value, 10) || 0,
        });
    });

    return {
        collectorId:      document.getElementById('collectorId').value.trim(),
        facilityId:       document.getElementById('facilityId').value.trim(),
        paymentYearMonth: document.getElementById('paymentYearMonth').value.trim(),
        totalNights:      parseInt(document.getElementById('totalNights').value, 10) || 0,
        exemptNights:     parseInt(document.getElementById('exemptNights').value, 10) || 0,
        details,
    };
}

// -----------------------------------------------------------------------
// フロントバリデーション
// -----------------------------------------------------------------------
function validateForm() {
    let valid = true;

    const required = ['collectorId', 'facilityId', 'paymentYearMonth', 'totalNights', 'exemptNights'];
    required.forEach(id => {
        const el = document.getElementById(id);
        if (!el.value.trim()) {
            setFieldError(el, '必須項目です');
            valid = false;
        }
    });

    const ym = document.getElementById('paymentYearMonth').value.trim();
    if (ym && !/^\d{6}$/.test(ym)) {
        setFieldError(document.getElementById('paymentYearMonth'), 'YYYYMM形式で入力してください');
        valid = false;
    }

    document.querySelectorAll('.taxable-nights').forEach(input => {
        if (parseInt(input.value, 10) < 0) {
            setFieldError(input, '0以上の値を入力してください');
            valid = false;
        }
    });

    // 総宿泊数の整合チェック
    const total   = parseInt(document.getElementById('totalNights').value, 10) || 0;
    const exempt  = parseInt(document.getElementById('exemptNights').value, 10) || 0;
    const taxable = Array.from(document.querySelectorAll('.taxable-nights'))
        .reduce((sum, i) => sum + (parseInt(i.value, 10) || 0), 0);

    if (valid && total !== taxable + exempt) {
        showAlert('warning',
            `<i class="bi bi-exclamation-triangle me-2"></i>` +
            `総宿泊数（${total}）が課税対象宿泊数の合計（${taxable}）＋課税対象外宿泊数（${exempt}）と一致しません。`
        );
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
// ユーティリティ
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
    const fb = el.closest('.col-md-4, td')?.querySelector('.invalid-feedback');
    if (fb) fb.textContent = message;
}

function clearAllErrors() {
    document.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    document.querySelectorAll('.invalid-feedback').forEach(el => { el.textContent = ''; });
}
