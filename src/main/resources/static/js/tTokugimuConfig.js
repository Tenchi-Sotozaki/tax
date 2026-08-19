'use strict';

// -----------------------------------------------------------------------
// 初期化
// -----------------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    bindEvents();
    initSmoothScroll();
    initKyodoSection();
    initBusinessStatusSection();
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

    // 自動コピーチェックボックス
    initCopyCheckboxes();
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
    // 特別徴収義務者情報エリアに自動入力
    const set = (id, val) => { const el = document.getElementById(id); if (el) el.value = val ?? ''; };
    set('atenaNo', d.addressNumber);
    set('tokugimuAddress', d.address);
    set('name', d.name);
    set('nameKana', d.nameKana);
    set('tokugimuPhone', d.phone);
    set('personalNumber', d.kojinNo);
    set('corporateNumber', d.hojinNo);
    set('tokugimuAddressNo', d.yubinNo);

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
    const reason = document.getElementById('suspensionOrAbolitionReason');
    // 全て非活性にしてクリア
    [suspendStart, suspendEnd, resumeClose, reason].forEach(el => {
        if (el) { el.disabled = true; el.value = ''; }
    });
    if (undecided) { undecided.disabled = true; undecided.checked = false; }

    if (value === '休止') {
        [suspendStart, suspendEnd, undecided, reason].forEach(el => {
            if (el) el.disabled = false;
        });
    } else if (value === '再開') {
        if (resumeClose) resumeClose.disabled = false;
    } else if (value === '廃止') {
        [resumeClose, reason].forEach(el => {
            if (el) el.disabled = false;
        });
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

// -----------------------------------------------------------------------
// 自動コピー機能
// -----------------------------------------------------------------------
function initCopyCheckboxes() {
    // 施設情報
    const copyToFacility = document.getElementById('copyToFacility');
    if (copyToFacility) {
        copyToFacility.addEventListener('change', () => {
            copyTokugimuInfoToFacility(copyToFacility.checked);
        });
    }

    // 営業許可情報
    const copyToLicense = document.getElementById('copyToLicense');
    if (copyToLicense) {
        copyToLicense.addEventListener('change', () => {
            copyTokugimuInfoToLicense(copyToLicense.checked);
        });
    }

    // 施設所有者情報
    const copyToOwner = document.getElementById('copyToOwner');
    if (copyToOwner) {
        copyToOwner.addEventListener('change', () => {
            copyTokugimuInfoToOwner(copyToOwner.checked);
        });
    }

    // 書類送付先情報
    const copyToMail = document.getElementById('copyToMail');
    if (copyToMail) {
        copyToMail.addEventListener('change', () => {
            copyTokugimuInfoToMail(copyToMail.checked);
        });
    }

    // 共同事業者情報の表示切替
    const kyodoCheck = document.getElementById('kyodoCheck');
    if (kyodoCheck) {
        kyodoCheck.addEventListener('change', () => {
            const kyodoBody = document.getElementById('kyodoBody');
            kyodoBody.style.display = kyodoCheck.checked ? '' : 'none';
            if (!kyodoCheck.checked) {
                kyodoBody.querySelectorAll('input, textarea, select').forEach(el => el.value = '');
            }
        });
    }

    // 営業状況情報の表示切替
    const businessStatusCheck = document.getElementById('businessStatusCheck');
    if (businessStatusCheck) {
        businessStatusCheck.addEventListener('change', () => {
            const body = document.getElementById('businessStatusBody');
            body.style.display = businessStatusCheck.checked ? '' : 'none';
            if (!businessStatusCheck.checked) {
                body.querySelectorAll('input:not([type="checkbox"]):not([type="radio"]), textarea, select').forEach(el => el.value = '');
                body.querySelectorAll('input[type="radio"]').forEach(el => el.checked = false);
                body.querySelectorAll('input[type="checkbox"]').forEach(el => el.checked = false);
            }
        });
    }

    // 共同事業者追加ボタン
    const kyodoAddBtn = document.getElementById('kyodoAddBtn');
    if (kyodoAddBtn) {
        kyodoAddBtn.addEventListener('click', addKyodoRow);
    }

    // 共同事業者削除ボタン（初期表示分）
    document.querySelectorAll('.kyodo-remove-btn').forEach(btn => {
        btn.addEventListener('click', () => removeKyodoRow(btn));
    });
}

function copyTokugimuInfoToFacility(enabled) {
    if (enabled) {
        setValue('facilityAddressNo', document.getElementById('tokugimuAddressNo')?.value || '');
        setValue('facilityAddress', document.getElementById('tokugimuAddress')?.value || '');
        setValue('facilityName', document.getElementById('name')?.value || '');
        setValue('facilityNameKana', document.getElementById('nameKana')?.value || '');
        setValue('facilityPhone', document.getElementById('tokugimuPhone')?.value || '');
    } else {
        setValue('facilityAddressNo', '');
        setValue('facilityAddress', '');
        setValue('facilityName', '');
        setValue('facilityNameKana', '');
        setValue('facilityPhone', '');
    }
}

function copyTokugimuInfoToLicense(enabled) {
    if (enabled) {
        setValue('licenseAddressNo', document.getElementById('tokugimuAddressNo')?.value || '');
        setValue('licenseAddress', document.getElementById('tokugimuAddress')?.value || '');
        setValue('licenseName', document.getElementById('name')?.value || '');
        setValue('licenseNameKana', document.getElementById('nameKana')?.value || '');
        setValue('licensePhone', document.getElementById('tokugimuPhone')?.value || '');
    } else {
        setValue('licenseAddressNo', '');
        setValue('licenseAddress', '');
        setValue('licenseName', '');
        setValue('licenseNameKana', '');
        setValue('licensePhone', '');
    }
}

function copyTokugimuInfoToOwner(enabled) {
    if (enabled) {
        setValue('ownerAddressNo', document.getElementById('tokugimuAddressNo')?.value || '');
        setValue('ownerAddress', document.getElementById('tokugimuAddress')?.value || '');
        setValue('ownerName', document.getElementById('name')?.value || '');
        setValue('ownerNameKana', document.getElementById('nameKana')?.value || '');
        setValue('ownerPhone', document.getElementById('tokugimuPhone')?.value || '');
    } else {
        setValue('ownerAddressNo', '');
        setValue('ownerAddress', '');
        setValue('ownerName', '');
        setValue('ownerNameKana', '');
        setValue('ownerPhone', '');
    }
}

function copyTokugimuInfoToMail(enabled) {
    if (enabled) {
        setValue('mailAddressNo', document.getElementById('tokugimuAddressNo')?.value || '');
        setValue('mailAddress', document.getElementById('tokugimuAddress')?.value || '');
        setValue('mailName', document.getElementById('name')?.value || '');
        setValue('mailNameKana', document.getElementById('nameKana')?.value || '');
        setValue('mailPhone', document.getElementById('tokugimuPhone')?.value || '');
    } else {
        setValue('mailAddressNo', '');
        setValue('mailAddress', '');
        setValue('mailName', '');
        setValue('mailNameKana', '');
        setValue('mailPhone', '');
    }
}

function setValue(id, value) {
    const element = document.getElementById(id);
    if (element && !element.readOnly) {
        element.value = value;
    }
}

// -----------------------------------------------------------------------
// 共同事業者行追加・削除
// -----------------------------------------------------------------------
function addKyodoRow() {
    const rows = document.getElementById('kyodoRows');
    const idx = rows.querySelectorAll('.kyodo-row').length;
    const div = document.createElement('div');
    div.className = 'kyodo-row border rounded p-3 mb-3';
    div.innerHTML = `
        <div class="d-flex justify-content-between align-items-center mb-2">
            <span class="fw-medium">共同事業者 ${idx + 1}</span>
            <button type="button" class="btn btn-sm btn-outline-danger kyodo-remove-btn"><i class="bi bi-trash"></i></button>
        </div>
        <div class="row g-3">
            <div class="col-md-2">
                <label class="form-label fw-medium">郵便番号</label>
                <input type="text" class="form-control" name="kyodoList[${idx}].kyodoAddressNo">
            </div>
            <div class="col-md-5">
                <label class="form-label fw-medium">住所</label>
                <input type="text" class="form-control" name="kyodoList[${idx}].kyodoAddress">
            </div>
            <div class="col-md-2">
                <label class="form-label fw-medium">氏名 <span class="text-danger">*</span></label>
                <input type="text" class="form-control" name="kyodoList[${idx}].kyodoName">
            </div>
            <div class="col-md-2">
                <label class="form-label fw-medium">氏名(ふりがな) <span class="text-danger">*</span></label>
                <input type="text" class="form-control" name="kyodoList[${idx}].kyodoNameKana" placeholder="ひらがなで入力">
            </div>
            <div class="col-md-2">
                <label class="form-label fw-medium">電話番号</label>
                <input type="tel" class="form-control" name="kyodoList[${idx}].kyodoPhone" placeholder="例）03-1234-5678">
            </div>
        </div>`;
    div.querySelector('.kyodo-remove-btn').addEventListener('click', () => removeKyodoRow(div.querySelector('.kyodo-remove-btn')));
    rows.appendChild(div);
    renumberKyodoRows();
}

function removeKyodoRow(btn) {
    btn.closest('.kyodo-row').remove();
    renumberKyodoRows();
}

function renumberKyodoRows() {
    document.querySelectorAll('#kyodoRows .kyodo-row').forEach((row, i) => {
        row.querySelector('span.fw-medium').textContent = `共同事業者 ${i + 1}`;
        row.querySelectorAll('input').forEach(input => {
            input.name = input.name.replace(/kyodoList\[\d+\]/, `kyodoList[${i}]`);
        });
    });
}

// -----------------------------------------------------------------------
// 共同事業者セクション初期化（編集・照会時にデータがあれば表示）
// -----------------------------------------------------------------------
function initKyodoSection() {
    const kyodoCheck = document.getElementById('kyodoCheck');
    if (kyodoCheck && kyodoCheck.checked) {
        document.getElementById('kyodoBody').style.display = '';
    }
    // 保存済みデータがない場合は初期行を追加
    const rows = document.getElementById('kyodoRows');
    if (rows && rows.querySelectorAll('.kyodo-row').length === 0) {
        addKyodoRow();
    }
}

// -----------------------------------------------------------------------
// 営業状況セクション初期化（編集・照会時にデータがあれば表示）
// -----------------------------------------------------------------------
function initBusinessStatusSection() {
    const check = document.getElementById('businessStatusCheck');
    if (check && check.checked) {
        document.getElementById('businessStatusBody').style.display = '';
    }
    // 初期表示時に申告区分の状態に応じて入力項目を制御
    applyDeclarationState();
}

function applyDeclarationState() {
    const checked = document.querySelector('input[name="declarationCategory"]:checked');
    const suspendStart = document.getElementById('suspensionStartDate');
    const suspendEnd = document.getElementById('suspensionEndDate');
    const undecided = document.getElementById('suspensionEndDateUndecided');
    const resumeClose = document.getElementById('resumptionOrAbolitionDate');
    const reason = document.getElementById('suspensionOrAbolitionReason');

    // ラジオ未選択時は全て非活性かつ値をクリア
    [suspendStart, suspendEnd, resumeClose, reason].forEach(el => {
        if (el) { el.disabled = true; el.value = ''; }
    });
    if (undecided) { undecided.disabled = true; undecided.checked = false; }

    if (!checked) return;
    const value = checked.value;

    if (value === '休止') {
        [suspendStart, suspendEnd, undecided, reason].forEach(el => {
            if (el) el.disabled = false;
        });
    } else if (value === '再開') {
        if (resumeClose) resumeClose.disabled = false;
    } else if (value === '廃止') {
        [resumeClose, reason].forEach(el => {
            if (el) el.disabled = false;
        });
    }
}

// -----------------------------------------------------------------------
// スムーズスクロール（フロートヘッダーの高さを考慮）
// -----------------------------------------------------------------------
function initSmoothScroll() {
    const scrollContainer = document.querySelector('main.overflow-auto');
    if (!scrollContainer) return;

    document.querySelectorAll('.sticky-top a[href^="#"]').forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (!target) return;
            const header = document.querySelector('.sticky-top');
            const headerBottom = header.getBoundingClientRect().bottom;
            const containerTop = scrollContainer.getBoundingClientRect().top;
            const targetTop = target.getBoundingClientRect().top;
            const offset = targetTop - headerBottom;
            scrollContainer.scrollBy({ top: offset, behavior: 'smooth' });
        });
    });
}

