/**
 * 振込先口座照会／登録／編集画面用JavaScript
 */

function editMode() {
    // 編集モードに切り替え
    document.getElementById('editForm').submit();
}

function updateKoza() {
    if (!confirm('振込先口座情報を更新しますか？')) {
        return;
    }

    // メインフォームの値を更新用フォームにコピー
    const updateForm = document.getElementById('updateForm');

    // 各フィールドの値をコピー
    updateForm.querySelector('input[name="bankCd"]').value =
        document.getElementById('bankCd').value;
    updateForm.querySelector('input[name="bankName"]').value =
        document.getElementById('bankName').value;
    updateForm.querySelector('input[name="branchCd"]').value =
        document.getElementById('branchCd').value;
    updateForm.querySelector('input[name="branchName"]').value =
        document.getElementById('branchName').value;
    updateForm.querySelector('input[name="shumoku"]').value =
        document.getElementById('shumoku').value;
    updateForm.querySelector('input[name="kozaNo"]').value =
        document.getElementById('kozaNo').value;
    updateForm.querySelector('input[name="meigi"]').value =
        document.getElementById('meigi').value;

    // 更新フォームを送信
    updateForm.submit();
}