'use strict';
(function() {
    const ADDR_API = '/accommodation-tax/api/address/search';

    document.addEventListener('DOMContentLoaded', () => {
        const meta = document.getElementById('kakuninMeta');
        const atenaSearchRequired = meta.dataset.atenaSearchRequired === 'true';
        const tokugimuName = meta.dataset.tokugimuName || '';
        const tokugimuJusho = meta.dataset.tokugimuJusho || '';
        const tokugimuTel = meta.dataset.tokugimuTel || '';
        const kojinNo = meta.dataset.kojinNo || '';
        const hojinNo = meta.dataset.hojinNo || '';

        // 宛名検索が必要な場合はモーダルを自動表示し、特別徴収義務者名を初期入力
        if (atenaSearchRequired) {
            document.getElementById('addrSearchName').value = tokugimuName;
            document.getElementById('addrSearchAddress').value = tokugimuJusho;
            document.getElementById('addrSearchPhone').value = tokugimuTel;
            document.getElementById('addrSearchKojinNo').value = kojinNo;
            document.getElementById('addrSearchHojinNo').value = hojinNo;
            new bootstrap.Modal(document.getElementById('addressSearchModal')).show();
        }

        // 検索ボタン
        document.getElementById('addrSearchBtn').addEventListener('click', async () => {
            const params = new URLSearchParams();
            const no = document.getElementById('addrSearchNo').value.trim();
            const name = document.getElementById('addrSearchName').value.trim();
            const address = document.getElementById('addrSearchAddress').value.trim();
            const phone = document.getElementById('addrSearchPhone').value.trim();
            const kojinNo = document.getElementById('addrSearchKojinNo').value.trim();
            const hojinNo = document.getElementById('addrSearchHojinNo').value.trim();
            if (no) params.set('addressNumber', no);
            if (name) params.set('name', name);
            if (address) params.set('address', address);
            if (phone) params.set('phone', phone);
            if (kojinNo) params.set('kojinNo', kojinNo);
            if (hojinNo) params.set('hojinNo', hojinNo);
            try {
                const res = await fetch(`${ADDR_API}?${params}`);
                const data = await res.json();
                renderResults(data);
            } catch {
                document.getElementById('addrSearchResult').innerHTML =
                    '<p class="text-danger small">通信エラーが発生しました。</p>';
            }
        });

        // 取込ボタン：宛名検索必要な場合は選択必須
        document.getElementById('commitBtn').addEventListener('click', () => {
            if (atenaSearchRequired && !document.getElementById('commitAtenaNo').value) {
                alert('宛名を選択してください。');
                new bootstrap.Modal(document.getElementById('addressSearchModal')).show();
                return;
            }
            document.getElementById('commitForm').submit();
        });
    });

    let _results = [];
    function renderResults(data) {
        const container = document.getElementById('addrSearchResult');
        if (!data.length) {
            container.innerHTML = '<p class="text-muted text-center small">該当する宛名が見つかりませんでした。</p>';
            return;
        }
        _results = data;
        const rows = data.map((d, i) => `
            <tr style="cursor:pointer" data-idx="${i}">
                <td>${d.addressNumber ?? ''}</td>
                <td>${d.name ?? ''}</td>
                <td>${d.address ?? ''}</td>
            </tr>`).join('');
        container.innerHTML = `
            <p class="small text-muted mb-1">行をクリックすると選択されます。</p>
            <div class="table-responsive">
                <table class="table table-sm table-hover table-bordered mb-0">
                    <thead class="table-primary"><tr><th>宛名番号</th><th>氏名/名称</th><th>住所</th></tr></thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>`;
        container.querySelectorAll('tbody tr').forEach(tr => {
            tr.addEventListener('click', () => {
                const d = _results[+tr.dataset.idx];
                document.getElementById('commitAtenaNo').value = d.addressNumber ?? '';
                document.getElementById('tokugimuNameText').value = d.name ?? '';
                document.getElementById('tokugimuJushoText').value = d.address ?? '';
                bootstrap.Modal.getInstance(document.getElementById('addressSearchModal')).hide();
            });
        });
    }
}());