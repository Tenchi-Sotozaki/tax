document.addEventListener('DOMContentLoaded', function() {
    const radios = document.querySelectorAll('input[name="month"]');
    const btnNew = document.querySelector('button[type="submit"]');
    const btnView = document.getElementById('btnViewDeclaration');

    // 1. 初期状態：選択されるまでボタンを無効化
    if (btnNew) btnNew.disabled = true;
    if (btnView) btnView.disabled = true;

    // 2. ラジオボタン変更時の活性制御
    radios.forEach(radio => {
        radio.addEventListener('change', function() {
            const isShinkokuZumi = this.getAttribute('data-status') === 'true';
            if (btnNew) btnNew.disabled = isShinkokuZumi;
            if (btnView) btnView.disabled = !isShinkokuZumi;
        });
    });

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

	if (btnView) {
	        btnView.addEventListener('click', function() {
	            const selected = document.querySelector('input[name="month"]:checked');
	            
	            const nendo = selected.getAttribute('data-nendo');
	            const kibetsu = selected.getAttribute('data-kibetsu');

	            if (!nendo || !kibetsu || kibetsu === "null") {
	                alert("このデータには期別情報が含まれていません。");
	                return;
	            }

	            // 💡 HTML側で作った generateViewUrl 関数をここで呼ぶ！
	            window.location.href = generateViewUrl(nendo, kibetsu);
	        });
	    }
});

// HTMLから移動したgenerateViewUrl関数
function generateViewUrl(nendo, kibetsu) {
    var baseUrl = window.declarationViewBaseUrl || '/declaration/view/';
    var shiteiNo = window.fukaDaichoFormShiteiNo || '';
    return baseUrl + shiteiNo + '/' + nendo + '/' + kibetsu;
}
