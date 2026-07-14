document.getElementById('btnView').addEventListener('click', function () {
    const selected = document.querySelector('input[name="selectedRow"]:checked');
    if (!selected) {
        alert('照会するデータを選択してください。');
        return;
    }
    location.href = document.getElementById('btnView').dataset.viewBase + selected.value;
});
