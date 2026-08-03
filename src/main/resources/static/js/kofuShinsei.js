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
    
    const formData = new FormData();
    formData.append('shiteiNo', shiteiNo);
    formData.append('nendo', nendo); // yyyy-MM形式で送信
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