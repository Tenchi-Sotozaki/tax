document.addEventListener('DOMContentLoaded', function() {
    const checkboxes = document.querySelectorAll('input[name="month"]');
    const btnNew = document.querySelector('button[type="submit"]');
    const btnView = document.getElementById('btnViewDeclaration');

    // 1. 初期状態：未選択のためボタンを無効化
    if (btnNew) btnNew.disabled = true;
    if (btnView) btnView.disabled = true;

    // 2. チェックボックス排他制御＋ボタン活性制御
    checkboxes.forEach(cb => {
        cb.addEventListener('change', function() {
            if (this.checked) {
                // 他のチェックを外す（排他制御）
                checkboxes.forEach(other => {
                    if (other !== this) other.checked = false;
                });
            }
            updateButtons();
        });
    });

    function updateButtons() {
        const selected = document.querySelector('input[name="month"]:checked');
        if (!selected) {
            if (btnNew) btnNew.disabled = true;
            if (btnView) btnView.disabled = true;
            return;
        }
        const isShinkokuZumi = selected.getAttribute('data-status') === 'true';
        if (btnNew) btnNew.disabled = isShinkokuZumi;
        if (btnView) btnView.disabled = !isShinkokuZumi;
    }

    // 3. 新規登録ボタンのsubmitチェック
    const form = document.querySelector('form[action*="/declaration/register"]');
    if (form) {
        form.addEventListener('submit', function(e) {
            const selected = document.querySelector('input[name="month"]:checked');
            if (!selected) {
                alert("登録対象の年月を選択してください。");
                e.preventDefault();
                return;
            }
            if (selected.getAttribute('data-status') === 'true') {
                alert("既に申告済みのデータです。「照会」ボタンを使用してください。");
                e.preventDefault();
            }
        });
    }

    // 4. 照会ボタン
    if (btnView) {
        btnView.addEventListener('click', function() {
            const selected = document.querySelector('input[name="month"]:checked');
            if (!selected) {
                alert("照会対象の年月を選択してください。");
                return;
            }
            const nendo = selected.getAttribute('data-nendo');
            const kibetsu = selected.getAttribute('data-kibetsu');
            if (!nendo || !kibetsu || kibetsu === "null") {
                alert("このデータには期別情報が含まれていません。");
                return;
            }
            window.location.href = generateViewUrl(nendo, kibetsu);
        });
    }
});

function generateViewUrl(nendo, kibetsu) {
    var baseUrl = window.declarationViewBaseUrl || '/declaration/view/';
    var shiteiNo = window.fukaDaichoFormShiteiNo || '';
    return baseUrl + shiteiNo + '/' + nendo + '/' + kibetsu;
}
