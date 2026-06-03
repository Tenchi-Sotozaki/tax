/**
 * 納入申告書の提出期限等の特例適用者指定通知書 JavaScript
 */

/**
 * PDF生成
 */
function generatePdf() {
    if (!validateForm()) {
        return;
    }

    const form = document.getElementById('tsuchiForm');
    form.action = '/accommodation-tax/reports/tokureiShitei/pdf';
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

    const form = document.getElementById('tsuchiForm');
    const formData = new FormData(form);

    fetch('/accommodation-tax/reports/tokureiShitei/preview', {
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

    const form = document.getElementById('tsuchiForm');
    const formData = new FormData(form);

    fetch('/accommodation-tax/reports/tokureiShitei/print', {
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
    const hakkoYmd = document.getElementById('hakkoYmd').value;
    const tekiyoYmd = document.getElementById('tekiyoYmd').value;
    const shonin = document.querySelector('input[name="shonin"]:checked');
    const riyu = document.getElementById('riyu').value;

    if (!hakkoYmd) {
        alert('発行日を入力してください。');
        document.getElementById('hakkoYmd').focus();
        return false;
    }

    if (!tekiyoYmd) {
        alert('適用年月日を入力してください。');
        document.getElementById('tekiyoYmd').focus();
        return false;
    }

    if (!shonin) {
        alert('承認を選択してください。');
        return false;
    }

    if (!riyu.trim()) {
        alert('不承認理由を入力してください。');
        document.getElementById('riyu').focus();
        return false;
    }

    return true;
}

document.addEventListener('DOMContentLoaded', function() {
    const hakkoYmdInput = document.getElementById('hakkoYmd');
    if (!hakkoYmdInput.value) {
        const today = new Date();
        const formattedDate = today.getFullYear() + '-' +
            String(today.getMonth() + 1).padStart(2, '0') + '-' +
            String(today.getDate()).padStart(2, '0');
        hakkoYmdInput.value = formattedDate;
    }
});
