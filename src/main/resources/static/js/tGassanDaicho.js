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

	// 検索条件初期化
    const resetBtn = document.getElementById('btn-reset');
    if (resetBtn) {
        resetBtn.addEventListener('click', function() {
            // テキスト入力欄をクリア
            document.getElementById('gassanShiteiNo').value = '';
            document.getElementById('shiteiNo').value = '';
            document.getElementById('name').value = '';

            // ラジオボタンを「部分」（デフォルト）にチェックを入れる
            const partialRadio = document.getElementById('namePartial');
            if (partialRadio) {
                partialRadio.checked = true;
            }
        });
    }
});