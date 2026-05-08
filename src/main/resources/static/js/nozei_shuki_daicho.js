function updateButtons() {
    const checked = document.querySelectorAll('.nozei-shuki-checkbox:checked');
    const editBtn = document.getElementById('editBtn');

    const singleSelection = checked.length === 1;
    editBtn.disabled = !singleSelection;
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
        
        // 納期限の計算
        let noki = '';
        if (item.shuki) {
            switch (item.shuki) {
                case 1:
                    noki = '翌月末日';
                    break;
				case 2:
					noki = '6月,8月,10月,12月,翌年2月,翌年4月末日';
					break;
                case 3:
                    noki = '7月,10月,翌年1月,翌年4月末日';
                    break;
                case 4:
                    noki = '8月,12月,翌年4月末日';
                    break;
                case 6:
                    noki = '10月,翌年4月末日';
                    break;
                case 12:
                    noki = '翌年5月末日';
                    break;
                default:
                    noki = '設定により決定';
            }
        }
        
        tr.innerHTML = `
            <td><input type="checkbox" class="nozei-shuki-checkbox" value="${item.seq}" onchange="updateButtons()"></td>
            <td>${item.label}</td>
            <td>${noki}</td>
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