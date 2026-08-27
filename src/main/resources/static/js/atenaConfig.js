/**
 * 宛名登録/編集画面用 変更検知スクリプト
 * 画面起動時の初期値と現在の入力値を比較し、
 * 変更があった項目の枠線を黄色に変更
 */
document.addEventListener('DOMContentLoaded', () => {

    const form = document.getElementById('atenaForm');
    if (!form || form.dataset.mode !== 'edit') return;

    const inputs = form.querySelectorAll('.form-select, .form-control');

    /**
     * 引数で渡された入力項目の値が変わったかどうかを判定し、枠線の色を変更
     * @param {HTMLElement} input - 判定対象の入力要素
     */
    function checkValue(input) {

        // サーバーから渡された画面表示時点の初期値を取得
        const initialValue = input.getAttribute('data-initial-value');
        const initialStr = (initialValue === null || initialValue === 'null') ? '' : String(initialValue).trim();
        const currentStr = String(input.value).trim();

        // 現在の値と初期値が一致しているか判定
        const isChanged = (currentStr !== initialStr);

        // 変化があれば黄色の枠を付与、戻ればスタイルをクリア
        if (isChanged) {
            input.classList.add('form-control-edited');
        } else {
            input.classList.remove('form-control-edited');
            input.style.border = '';
        }
    }

    // 取得した入力項目に対して、値の変更を監視するイベントリスナーを登録
    inputs.forEach(input => {
        input.addEventListener('input', () => checkValue(input));
        input.addEventListener('change', () => checkValue(input));
    });
});
