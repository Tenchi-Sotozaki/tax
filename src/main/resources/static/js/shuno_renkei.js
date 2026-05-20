// 既存プロジェクトのスタイルに合わせたシンプルなVanilla JS実装
(function () {
    'use strict';

    // サンプルデータ（本番ではサーバーから取得する想定）
    const data = [
        { shitei_no: 'S0001', atena_no: 'A100', name: '山田 太郎', taisho_ym: '2024-04', total_zeigaku: 1250000, toroku_ymd: '2024-04-10', shinkoku_ymd: '2024-04-15' },
        { shitei_no: 'S0002', atena_no: 'A101', name: '鈴木 次郎', taisho_ym: '2024-05', total_zeigaku: 980500, toroku_ymd: '2024-05-12', shinkoku_ymd: '2024-05-20' },
        { shitei_no: 'S0003', atena_no: 'A102', name: '佐藤 花子', taisho_ym: '2024-06', total_zeigaku: 1450200, toroku_ymd: '2024-06-05', shinkoku_ymd: '2024-06-10' }
    ];

    const table_body = document.querySelector('#shunoTable tbody');
    const result_count = document.getElementById('resultCount');

    function render_rows(rows) {
        table_body.innerHTML = '';
        rows.forEach((r, idx) => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><input type="checkbox" class="row-check" data-idx="${idx}"></td>
                <td>${escape_html(r.shitei_no)}</td>
                <td>${escape_html(r.atena_no)}</td>
                <td>${escape_html(r.name)}</td>
                <td>${escape_html(r.taisho_ym)}</td>
                <td class="text-end fw-bold">${number_with_commas(r.total_zeigaku)}</td>
                <td>${escape_html(r.toroku_ymd)}</td>
                <td>${escape_html(r.shinkoku_ymd)}</td>
            `;
            table_body.appendChild(tr);
        });
        result_count.textContent = rows.length;
    }

    function number_with_commas(x) {
        return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
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

    // 初期描画
    render_rows(data);

    // 検索処理：サーバー API を呼ぶ。フォームの id が付いていないテンプレートにも対応
    (function attach_search() {
        const form = document.querySelector('#searchPanel form') || document.querySelector('form');
        if (!form) return;
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            const shinkoku_from = document.getElementById('shinkokuFrom')?.value || '';
            const shinkoku_to = document.getElementById('shinkokuTo')?.value || '';
            const taisho_month = document.getElementById('taishoMonth')?.value || '';
            const shitei_no = document.getElementById('shiteiNo')?.value.trim() || '';
            const name = document.getElementById('name')?.value.trim() || '';

            const params = new URLSearchParams();
            if (shinkoku_from) params.append('shinkokuFrom', shinkoku_from);
            if (shinkoku_to) params.append('shinkokuTo', shinkoku_to);
            if (taisho_month) params.append('taishoMonth', taisho_month);
            if (shitei_no) params.append('shiteiNo', shitei_no);
            if (name) params.append('name', name);

            fetch(window.location.origin + '/shunoRenkei/api/search?' + params.toString(), { credentials: 'same-origin' })
                .then(r => r.json())
                .then(json => {
                    const rows = json.map(item => ({
                        shitei_no: item.shiteiNo || item.shitei_no || '',
                        atena_no: item.atenaNo || item.atena_no || '',
                        name: item.name || '',
                        taisho_ym: item.taishoYm || item.taisho_ym || '',
                        total_zeigaku: item.totalZeigaku || item.total_zeigaku || '',
                        toroku_ymd: item.torokuYmd || item.toroku_ymd || '',
                        shinkoku_ymd: item.shinkokuYmd || item.shinkoku_ymd || ''
                    }));
                    render_rows(rows);
                    const sel = document.getElementById('selectAll'); if (sel) sel.checked = false;
                })
                .catch(err => {
                    console.error('検索APIエラー', err);
                    alert('検索に失敗しました。コンソールを確認してください。');
                });
        });
    })();

    document.getElementById('clearBtn')?.addEventListener('click', function () {
        document.getElementById('searchForm')?.reset();
        render_rows(data);
        document.getElementById('selectAll').checked = false;
    });

    // select all
    document.getElementById('selectAll')?.addEventListener('change', function (e) {
        const checks = document.querySelectorAll('.row-check');
        checks.forEach(c => c.checked = e.target.checked);
    });

    // 照会ボタン（選択したレコードの簡易表示）
    document.getElementById('viewBtn')?.addEventListener('click', function () {
        const selected = get_selected_rows();
        if (selected.length === 0) {
            alert('照会するレコードを選択してください。');
            return;
        }
        const info = selected.map(r => `${r.shitei_no} / ${r.name} / ${r.taisho_ym}`).join('\n');
        alert('選択レコード:\n' + info);
    });

    // CSV出力
    document.getElementById('csvBtn')?.addEventListener('click', function () {
        const selected = get_selected_rows();
        if (selected.length === 0) {
            alert('CSV出力するレコードを選択してください。');
            return;
        }
        const headers = ['宛名番号','賦課年度','期別','登録年月日','申告年月日','対象年月','合計税額','市区町村税額','都道府県税額','加算金額区分','加算割合','加算金額','納期限'];

        const rows = selected.map(r => [
            r.atena_no || '',
            '', // nendo
            '', // kibetsu
            r.toroku_ymd || '',
            r.shinkoku_ymd || '',
            r.taisho_ym || '',
            r.total_zeigaku || '',
            '', // city_zeigaku
            '', // ken_zeigaku
            '', // kasan_kbn
            '', // kasan_ritsu
            '', // kasan_gaku
            ''  // nokigen
        ]);

        const csv_content = [headers, ...rows].map(e => e.map(v => '"' + String(v).replace(/"/g, '""') + '"').join(',')).join('\n');
        const blob = new Blob([csv_content], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = 'shuno_renkei.csv';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    });

    function get_selected_rows() {
        const checks = Array.from(document.querySelectorAll('.row-check'));
        const selected = [];
        const current_rows = Array.from(table_body.querySelectorAll('tr'));
        checks.forEach((c, idx) => {
            if (c.checked) {
                const row = current_rows[idx];
                if (!row) return;
                const cols = row.querySelectorAll('td');
                selected.push({
                    shitei_no: cols[1].textContent.trim(),
                    atena_no: cols[2].textContent.trim(),
                    name: cols[3].textContent.trim(),
                    taisho_ym: cols[4].textContent.trim(),
                    total_zeigaku: cols[5].textContent.replace(/,/g, '').trim(),
                    toroku_ymd: cols[6].textContent.trim(),
                    shinkoku_ymd: cols[7].textContent.trim()
                });
            }
        });
        return selected;
    }

})();
