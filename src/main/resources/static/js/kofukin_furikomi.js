// 交付金振込情報連携画面のJavaScript

document.addEventListener('DOMContentLoaded', function() {
    
    // 年度カレンダーの処理
    const nendoDateInput = document.getElementById('nendo');
    const nendoHiddenInput = document.getElementById('nendoHidden');
    
    if (nendoDateInput && nendoHiddenInput) {
        // 年のみ表示の処理
        function updateYearDisplay() {
            if (nendoDateInput.value) {
                const selectedDate = new Date(nendoDateInput.value);
                const year = selectedDate.getFullYear();
                nendoHiddenInput.value = year;
                
                // 年のみを表示するためのカスタム表示
                nendoDateInput.setAttribute('data-year', year);
            } else {
                nendoHiddenInput.value = '';
                nendoDateInput.removeAttribute('data-year');
            }
        }
        
        // カレンダーの値が変更された時に年のみを抽出
        nendoDateInput.addEventListener('change', updateYearDisplay);
        
        // 初期値設定（既存の年度がある場合）
        if (nendoHiddenInput.value) {
            nendoDateInput.value = nendoHiddenInput.value + '-01-01';
            updateYearDisplay();
        }
        
        // フォーカス時とブラー時の処理
        nendoDateInput.addEventListener('focus', function() {
            this.style.color = '#495057';
        });
        
        nendoDateInput.addEventListener('blur', function() {
            updateYearDisplay();
        });
    }
    
    // 全選択チェックボックスの処理
    const selectAllCheckbox = document.getElementById('selectAll');
    const itemCheckboxes = document.querySelectorAll('input[name="selectedIds"]');
    
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            itemCheckboxes.forEach(checkbox => {
                checkbox.checked = this.checked;
            });
        });
    }
    
    // 個別チェックボックスの処理
    itemCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            const checkedCount = document.querySelectorAll('input[name="selectedIds"]:checked').length;
            selectAllCheckbox.checked = checkedCount === itemCheckboxes.length;
            selectAllCheckbox.indeterminate = checkedCount > 0 && checkedCount < itemCheckboxes.length;
        });
    });
    
    // 照会ボタンの処理
    const viewBtn = document.getElementById('viewBtn');
    if (viewBtn) {
        viewBtn.addEventListener('click', function() {
            const checkedItems = document.querySelectorAll('input[name="selectedIds"]:checked');
            if (checkedItems.length === 0) {
                alert('照会する項目を選択してください。');
                return;
            }
            if (checkedItems.length > 1) {
                alert('照会は1件ずつ行ってください。');
                return;
            }
            
            const shiteiNo = checkedItems[0].getAttribute('data-shiteiNo');
            const nendo = checkedItems[0].getAttribute('data-nendo');
            
            // 照会画面への遷移（実装に応じて調整）
            window.location.href = `/kofukinFurikomi/view/${shiteiNo}/${nendo}`;
        });
    }
    
    // CSV出力ボタンの処理
    const csvBtn = document.getElementById('csvBtn');
    if (csvBtn) {
        csvBtn.addEventListener('click', function() {
            const checkedItems = document.querySelectorAll('input[name="selectedIds"]:checked');
            if (checkedItems.length === 0) {
                alert('CSV出力する項目を選択してください。');
                return;
            }
            
            const keys = Array.from(checkedItems).map(item => ({
                shiteiNo: item.getAttribute('data-shiteiNo'),
                nendo: item.getAttribute('data-nendo')
            }));
            
            // CSRFトークンを取得
            const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
            
            // JSONでPOSTリクエストを送信
            fetch('/accommodation-tax/kofukinFurikomi/download', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify(keys)
            })
            .then(response => {
                if (response.ok) {
                    return response.blob();
                } else {
                    throw new Error('CSV出力に失敗しました');
                }
            })
            .then(blob => {
                // ファイルダウンロード
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'kofukin_furikomi.csv';
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);
            })
            .catch(error => {
                console.error('Error:', error);
                alert('CSV出力中にエラーが発生しました。');
            });
        });
    }
});