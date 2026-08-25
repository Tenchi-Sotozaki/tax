'use strict';
(function() {
    const ADDR_API = '/accommodation-tax/api/address/search-or';

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
            const modal = new bootstrap.Modal(document.getElementById('addressSearchModal'));
            modal.show();
            document.getElementById('addressSearchModal').addEventListener('shown.bs.modal', () => {
                document.getElementById('addrSearchBtn').click();
            }, { once: true });
        }

        // 宛名検索ボタン（施設情報エリア）
        document.getElementById('openAddrSearchBtn')?.addEventListener('click', () => {
            new bootstrap.Modal(document.getElementById('addressSearchModal')).show();
        });

        // 特別徴収義務者指定モーダル：検索・選択処理
        document.getElementById('sgSearchBtn')?.addEventListener('click', executeSgSearch);
        ['sgSearchShiteiNo', 'sgSearchGassanShiteiNo', 'sgSearchName', 'sgSearchShisetsuName', 'sgSearchKojinNo', 'sgSearchHojinNo'].forEach(id => {
            document.getElementById(id)?.addEventListener('keydown', e => {
                if (e.key === 'Enter') { e.preventDefault(); document.getElementById('sgSearchBtn').click(); }
            });
        });

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

    const SG_SEARCH_API = '/accommodation-tax/api/shitei-gassan/search';
    let _sgResults = [];

    async function executeSgSearch() {
        const params = new URLSearchParams();
        const val = (id) => document.getElementById(id)?.value.trim() || '';
        if (val('sgSearchShiteiNo')) params.set('shiteiNo', val('sgSearchShiteiNo'));
        if (val('sgSearchGassanShiteiNo')) params.set('gassanShiteiNo', val('sgSearchGassanShiteiNo'));
        if (val('sgSearchName')) {
            params.set('name', val('sgSearchName'));
            params.set('nameMatchType', document.querySelector('input[name="sgSearchNameMatchType"]:checked')?.value || 'partial');
        }
        if (val('sgSearchShisetsuName')) {
            params.set('shisetsuName', val('sgSearchShisetsuName'));
            params.set('shisetsuNameMatchType', document.querySelector('input[name="sgSearchShisetsuNameMatchType"]:checked')?.value || 'partial');
        }
        if (val('sgSearchKojinNo')) params.set('kojinNo', val('sgSearchKojinNo'));
        if (val('sgSearchHojinNo')) params.set('hojinNo', val('sgSearchHojinNo'));
        try {
            const res = await fetch(`${SG_SEARCH_API}?${params}`);
            const data = await res.json();
            data.sort((a, b) => (a.shiteiNo ?? '').localeCompare(b.shiteiNo ?? ''));
            renderSgResults(data);
        } catch {
            document.getElementById('sgSearchResult').innerHTML =
                '<p class="text-danger small">通信エラーが発生しました。</p>';
        }
    }

    function renderSgResults(data) {
        const container = document.getElementById('sgSearchResult');
        if (!data.length) {
            container.innerHTML = '<p class="text-muted text-center small">該当する特別徴収義務者が見つかりませんでした。</p>';
            return;
        }
        _sgResults = data;
        const rows = data.map((d, i) => `
            <tr style="cursor:pointer" data-idx="${i}">
                <td>${d.gassanShiteiNo ? '' : (d.shiteiNo ?? '')}</td>
                <td>${d.gassanShiteiNo ?? ''}</td>
                <td>${d.name ?? ''}</td>
                <td>${d.shisetsuName ?? ''}</td>
            </tr>`).join('');
        container.innerHTML = `
            <p class="small text-muted mb-1">行をクリックすると選択されます。</p>
            <div class="table-responsive">
                <table class="table table-sm table-hover table-bordered mb-0">
                    <thead class="table-primary">
                        <tr><th>指定番号</th><th>合算指定番号</th><th>氏名/名称</th><th>施設名称</th></tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>`;
        container.querySelectorAll('tbody tr').forEach(tr => {
            tr.addEventListener('click', async () => {
                const d = _sgResults[+tr.dataset.idx];
                bootstrap.Modal.getInstance(document.getElementById('shiteiGassanSearchModal')).hide();
                try {
                    const res = await fetch('/accommodation-tax/eltax-renkei/kakunin/repreview', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            [document.querySelector('meta[name="_csrf_header"]')?.content]: document.querySelector('meta[name="_csrf"]')?.content
                        },
                        body: JSON.stringify({ shiteiNo: d.shiteiNo ?? '' })
                    });
                    if (!res.ok) { alert(await res.text()); return; }
                    const dto = await res.json();
                    applyRepreviewDto(dto);
                } catch {
                    alert('通信エラーが発生しました。');
                }
            });
        });
    }

    function applyRepreviewDto(dto) {
        // 施設情報エリア更新
        document.getElementById('shiteiNoText').value = dto.shiteiNo ?? '';
        document.getElementById('shisetsuNameText').value = dto.shisetsuName ?? '';
        document.getElementById('shisetsuJushoText').value = dto.shisetsuJusho ?? '';
        document.getElementById('tokugimuNameText').value = dto.atenaName ?? '';
        document.getElementById('tokugimuJushoText').value = dto.atenaJusho ?? '';
        // diffRowsテーブル更新
        const tbody = document.querySelector('.card .card-body.p-0 tbody');
        if (!tbody || !dto.diffRows) return;
        tbody.innerHTML = dto.diffRows.length === 0
            ? '<tr><td colspan="3" class="text-center p-3">取込結果がありません</td></tr>'
            : dto.diffRows.map(row => {
                const isDiff = row.beforeValue !== row.afterValue
                    && row.beforeValue !== ''
                    && !(row.beforeValue === '－' && (row.afterValue ?? '') === '');
                const bg = row.dispFlg === '1' ? 'background-color: #dee2e6 !important;' : '';
                const cls = isDiff ? 'class="table-warning"' : '';
                return `<tr ${cls}>
                    <td style="${bg}">${row.itemName ?? ''}</td>
                    <td style="${bg}">${row.beforeValue ?? ''}</td>
                    <td style="${bg}"><span>${row.afterValue ?? ''}</span></td>
                </tr>`;
            }).join('');
    }

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