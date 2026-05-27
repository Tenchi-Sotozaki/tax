document.querySelectorAll('.user-checkbox').forEach(cb => {
    cb.addEventListener('change', function () {
        document.querySelectorAll('.user-checkbox').forEach(other => {
            if (other !== this) other.checked = false;
        });
        document.getElementById('editBtn').disabled =
            !document.querySelector('.user-checkbox:checked');
    });
});

function goToEdit() {
    const checked = document.querySelector('.user-checkbox:checked');
    if (!checked) return;
    location.href = editBasePath + encodeURIComponent(checked.value);
}