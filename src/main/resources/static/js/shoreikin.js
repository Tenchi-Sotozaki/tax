/**
 * 特別徴収事務交付金画面用JavaScript
 */

function toggleAll() {
    // 1件のみ選択可能なため全選択機能は無効
    const selectAll = document.getElementById('selectAll');
    if (selectAll) {
        selectAll.checked = false;
    }
}

function getSelectedItems() {
    const checkboxes = document.querySelectorAll('.item-checkbox:checked');
    return Array.from(checkboxes).map(cb => ({
        shiteiNo: cb.value,
        nendo: cb.getAttribute('data-nendo')
    }));
}

function viewKofu() {
    const selected = getSelectedItems();
    if (selected.length === 0) {
        alert('交付金照会する項目を選択してください。');
        return;
    }

    if (selected.length > 1) {
        alert('交付金照会は1件ずつ選択してください。');
        return;
    }

    const selectedItem = selected[0];

    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/accommodation-tax/shoreikin/viewKofu';

    // CSRFトークンを追加
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    if (csrfToken) {
        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = '_csrf';
        csrfInput.value = csrfToken;
        form.appendChild(csrfInput);
    }

    // 選択された指定番号を追加
    const shiteiNoInput = document.createElement('input');
    shiteiNoInput.type = 'hidden';
    shiteiNoInput.name = 'selectedItems';
    shiteiNoInput.value = selectedItem.shiteiNo;
    form.appendChild(shiteiNoInput);

    // 選択された行の年度情報を追加
    if (selectedItem.nendo && selectedItem.nendo !== 'null') {
        const nendoInput = document.createElement('input');
        nendoInput.type = 'hidden';
        nendoInput.name = 'nendo';
        nendoInput.value = selectedItem.nendo;
        form.appendChild(nendoInput);
    }

    document.body.appendChild(form);
    form.submit();
}

function viewKoza() {
    const selected = getSelectedItems();
    if (selected.length === 0) {
        alert('口座照会する項目を選択してください。');
        return;
    }

    if (selected.length > 1) {
        alert('口座照会は1件ずつ選択してください。');
        return;
    }

    const selectedItem = selected[0];

    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/accommodation-tax/shoreikin/viewKoza';

    // CSRFトークンを追加
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    if (csrfToken) {
        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = '_csrf';
        csrfInput.value = csrfToken;
        form.appendChild(csrfInput);
    }

    const shiteiNoInput = document.createElement('input');
    shiteiNoInput.type = 'hidden';
    shiteiNoInput.name = 'selectedItems';
    shiteiNoInput.value = selectedItem.shiteiNo;
    form.appendChild(shiteiNoInput);

    document.body.appendChild(form);
    form.submit();
}

// チェックボックスの単一選択制御
document.addEventListener('DOMContentLoaded', function() {
    const checkboxes = document.querySelectorAll('.item-checkbox');
    checkboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            if (this.checked) {
                // 他のチェックボックスをすべて未選択にする
                checkboxes.forEach(cb => {
                    if (cb !== this) {
                        cb.checked = false;
                    }
                });
            }
        });
    });
});