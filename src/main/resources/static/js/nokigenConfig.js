function applyDayColor(el) {
    if (!el.value) { el.style.color = ''; return; }
    const day = new Date(el.value).getDay();
    el.style.color = day === 6 ? 'blue' : day === 0 ? 'red' : '';
}

function changeNendo(select) {
    window.location.href = select.dataset.baseUrl + select.value;
}

function getShiftMode() {
    const checked = document.querySelector('input[name="shiftWeekend"]:checked');
    return checked ? checked.value : 'none';
}

function copyPrevYear(btn) {
    const nendo = document.getElementById('nendo').value;
    if (!nendo) { alert('年度を入力してください。'); return; }
    const existsUrl = btn.dataset.existsUrl;
    const shiftMode = getShiftMode();
    fetch(existsUrl + nendo)
        .then(res => res.json())
        .then(data => {
            if (data.exists) {
                alert('既に登録済みです。編集画面から修正してください。');
                return;
            }
            // サーバー側に shiftMode をクエリパラメータとして渡す
            const url = `${btn.dataset.url}${nendo}?shiftMode=${shiftMode}`;
            return fetch(url);
        })
        .then(res => {
            if (!res) return;
            if (!res.ok) { alert('前年度のデータが見つかりません。'); return; }
            return res.json();
        })
        .then(data => {
            if (!data) return;
            ['1st', '2nd', '3rd', '4th', '5th', '6th', '7th', '8th', '9th', '10th', '11th', '12th'].forEach(k => {
                const el = document.getElementById('nokigen' + k);
                if (el) {
                    // サーバー側で休業日考慮済みの日付が返却される想定
                    el.value = data['nokigen' + k] || '';
                    applyDayColor(el);
                }
            });
        });
}

/**
 * フォームバリデーション
 * @author Atsumu Kuboichi
 */
function validateForm() {
    let errors = [];

    // 年度チェック
    const nendoInput = document.getElementById('nendo');
    if (!nendoInput || !nendoInput.value.trim()) {
        errors.push('年度は必須入力です。');
    } else {
        const nendoVal = parseInt(nendoInput.value, 10);
        if (isNaN(nendoVal) || nendoVal < 2000 || nendoVal > 2100) {
            errors.push('年度は有効な4桁の数値を入力してください。');
        }
    }

    // 1期〜12期の未入力チェックおよび日付整合性チェック
    const kiKeys = ['1st', '2nd', '3rd', '4th', '5th', '6th', '7th', '8th', '9th', '10th', '11th', '12th'];
    let previousDate = null;

    kiKeys.forEach((k, index) => {
        const el = document.getElementById('nokigen' + k);
        if (el) {
            const val = el.value.trim();
            const kiNum = index + 1;

            if (!val) {
                errors.push(`${kiNum}期の日付が未入力です。`);
            } else {
                const currentDate = new Date(val);
                if (isNaN(currentDate.getTime())) {
                    errors.push(`${kiNum}期の日付形式が不正です。`);
                } else {
//                    // 期ごとの時系列チェック（前の期より後の日付になっているか）
//                    if (previousDate && currentDate <= previousDate) {
//                        errors.push(`${kiNum}期の日付は前回の期より後の日付を設定してください。`);
//                    }
                    previousDate = currentDate;
                }
            }
        }
    });

    // エラーメッセージの表示制御
    showClientErrors(errors);

    return errors.length === 0;
}

/**
 * エラーメッセージの反映
 * @author Atsumu Kuboichi
 */
function showClientErrors(errors) {

    // 既存のエラー表示エリアを取得または作成
    let errorAlert = document.getElementById('clientErrorAlert');

    if (errors.length > 0) {
        if (!errorAlert) {
            errorAlert = document.createElement('div');
            errorAlert.id = 'clientErrorAlert';
            errorAlert.className = 'alert alert-danger alert-dismissible fade show';
            errorAlert.setAttribute('role', 'alert');

            // フォームの先頭（カードの直上など）に挿入
            const card = document.querySelector('.card.shadow-sm');
            if (card) {
                card.parentNode.insertBefore(errorAlert, card);
            }
        }

        let html = `<i class="bi bi-exclamation-triangle-fill me-2"></i> 入力内容に <span>${errors.length}</span> 件のエラーがあります。`;
        html += '<ul class="mb-0 mt-1">';
        errors.forEach(err => {
            html += `<li class="small">${err}</li>`;
        });
        html += '</ul>';
        html += '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>';

        errorAlert.innerHTML = html;
        window.scrollTo({ top: 0, behavior: 'smooth' });
    } else {
        if (errorAlert) {
            errorAlert.remove();
        }
    }
}

