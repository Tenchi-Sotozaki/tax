document.addEventListener('DOMContentLoaded', function () {
    // 1. 金額不整合の警告モーダル表示処理
    const triggerFlag = document.getElementById('modalTriggerFlag');
    if (triggerFlag && triggerFlag.value === 'true') {
        const warningModal = new bootstrap.Modal(document.getElementById('taxWarningModal'));
        warningModal.show();
    }

    // 2. 「そのまま保存する」ボタンの処理
    const btnForceSave = document.getElementById('btnForceSave');
    if (btnForceSave) {
        btnForceSave.addEventListener('click', function () {
            document.getElementById('taxCheckBypassed').value = 'true';
            document.getElementById('fukaDeclarationForm').submit();
        });
    }

    // 3. 月計表モーダル内でのEnterキー抑制と反映ボタン連携
    const monthlyTallyModal = document.getElementById('monthlyTallyModal');
    if (monthlyTallyModal) {
        monthlyTallyModal.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                document.getElementById('btnApplyTally').click();
            }
        });
    }

    // 4. 登録日/更生日/修正日の動的ラベリング機能
    const modCategorySelect = document.getElementById('modificationCategory');
    const regDateLabel = document.getElementById('registrationDateLabel');
    if (modCategorySelect && regDateLabel) {
        const updateDateLabel = () => {
            const val = modCategorySelect.value;
            regDateLabel.textContent = (val === '1') ? '更生年月日' : (val === '2') ? '修正年月日' : '登録日';
        };
        updateDateLabel();
        modCategorySelect.addEventListener('change', updateDateLabel);
    }

    // 5. 月計表モーダルの反映ボタン処理（必要に応じてロジックを追記してください）
    const btnApply = document.getElementById('btnApplyTally');
    if (btnApply) {
        btnApply.addEventListener('click', function () {
            // ここに月計表の値を集計して親画面へ転記する処理を書く
            console.log('月計表の値を反映します');
        });
    }
});