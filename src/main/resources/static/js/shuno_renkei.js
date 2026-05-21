// 既存プロジェクトのスタイルに合わせたシンプルなVanilla JS実装
(function () {
    'use strict';

    // テンプレート側でサーバーから描画するため、クライアント側の初期描画は不要。
    const table_body = document.querySelector('#shunoTable tbody');
    const result_count = document.getElementById('resultCount');

    function number_with_commas(x) {
        if (x == null || x === '') return '';
        return String(x).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    }

    function escape_html(text) {
        if (text == null) return '';
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    // サーバーサイドで描画する設計のため、フォーム送信は通常の submit を利用します。
    // （必要なら API に切り替える実装を追加できますが、今回はサーバー描画優先）

    document.getElementById('clearBtn')?.addEventListener('click', function () {
        document.getElementById('searchForm')?.reset();
        // サーバー描画なのでフォームリセット後は submit して再描画するか手動で行う
        document.getElementById('selectAll').checked = false;
    });

    // select all: テンプレートの name="selectedIds" を対象にする
    document.getElementById('selectAll')?.addEventListener('change', function (e) {
        const checks = document.querySelectorAll('input[name="selectedIds"]');
        checks.forEach(c => c.checked = e.target.checked);
    });

    // 照会ボタン（選択したレコードの簡易表示）
    document.getElementById('viewBtn')?.addEventListener('click', function () {
        const selected = get_selected_rows();
        if (selected.length === 0) {
            alert('照会するレコードを選択してください。');
            return;
        }
        // サーバーの kakunin エンドポイントに選択キーを POST して画面遷移
        const keys = selected.map(s => ({ shiteiNo: s.shiteiNo, nendo: s.nendo || '', kibetsu: s.kibetsu != null && s.kibetsu !== '' ? Number(s.kibetsu) : null }));
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = window.location.origin + '/shunoRenkei/kakunin';
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'keysJson';
        input.value = JSON.stringify(keys);
        form.appendChild(input);
        document.body.appendChild(form);
        form.submit();
    });

    // CSV出力
    document.getElementById('csvBtn')?.addEventListener('click', function () {
        const selected = get_selected_rows();
        if (selected.length === 0) {
            alert('CSV出力するレコードを選択してください。');
            return;
        }
        // サーバー側の /download/csv を呼び出してファイルを取得する
        const keys = selected.map(s => ({ shiteiNo: s.shiteiNo, nendo: s.nendo || '', kibetsu: s.kibetsu != null && s.kibetsu !== '' ? Number(s.kibetsu) : null }));
        fetch(window.location.origin + '/shunoRenkei/download/csv', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'same-origin',
            body: JSON.stringify(keys)
        }).then(resp => {
            if (!resp.ok) throw new Error('CSV download failed');
            return resp.blob().then(blob => ({
                blob: blob,
                disposition: resp.headers.get('Content-Disposition')
            }));
        }).then(({ blob, disposition }) => {
            let filename = 'shuno_renkei.csv';
            if (disposition) {
                const m = /filename="?([^";]+)"?/.exec(disposition);
                if (m) filename = m[1];
            }
            const link = document.createElement('a');
            link.href = URL.createObjectURL(blob);
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }).catch(err => {
            console.error(err);
            alert('CSVの出力に失敗しました。コンソールを確認してください。');
        });
    });

    function get_selected_rows() {
        const checks = Array.from(document.querySelectorAll('input[name="selectedIds"]'));
        const selected = [];
        checks.forEach(c => {
            if (!c.checked) return;
            const tr = c.closest('tr');
            if (!tr) return;
            const cols = tr.querySelectorAll('td');
            // template columns: 0=checkbox,1=shiteiNo,2=atenaNo,3=name,4=taishoYm,5=totalZeigaku,6=torokuYmd,7=shinkokuYmd
            const shiteiNo = c.value || (cols[1] && cols[1].textContent.trim()) || '';
            const nendo = c.getAttribute('data-nendo') || '';
            const kibetsu = c.getAttribute('data-kibetsu') || '';
            const atenaNo = (cols[2] && cols[2].textContent.trim()) || '';
            const name = (cols[3] && cols[3].textContent.trim()) || '';
            const taishoYm = (cols[4] && cols[4].textContent.trim()) || '';
            const totalZeigaku = (cols[5] && cols[5].textContent.replace(/,/g, '').trim()) || '';
            const torokuYmd = (cols[6] && cols[6].textContent.trim()) || '';
            const shinkokuYmd = (cols[7] && cols[7].textContent.trim()) || '';
            selected.push({
                shiteiNo: shiteiNo,
                nendo: nendo,
                kibetsu: kibetsu,
                atenaNo: atenaNo,
                name: name,
                taishoYm: taishoYm,
                totalZeigaku: totalZeigaku,
                torokuYmd: torokuYmd,
                shinkokuYmd: shinkokuYmd
            });
        });
        return selected;
    }

})();
