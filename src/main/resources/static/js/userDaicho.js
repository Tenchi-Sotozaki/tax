document.querySelectorAll('.user-checkbox').forEach(cb => {
    cb.addEventListener('change', function () {
        const checked = document.querySelectorAll('.user-checkbox:checked');
        document.getElementById('editBtn').disabled = checked.length !== 1;
        document.getElementById('deleteBtn').disabled = checked.length === 0;
    });
});

function goToEdit() {
    const checked = document.querySelector('.user-checkbox:checked');
    if (!checked) return;
    location.href = editBasePath + encodeURIComponent(checked.value);
}

function confirmDelete() {
    const checked = document.querySelectorAll('.user-checkbox:checked');
    if (checked.length === 0) return;
    const form = document.getElementById('deleteForm');
    const csrfInput = form.querySelector('input[name="_csrf"]');
    form.innerHTML = '';
    if (csrfInput) form.appendChild(csrfInput);
    checked.forEach(cb => {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'ids';
        input.value = cb.value;
        form.appendChild(input);
    });
    new bootstrap.Modal(document.getElementById('deleteModal')).show();
}
