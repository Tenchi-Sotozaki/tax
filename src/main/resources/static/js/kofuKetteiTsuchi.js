/**
 * 宿泊税特別徴収事務交付金交付決定通知書 JavaScript
 */

/**
 * ページ読み込み時の初期化
 */
document.addEventListener('DOMContentLoaded', function() {
    // 発行年月日が空の場合、当日を設定
    const hakkoYmdInput = document.querySelector('input[name="hakkoYmd"]');
    if (hakkoYmdInput && (!hakkoYmdInput.value || hakkoYmdInput.value === '')) {
        const today = new Date();
        const year = today.getFullYear();
        const month = String(today.getMonth() + 1).padStart(2, '0');
        const day = String(today.getDate()).padStart(2, '0');
        const todayStr = `${year}-${month}-${day}`;
        hakkoYmdInput.value = todayStr;
    }
    
    // 初期表示更新
    updateDisplayDate();
});

/**
 * 日付表示を更新
 */
function updateDisplayDate() {
    const hakkoYmdInput = document.querySelector('input[name="hakkoYmd"]');
    const displayDate = document.getElementById('displayDate');
    
    if (hakkoYmdInput && displayDate) {
        const dateValue = hakkoYmdInput.value;
        if (dateValue) {
            const date = new Date(dateValue);
            const year = date.getFullYear();
            const month = date.getMonth() + 1;
            const day = date.getDate();
            displayDate.textContent = `${year}年${month}月${day}日`;
        } else {
            displayDate.textContent = '年　　月　　日';
        }
    }
}

/**
 * PDF生成
 */
function generatePdf() {
    if (!validateForm()) {
        return;
    }

    const form = document.getElementById('kofuKetteiForm');
    form.action = '/accommodation-tax/reports/kofuKetteiTsuchi/pdf';
    form.target = '_self';
    form.submit();
}

/**
 * プレビュー
 */
function preview() {
    if (!validateForm()) {
        return;
    }

    const form = document.getElementById('kofuKetteiForm');
    const formData = new FormData(form);

    fetch('/accommodation-tax/reports/kofuKetteiTsuchi/preview', {
        method: 'POST',
        body: formData
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('プレビューの生成に失敗しました');
            }
            return response.blob();
        })
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            window.open(url, '_blank');
            setTimeout(() => window.URL.revokeObjectURL(url), 1000);
        })
        .catch(error => {
            console.error('Error:', error);
            alert('プレビューの表示に失敗しました。');
        });
}

/**
 * 印刷
 */
function print() {
    if (!validateForm()) {
        return;
    }

    const form = document.getElementById('kofuKetteiForm');
    const formData = new FormData(form);

    fetch('/accommodation-tax/reports/kofuKetteiTsuchi/print', {
        method: 'POST',
        body: formData
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('印刷用PDFの生成に失敗しました');
            }
            return response.blob();
        })
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            const printWindow = window.open(url, '_blank');

            printWindow.onload = function() {
                setTimeout(() => {
                    printWindow.print();
                    printWindow.onafterprint = function() {
                        printWindow.close();
                        window.URL.revokeObjectURL(url);
                    };
                }, 500);
            };
        })
        .catch(error => {
            console.error('Error:', error);
            alert('印刷の実行に失敗しました。');
        });
}

/**
 * フォームバリデーション
 */
function validateForm() {
    const shiteiNo = document.querySelector('input[name="shiteiNo"]').value;

    if (!shiteiNo) {
        alert('指定番号が取得できません。');
        return false;
    }

    return true;
}