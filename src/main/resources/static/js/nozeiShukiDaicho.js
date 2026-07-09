document.addEventListener('DOMContentLoaded', () => searchNozeiShuki());

function updateButtons() {
    const checked = document.querySelectorAll('.nozei-shuki-checkbox:checked');
    const editBtn = document.getElementById('editBtn');
    editBtn.disabled = checked.length !== 1;
}

function exclusiveCheck(current) {
    document.querySelectorAll('.nozei-shuki-checkbox').forEach(cb => {
        if (cb !== current) cb.checked = false;
    });
    updateButtons();
}

function searchNozeiShuki() {
    const ctx = document.querySelector('[data-ctx]')?.dataset.ctx?.replace(/\/$/, '') ?? '';
    const shuki = document.getElementById('shuki').value;
    
    const params = new URLSearchParams();
    if (shuki) {
        params.append('shuki', shuki);
    }
    
    fetch(`${ctx}/admin/nozei-shuki/search?${params}`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                updateTable(data.data);
            } else {
                alert('検索に失敗しました: ' + data.message);
            }
        })
        .catch(err => alert('通信エラー: ' + err.message));
}

function updateTable(data) {
    const tbody = document.querySelector('#nozeiShukiTable tbody');
    tbody.innerHTML = '';
    
    if (data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" class="text-center">登録された周期がありません</td></tr>';
        updateButtons();
        return;
    }

    data.forEach(item => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><input type="checkbox" class="nozei-shuki-checkbox" value="${item.seq}" onchange="exclusiveCheck(this)"></td>
            <td>${item.label}</td>
            <td>${item.shinkokuKigen || ''}</td>
        `;
        tbody.appendChild(tr);
    });
    
    updateButtons();
}

function editNozeiShuki() {
    const checked = document.querySelector('.nozei-shuki-checkbox:checked');
    if (!checked) return;

    const seq = checked.value;
    const ctx = document.querySelector('[data-ctx]')?.dataset.ctx?.replace(/\/$/, '') ?? '';
    window.location.href = `${ctx}/admin/nozei-shuki/edit/${seq}`;
}