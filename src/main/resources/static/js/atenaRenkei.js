/**
 * 宛名情報取込 画面用JavaScript
 *
 * 「内容取込」押下時はCSVを解析するだけでDBは更新せず、
 * 既存データとの差分を確認モーダルに表示する。
 * モーダルで取込対象を選択したうえで確定処理を行う。
 */

// 差分ありの宛名番号ごとの選択状態（true:取り込む / false:取り込まない）
const selection = new Map();

// 再読み込みをまたいで完了メッセージを引き継ぐためのキー
const COMPLETED_MESSAGE_KEY = 'atenaImportCompletedMessage';

document.addEventListener('DOMContentLoaded', function () {

    // 取込完了後の再読み込み時にメッセージを表示する
    const completed = sessionStorage.getItem(COMPLETED_MESSAGE_KEY);
    if (completed) {
        sessionStorage.removeItem(COMPLETED_MESSAGE_KEY);
        showAlert('success', completed);
    }

    const form = document.getElementById('importForm');
    if (form) {
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            analyze();
        });
    }

    const btnConfirm = document.getElementById('btnConfirmImport');
    if (btnConfirm) {
        btnConfirm.addEventListener('click', confirmImport);
    }

    document.querySelectorAll('.js-detail-link').forEach(link => {
        link.addEventListener('click', function (e) {
            e.preventDefault();
            showDetail(this.dataset.seq, this.dataset.fileName);
        });
    });
});

/**
 * CSRFヘッダーを付与したヘッダーを組み立てる
 */
