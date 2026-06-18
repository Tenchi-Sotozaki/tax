// DOMの読み込み待ち
document.addEventListener('DOMContentLoaded', function() {

    function requireSelected(msg) {
        const cb = document.querySelector('.row-select:checked');
        if (!cb) { 
            alert(msg || 'レコードを選択してください。'); 
            return null; 
        }
        return cb.dataset.gassanShiteiNo;
    }

    // 行クリックでチェックボックスをトグル＋ハイライト
    document.querySelectorAll('tbody tr').forEach(function (row) {
        row.style.cursor = 'pointer';
        row.addEventListener('click', function (e) {
            if (e.target.closest('.btn, input[type="checkbox"]')) return;
            const cb = row.querySelector('.row-select');
            if (!cb) return;
            const next = !cb.checked;
            document.querySelectorAll('.row-select').forEach(o => {
                o.checked = false;
                o.closest('tr')?.classList.remove('row-selected');
            });
            cb.checked = next;
            row.classList.toggle('row-selected', next);
        });
    });

    document.querySelectorAll('.row-select').forEach(function (cb) {
        cb.addEventListener('click', function (e) {
            e.stopPropagation();
            document.querySelectorAll('.row-select').forEach(o => {
                if (o !== cb) {
                    o.checked = false;
                    o.closest('tr')?.classList.remove('row-selected');
                }
            });
            cb.closest('tr')?.classList.toggle('row-selected', cb.checked);
        });
    });

    // 照会ボタン
    document.getElementById('btnView')?.addEventListener('click', () => {
        const gassanShiteiNo = requireSelected('照会する合算申告情報を選択してください。');
        if (gassanShiteiNo) {
            location.href = '/accommodation-tax/gassan/view-form/' + gassanShiteiNo;
        }
    });

});