/**
 * 宿泊税納入金額管理台帳 画面制御用スクリプト
 */
document.addEventListener('DOMContentLoaded', () => {

    // 照会ボタンのクリックイベント[cite: 1]
    const btnView = document.getElementById('btnViewDeclaration');
    if (btnView) {
        btnView.addEventListener('click', () => {
            // 選択された行のチェックボックスを取得
            const selectedRow = document.querySelector('.row-select:checked');
            
            if (!selectedRow) {
                alert('照会する納入記録を選択してください。');
                return;
            }

            // 複合主キーを特定するための情報を取得[cite: 3, 4]
            const shiteiNo = selectedRow.dataset.shiteiNo;
            const nendo = selectedRow.dataset.nendo;
            const kibetsu = selectedRow.dataset.kibetsu;

            // すべての情報が揃っているか確認
            if (shiteiNo && nendo && kibetsu) {
                // Controllerの複合キー対応エンドポイントへ遷移
                // パス構成: /declaration/view/{shiteiNo}/{nendo}/{kibetsu}
                window.location.href = `/accommodation-tax/declaration/view/${shiteiNo}/${nendo}/${kibetsu}`;
            } else {
                console.error('必要なデータ属性が不足しています:', { shiteiNo, nendo, kibetsu });
                alert('データの取得に失敗しました。');
            }
        });
    }
});