function buildHeaders(base) {
    const headers = base || {};
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    if (csrfHeader && csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    return headers;
}

function showAlert(type, message) {
    const box = document.getElementById('importAlert');
    const text = document.getElementById('importAlertText');
    if (!box || !text) return;
    box.classList.remove('d-none', 'alert-success', 'alert-danger');
    box.classList.add(type === 'success' ? 'alert-success' : 'alert-danger');
    text.textContent = message;
}

/**
 * 前回の処理結果メッセージを消す
 */
function clearAlert() {
    const box = document.getElementById('importAlert');
    if (box) {
        box.classList.add('d-none');
    }
    // サーバーサイドで描画された成功・エラーメッセージも消す
    document.querySelectorAll('.alert-dismissible').forEach(el => {
        if (el.id !== 'importAlert') {
            el.classList.add('d-none');
        }
    });
}

/**
 * CSVを解析して差分確認モーダルを表示する
 */
async function analyze() {
    // 前回のエラーメッセージが残らないようにする
    clearAlert();

    const fileInput = document.getElementById('file');
    if (!fileInput || !fileInput.files.length) {
        showAlert('danger', 'ファイルを選択してください。');
        return;
    }

    const btn = document.getElementById('btnAnalyze');
    btn.disabled = true;

    try {
        const formData = new FormData();
        formData.append('file', fileInput.files[0]);

        const res = await fetch(window.atenaImportUrls.analyze, {
            method: 'POST',
            headers: buildHeaders({}),
            body: formData
        });

        const body = await res.json();
        if (!res.ok) {
            showAlert('danger', body.message || '取込内容の確認に失敗しました。');
            return;
        }

        // 差分が無い場合は確認モーダルを表示せず、そのまま取り込む
        const sabunRows = (body.rows || []).filter(r => r.sabunAri);
        if (sabunRows.length === 0) {
            selection.clear();
            await confirmImport();
            return;
        }
        renderSabunModal(body);
    } catch (err) {
        console.error(err);
        showAlert('danger', '取込内容の確認に失敗しました。');
    } finally {
        btn.disabled = false;
    }
}

/**
 * 解析結果を差分確認モーダルに描画する
 */
function renderSabunModal(preview) {
    selection.clear();

    document.getElementById('sumShinki').textContent = preview.shinkiKensu;
    document.getElementById('sumSabun').textContent = preview.sabunKensu;
    document.getElementById('sumSaiNashi').textContent = preview.saiNashiKensu;

    const tbody = document.getElementById('sabunList');
    tbody.innerHTML = '';

    const sabunRows = (preview.rows || []).filter(r => r.sabunAri);

    sabunRows.forEach(row => {
        // 初期値は「取り込む」
        selection.set(row.atenaNo, true);
        tbody.appendChild(buildSabunRow(row));
        tbody.appendChild(buildDetailRow(row));
    });

    new bootstrap.Modal(document.getElementById('sabunModal')).show();
}

/**
 * 差分1件分の行を組み立てる
 */
function buildSabunRow(row) {
    const tr = document.createElement('tr');

    const tdNo = document.createElement('td');
    tdNo.textContent = row.atenaNo;
    tr.appendChild(tdNo);

    const tdName = document.createElement('td');
    tdName.textContent = row.name;
    tr.appendChild(tdName);

    // 詳細表示（クリックで下に展開する）
    const tdToggle = document.createElement('td');
    tdToggle.className = 'text-center';
    const toggle = document.createElement('button');
    toggle.type = 'button';
    toggle.className = 'btn btn-link btn-sm p-0 text-decoration-none';
    toggle.textContent = '詳細▼';
    toggle.addEventListener('click', () => {
        const detail = document.getElementById(detailRowId(row.atenaNo));
        const hidden = detail.classList.toggle('d-none');
        toggle.textContent = hidden ? '詳細▼' : '詳細▲';
    });
    tdToggle.appendChild(toggle);
    tr.appendChild(tdToggle);

    // 取り込む／取り込まない
    const tdAction = document.createElement('td');
    tdAction.className = 'text-center';
    const group = document.createElement('div');
    group.className = 'btn-group btn-group-sm';

    const btnTorikomu = document.createElement('button');
    btnTorikomu.type = 'button';
    btnTorikomu.className = 'btn btn-primary';
    btnTorikomu.textContent = '取り込む';

    const btnSkip = document.createElement('button');
    btnSkip.type = 'button';
    btnSkip.className = 'btn btn-outline-primary';
    btnSkip.textContent = '取り込まない';

    btnTorikomu.addEventListener('click', () => {
        selection.set(row.atenaNo, true);
        btnTorikomu.className = 'btn btn-primary';
        btnSkip.className = 'btn btn-outline-primary';
    });
    btnSkip.addEventListener('click', () => {
        selection.set(row.atenaNo, false);
        btnTorikomu.className = 'btn btn-outline-primary';
        btnSkip.className = 'btn btn-primary';
    });

    group.appendChild(btnTorikomu);
    group.appendChild(btnSkip);
    tdAction.appendChild(group);
    tr.appendChild(tdAction);

    return tr;
}

/**
 * 差分詳細（項目名／現在／差分）の行を組み立てる
 */
function buildDetailRow(row) {
    const tr = document.createElement('tr');
    tr.id = detailRowId(row.atenaNo);
    tr.className = 'd-none';

    const td = document.createElement('td');
    td.colSpan = 4;
    td.className = 'bg-light-subtle';

    const table = document.createElement('table');
    table.className = 'table table-sm table-bordered mb-0';

    const thead = document.createElement('thead');
    thead.innerHTML = '<tr><th style="width:180px;">項目名</th><th>現在</th><th>差分（更新内容）</th></tr>';
    table.appendChild(thead);

    const tbody = document.createElement('tbody');
    (row.diffs || []).forEach(d => {
        const dtr = document.createElement('tr');

        const label = document.createElement('td');
        label.textContent = d.label;
        dtr.appendChild(label);

        const current = document.createElement('td');
        current.textContent = d.current;
        dtr.appendChild(current);

        const updated = document.createElement('td');
        updated.textContent = d.updated;
        // 変更のある項目は赤字で強調する
        if (d.changed) {
            updated.className = 'text-danger fw-bold';
        }
        dtr.appendChild(updated);

        tbody.appendChild(dtr);
    });
    table.appendChild(tbody);

    td.appendChild(table);
    tr.appendChild(td);
    return tr;
}

function detailRowId(atenaNo) {
    return 'sabunDetail-' + atenaNo;
}

/**
 * 選択結果を送信して取込を確定する
 */
async function confirmImport() {
    const btn = document.getElementById('btnConfirmImport');
    btn.disabled = true;

    try {
        const torikomu = [];
        selection.forEach((v, k) => {
            if (v) torikomu.push(k);
        });

        const res = await fetch(window.atenaImportUrls.confirm, {
            method: 'POST',
            headers: buildHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(torikomu)
        });

        const body = await res.json();
        if (!res.ok) {
            showAlert('danger', body.message || '取込に失敗しました。');
            return;
        }
        // 再読み込みで消えてしまうため、完了メッセージを退避してから読み込み直す
        sessionStorage.setItem(COMPLETED_MESSAGE_KEY, body.message || 'CSVファイルの取込が完了しました。');
        // 取込済みファイル一覧を最新化するため再読み込みする
        window.location.reload();
    } catch (err) {
        console.error(err);
        showAlert('danger', '取込に失敗しました。');
    } finally {
        btn.disabled = false;
    }
}

/**
 * 取込結果（どの宛名を取り込んだか）を表示する
 */
async function showDetail(seq, fileName) {
    try {
        const res = await fetch(window.atenaImportUrls.detail + encodeURIComponent(seq));
        if (!res.ok) {
            showAlert('danger', '取込結果の取得に失敗しました。');
            return;
        }
        const list = await res.json();

        document.getElementById('detailFileName').textContent = fileName || '';
        const tbody = document.getElementById('detailList');
        tbody.innerHTML = '';

        list.forEach(d => {
            const tr = document.createElement('tr');

            const tdNo = document.createElement('td');
            tdNo.textContent = d.atenaNo;
            tr.appendChild(tdNo);

            const tdName = document.createElement('td');
            tdName.textContent = d.name;
            tr.appendChild(tdName);

            const tdKbn = document.createElement('td');
            tdKbn.className = 'text-center';
            tdKbn.textContent = kbnLabel(d.kbn);
            if (d.kbn === '2') {
                tdKbn.classList.add('text-success');
            } else if (d.kbn === '3') {
                tdKbn.classList.add('text-danger');
            }
            tr.appendChild(tdKbn);

            tbody.appendChild(tr);
        });

        new bootstrap.Modal(document.getElementById('detailModal')).show();
    } catch (err) {
        console.error(err);
        showAlert('danger', '取込結果の取得に失敗しました。');
    }
}

function kbnLabel(kbn) {
    switch (kbn) {
        case '1': return '差異なし';
        case '2': return '取込';
        case '3': return 'スキップ';
        default: return '';
    }
}