// -----------------------------------------------------------------------
// 特別徴収義務者 登録・編集画面用 変更検知スクリプト
// -----------------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {

    // 追加：編集モードでなければ処理を行わない
    const contentContainer = document.querySelector('[data-is-edit]');
    const isEdit = contentContainer ? contentContainer.getAttribute('data-is-edit') === 'true' : false;
    if (!isEdit) return;

    // 対象となる入力要素を取得
    const inputs = document.querySelectorAll(
        '.form-control:not(#addressSearchModal .form-control), ' +
        '.form-select:not(#addressSearchModal .form-select), ' +
        '.form-check-input:not(#addressSearchModal .form-check-input)'
    );

    /**
     * 値が変わったかどうかを判定し、枠線を黄色にする
     */
    function checkValue(input) {

        // 属性がない場合は空文字にする
        const initialValue = input.getAttribute('data-initial-value') || '';
        let isChanged = false;

        // nullという文字列になってしまうのを防ぐ
        const initialStr = (initialValue === null || initialValue === 'null') ? '' : String(initialValue).trim();

        // チェックボックスとラジオボタンは枠線を付けない
        if (input.type != 'checkbox' && input.type != 'radio') {

            const currentStr = String(input.value).trim();
            isChanged = (currentStr !== initialStr);
        }
		
        if (isChanged) {
            input.style.border = '3px solid #ffeb3b';
        } else {
            input.style.border = '';
        }
    }

    // 手入力や選択変更に対するリアルタイムイベントを設定
    inputs.forEach(input => {
        input.addEventListener('input', () => checkValue(input));
        input.addEventListener('change', () => checkValue(input));
        input.addEventListener('blur', () => checkValue(input));
    });

    // 画面全体でクリックや変更があった時にすべての項目を一斉再チェック
    document.addEventListener('click', () => {
        inputs.forEach(input => checkValue(input));
    });
    document.addEventListener('change', () => {
        inputs.forEach(input => checkValue(input));
    });

    // 値をコピーした後に、強制的にイベントを発生させて黄色枠をトリガーする
    targetInput.value = sourceInput.value;
    targetInput.dispatchEvent(new Event('change'));
});
