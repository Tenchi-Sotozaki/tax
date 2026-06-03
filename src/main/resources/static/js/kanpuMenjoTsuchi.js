/**
 * 徴収不能額の還付又は納入義務の免除決定通知書 JavaScript
 */

// PDF生成
function generatePdf() {
    if (!validateForm()) {
        return;
    }
    
    const form = document.getElementById('tsuchiForm');
    form.action = '/accommodation-tax/kanpuMenjoTsuchi/generatePdf';
    form.target = '_blank';
    form.submit();
}

// プレビュー
function preview() {
    if (!validateForm()) {
        return;
    }
    
    const form = document.getElementById('tsuchiForm');
    form.action = '/accommodation-tax/kanpuMenjoTsuchi/preview';
    form.target = '_blank';
    form.submit();
}

// 印刷
function print() {
    if (!validateForm()) {
        return;
    }
    
    const form = document.getElementById('tsuchiForm');
    form.action = '/accommodation-tax/kanpuMenjoTsuchi/print';
    form.target = '_blank';
    form.submit();
}

// フォームバリデーション
function validateForm() {
    const requiredFields = [
        'hakkoYmd',
        'juriYmd', 
        'shinseiYm',
        'zeigaku',
        'kanpuMenjoGaku'
    ];
    
    let isValid = true;
    const errorMessages = [];
    
    requiredFields.forEach(fieldId => {
        const field = document.getElementById(fieldId);
        if (!field.value.trim()) {
            isValid = false;
            field.classList.add('is-invalid');
            
            const label = document.querySelector(`label[for="${fieldId}"]`);
            if (label) {
                errorMessages.push(`${label.textContent.replace(' *', '')}は必須です。`);
            }
        } else {
            field.classList.remove('is-invalid');
        }
    });
    
    // 申請の年月フォーマットチェック
    const shinseiYm = document.getElementById('shinseiYm');
    if (shinseiYm.value.trim()) {
        // type="month"の場合のフォーマットチェックはブラウザが自動的に行う
        // YYYY-MM形式で入力されるので、特別なチェックは不要
    }
    
    if (!isValid) {
        alert('入力エラー:\n' + errorMessages.join('\n'));
    }
    
    return isValid;
}

// DOM読み込み完了後の初期化
document.addEventListener('DOMContentLoaded', function() {
    // 金額フィールドの数値入力支援
    const amountFields = ['zeigaku', 'kanpuMenjoGaku'];
    amountFields.forEach(fieldId => {
        const field = document.getElementById(fieldId);
        if (field) {
            // type="number"のフィールドはブラウザが数値のみ入力を許可する
            // マイナス値を防ぐためのバリデーション
            field.addEventListener('input', function(e) {
                if (e.target.value < 0) {
                    e.target.value = 0;
                }
            });
        }
    });
});