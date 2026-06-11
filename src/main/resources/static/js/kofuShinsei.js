/**
 * 宿泊税特別徴収事務交付金交付申請書 JavaScript
 */

/**
 * 年度変更時のデータ再読み込み
 */
function loadReportData() {
    const shiteiNo = document.querySelector('input[name="shiteiNo"]').value;
    const nendo = document.getElementById('nendoInput').value;
    
    if (!shiteiNo || !nendo) {
        return;
    }
    
    // 年度のバリデーション
    if (isNaN(nendo) || nendo.length !== 4) {
        alert('正しい年度を入力してください（例：2024）');
        return;
    }
    
    const formData = new FormData();
    formData.append('shiteiNo', shiteiNo);
    formData.append('nendo', nendo);
    formData.append('_csrf', document.querySelector('input[name="_csrf"]').value);
    
    fetch('/accommodation-tax/reports/kofuShinsei/reload', {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('データの再読み込みに失敗しました');
        }
        return response.json();
    })
    .then(data => {
        // フォームにデータを設定
        updateFormData(data);
    })
    .catch(error => {
        console.error('Error:', error);
        alert('データの取得に失敗しました。');
    });
}

/**
 * フォームデータを更新
 */
function updateFormData(data) {
    document.querySelector('input[name="tokuName"]').value = data.tokuName || '';
    document.querySelector('input[name="shisetsuName"]').value = data.shisetsuName || '';
    document.querySelector('input[name="shisetsuJusho"]').value = data.shisetsuJusho || '';
    
    document.querySelector('input[name="cityName"]').value = data.cityName || '';
    document.querySelector('input[name="jorei"]').value = data.jorei || '';
    document.querySelector('input[name="hakkoYoshiki"]').value = data.hakkoYoshiki || '';
    document.querySelector('input[name="nonyugaku"]').value = data.nonyugaku || '0';
    document.querySelector('input[name="kofugaku"]').value = data.kofugaku || '0';
    document.querySelector('input[name="kofuJoken"]').value = data.kofuJoken || '';
}

/**
 * PDF生成
 */
function generatePdf() {
    if (!validateForm()) {
        return;
    }

    const form = document.getElementById('kofuForm');
    form.action = '/accommodation-tax/reports/kofuShinsei/pdf';
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

    const form = document.getElementById('kofuForm');
    const formData = new FormData(form);

    fetch('/accommodation-tax/reports/kofuShinsei/preview', {
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

    const form = document.getElementById('kofuForm');
    const formData = new FormData(form);

    fetch('/accommodation-tax/reports/kofuShinsei/print', {
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