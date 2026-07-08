function clearForm() {
    document.getElementById('searchForm').reset();
    document.getElementById('targetYear').value = '2024-04';
}

document.getElementById('searchForm').addEventListener('submit', function (e) {
    e.preventDefault();
});
