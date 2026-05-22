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
    return Array.from(checkboxes).map(cb => cb.value);
}

function viewKofu() {
    const selected = getSelectedItems();
    if (selected.length === 0) {
        alert('交付金照会する項目を選択してください。');
        return;
    }
    
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
    
    // 選択されたアイテムを追加
    selected.forEach(item => {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'selectedItems';
        input.value = item;
        form.appendChild(input);
    });
    
    // 年度情報を追加
    const nendoInput = document.getElementById('nendo');
    if (nendoInput && nendoInput.value) {
        const nendoHidden = document.createElement('input');
        nendoHidden.type = 'hidden';
        nendoHidden.name = 'nendo';
        nendoHidden.value = nendoInput.value;
        form.appendChild(nendoHidden);
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
    
    selected.forEach(item => {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'selectedItems';
        input.value = item;
        form.appendChild(input);
    });
    
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