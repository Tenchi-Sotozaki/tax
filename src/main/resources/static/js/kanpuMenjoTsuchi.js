/**
 * 徴収不能額の還付又は納入義務の免除決定通知書 JavaScript
 */

// PDF生成
// プレビュー
// 印刷
// フォームバリデーション
function validateForm() {
    const fields = [
        { id: 'hakkoYmd',       msg: '発行年月日を入力してください。' },
        { id: 'juriYmd',        msg: '申請受理年月日を入力してください。' },
        { id: 'shinseiYm',      msg: '対象年月を入力してください。' },
        { id: 'zeigaku',        msg: '申請した税額を入力してください。' },
        { id: 'kanpuMenjoGaku', msg: '還付又は納入義務の免除を決定した額を入力してください。' }
    ];
    let hasError = false;
    let firstError = null;

    fields.forEach(({ id, msg }) => {
        const el = document.getElementById(id);
        const errEl = document.getElementById(id + 'Error');
        el.classList.remove('is-invalid');
        errEl.textContent = '';
        if (!el.value.toString().trim()) {
            el.classList.add('is-invalid');
            errEl.textContent = msg;
            if (!firstError) firstError = el;
            hasError = true;
        }
    });

    if (hasError) {
        firstError.focus();
        return false;
    }
    return true;
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