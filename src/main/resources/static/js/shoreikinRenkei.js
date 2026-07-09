// 交付金振込情報連携のJavaScript
(function () {
    'use strict';

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

    // select all: テンプレートの name="selectedIds" を対象にする
    document.getElementById('selectAll')?.addEventListener('change', function (e) {
        const checks = document.querySelectorAll('input[name="selectedIds"]');
        checks.forEach(c => c.checked = e.target.checked);
    });

    // 照会ボタン（選択したレコードの詳細表示）
    document.getElementById('viewBtn')?.addEventListener('click', function () {
        const selected = get_selected_rows();
        if (selected.length === 0) {
            alert('照会するレコードを選択してください。');
            return;
        }
        // サーバーの kakunin エンドポイントに選択キーを POST して画面遷移
        const keys = selected.map(s => ({ 
            shiteiNo: s.shiteiNo, 
            nendo: s.nendo || ''
        }));
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = window.location.origin + '/shoreikinRenkei/kakunin';
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
        // サーバー側の /download を呼び出してファイルを取得する
        const keys = selected.map(s => ({ 
            shiteiNo: s.shiteiNo, 
            nendo: s.nendo || ''
        }));
        fetch(window.location.origin + '/shoreikinRenkei/download', {
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
            let filename = 'shoreikin_renkei.csv';
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
            // template columns: 0=checkbox,1=shiteiNo,2=atenaNo,3=name,4=nendo,5=kofuGaku,6=kofuYmd
            const shiteiNo = c.value || (cols[1] && cols[1].textContent.trim()) || '';
            const nendo = c.getAttribute('data-nendo') || '';
            const atenaNo = (cols[2] && cols[2].textContent.trim()) || '';
            const name = (cols[3] && cols[3].textContent.trim()) || '';
            const kofuGaku = (cols[5] && cols[5].textContent.replace(/,/g, '').trim()) || '';
            const kofuYmd = (cols[6] && cols[6].textContent.trim()) || '';
            
            selected.push({
                shiteiNo: shiteiNo,
                nendo: nendo,
                atenaNo: atenaNo,
                name: name,
                kofuGaku: kofuGaku,
                kofuYmd: kofuYmd
            });
        });
        return selected;
    }

})();