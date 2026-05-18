const form = document.getElementById('taxSystemForm');
const deleteBtn = document.getElementById('deleteBtn');

form.addEventListener('submit', function (e) {
    e.preventDefault();

    document.querySelectorAll('.text-danger.small').forEach(el => el.classList.add('d-none'));
    document.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    document.getElementById('errorFutureDate').classList.add('d-none');
    document.getElementById('errorDuplicate').classList.add('d-none');

    let valid = true;

    if (!document.querySelector('input[name="taxType"]:checked')) {
        document.getElementById('taxTypeError').classList.remove('d-none');
        valid = false;
    }
    if (!document.querySelector('input[name="taxCategory"]:checked')) {
        document.getElementById('taxCategoryError').classList.remove('d-none');
        valid = false;
    }
    const period = document.getElementById('applicablePeriod');
    if (!period.value) {
        document.getElementById('applicablePeriodError').classList.remove('d-none');
        period.classList.add('is-invalid');
        valid = false;
    }
    const amount1 = document.getElementById('taxAmount1');
    if (!amount1.value.trim()) {
        document.getElementById('taxAmount1Error').classList.remove('d-none');
        amount1.classList.add('is-invalid');
        valid = false;
    }
    const cond1From = document.getElementById('taxCond1From');
    const cond1To = document.getElementById('taxCond1To');
    if (!cond1From.value.trim() || !cond1To.value.trim()) {
        document.getElementById('taxCond1Error').classList.remove('d-none');
        if (!cond1From.value.trim()) cond1From.classList.add('is-invalid');
        if (!cond1To.value.trim()) cond1To.classList.add('is-invalid');
        valid = false;
    }

    if (!valid) return;

    // TODO: 登録処理
});

deleteBtn.addEventListener('click', function () {
    new bootstrap.Modal(document.getElementById('deleteConfirmModal')).show();
});

document.getElementById('confirmDeleteBtn').addEventListener('click', function () {
    bootstrap.Modal.getInstance(document.getElementById('deleteConfirmModal')).hide();
    // TODO: 削除処理
});