document.addEventListener('DOMContentLoaded', function() {
    // 全日付入力に色適用 + changeイベント
    document.querySelectorAll('input[type="date"]').forEach(el => {
        applyDayColor(el);
        el.addEventListener('change', function() { applyDayColor(this); });
    });

    // チェックボックス排他制御（ラジオボタン風）
    document.querySelectorAll('input[name="shiftWeekend"]').forEach(cb => {
        cb.addEventListener('change', function() {
            if (this.checked) {
                document.querySelectorAll('input[name="shiftWeekend"]').forEach(other => {
                    if (other !== this) other.checked = false;
                });
            }
        });
    });

    // フォームの取得をIDで確実に指定
    const form = document.getElementById('nokigenForm');
    const modeInput = document.querySelector('input[name="mode"]');

    if (form) {
        form.addEventListener('submit', function(e) {
            // 1. クライアントサイドバリデーション（未入力・日付整合性チェック）
            if (!validateForm()) {
                e.preventDefault();
                return;
            }

            // 2. 登録モード時のみ送信前の重複チェック
            if (modeInput && modeInput.value === 'register') {
                const existsBtn = document.querySelector('[data-exists-url]');
                if (existsBtn) {
                    const existsUrl = existsBtn.dataset.existsUrl;
                    const nendo = document.getElementById('nendo').value;
                    
                    // 非同期通信を行うため、必ずここで一度デフォルト送信を止める
                    e.preventDefault();
                    
                    fetch(existsUrl + nendo)
                        .then(res => res.json())
                        .then(data => {
                            if (data.exists) {
                                alert('既に登録済みです。編集画面から修正してください。');
                            } else {
                                // 重複がない場合はフォームを再度サブミット
                                form.submit();
                            }
                        })
                        .catch(err => {
                            console.error(err);
                            alert('重複チェック中にエラーが発生しました。');
                        });
                }
            }
        });
    }

    // 編集モードでなければ以降の処理を行わない
    const contentContainer = document.querySelector('[data-is-edit]');
    const isEdit = contentContainer ? contentContainer.getAttribute('data-is-edit') === 'true' : false;
    if (!isEdit) return;

    // 編集変更チェック
    function checkValue(input) {
        if (input.type === 'checkbox' || input.type === 'radio') return;
        if (!input.hasAttribute('data-initial-value')) return;

        const initialValue = input.getAttribute('data-initial-value') || '';
        let initialStr = (initialValue === null || initialValue === 'null') ? '' : String(initialValue).trim();
        let currentStr = String(input.value).trim();

        if (input.type === 'date' && initialStr.length === 8 && !initialStr.includes('-')) {
            initialStr = initialStr.substring(0, 4) + '-' + initialStr.substring(4, 6) + '-' + initialStr.substring(6, 8);
        } else if (input.type === 'month' && initialStr.length === 6 && !initialStr.includes('-')) {
            initialStr = initialStr.substring(0, 4) + '-' + initialStr.substring(4, 6);
        }

        const isChanged = (currentStr !== initialStr);
        if (isChanged) {
            input.style.border = '3px solid #ffeb3b';
        } else {
            input.style.border = '';
        }
    }

    // 画面表示時に最初から値が変わっているものを検知、および各イベントへの登録
    const inputs = document.querySelectorAll('.form-control, .form-select');
    inputs.forEach(input => {
        checkValue(input);
        input.addEventListener('input', () => checkValue(input));
        input.addEventListener('change', () => checkValue(input));
        input.addEventListener('blur', () => checkValue(input));
    });
});