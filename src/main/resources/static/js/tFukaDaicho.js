document.addEventListener('DOMContentLoaded', function() {

    // 新規登録ボタンの処理
    const registerButtons = document.querySelectorAll('.btn-register');
    registerButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            // クリックされたボタンと同じ行（tr）を取得
            const tr = this.closest('tr');
            const hiddenInput = tr ? tr.querySelector('input[name="month"]') : null;

            if (!hiddenInput) {
                alert("対象データが見つかりません。");
                e.preventDefault();
                return;
            }

            // 万が一申告済みの行で押された場合のガード
            const isShinkokuZumi = hiddenInput.getAttribute('data-status') === 'true';
            if (isShinkokuZumi) {
                alert("既に申告済みのデータです。「照会」ボタンを使用してください。");
                e.preventDefault();
                return;
            }

            // 送信不要な他行の hidden input を一時的に無効化（現在行のデータのみ送信するため）
            document.querySelectorAll('input[name="month"]').forEach(input => {
                if (input !== hiddenInput) {
                    input.disabled = true;
                }
            });
        });
    });

    // 照会ボタンの処理
    const viewButtons = document.querySelectorAll('.btn-view');
    viewButtons.forEach(button => {
        button.addEventListener('click', function() {
            // クリックされたボタンと同じ行（tr）を取得
            const tr = this.closest('tr');
            const hiddenInput = tr ? tr.querySelector('input[name="month"]') : null;

            if (!hiddenInput) {
                alert("照会対象のデータが見つかりません。");
                return;
            }

            const nendo = hiddenInput.getAttribute('data-nendo');
            const kibetsu = hiddenInput.getAttribute('data-kibetsu');

            if (!nendo || !kibetsu || kibetsu === "null") {
                alert("このデータには期別情報が含まれていません。");
                return;
            }

            // 照会画面へ遷移
            window.location.href = generateViewUrl(nendo, kibetsu);
        });
    });
});

function generateViewUrl(nendo, kibetsu) {
    var baseUrl = window.declarationViewBaseUrl || '/declaration/view';
    return baseUrl + '?nendo=' + nendo + '&kibetsu=' + kibetsu;
}
