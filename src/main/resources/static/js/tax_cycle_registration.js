document.getElementById('taxCycleForm').addEventListener('submit', function (e) {
    e.preventDefault();

    document.getElementById('taxCycleError').classList.add('d-none');
    document.getElementById('taxCycle').classList.remove('is-invalid');
    document.getElementById('errorDuplicate').classList.add('d-none');

    const taxCycle = document.getElementById('taxCycle');
    if (!taxCycle.value) {
        document.getElementById('taxCycleError').classList.remove('d-none');
        taxCycle.classList.add('is-invalid');
        return;
    }

    // TODO: 登録処理
});

document.getElementById('updateBtn').addEventListener('click', function () {
    // TODO: 更新処理
});

document.getElementById('deleteBtn').addEventListener('click', function () {
    new bootstrap.Modal(document.getElementById('deleteConfirmModal')).show();
});

document.getElementById('confirmDeleteBtn').addEventListener('click', function () {
    bootstrap.Modal.getInstance(document.getElementById('deleteConfirmModal')).hide();
    // TODO: 削除処理
});
