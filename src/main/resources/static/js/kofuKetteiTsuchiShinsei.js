/**
 * 宿泊税特別徴収事務交付金交付申請書 JavaScript
 */

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
 * 共通のエラーハンドリング
 */
async function handleResponseError(response, defaultMessage) {
    let errorMessage = defaultMessage;
    try {
		// Controllerから渡されたエラーメッセージを設定
        const text = await response.text();
        if (text) {
            errorMessage = text;
        }
    } catch (e) {
        // パース失敗時はデフォルトメッセージを使用
    }
    throw new Error(errorMessage);
}

/**
 * PDF生成
 */
function generatePdf() {
	
    const ketteiCheckbox = document.querySelector('input[name="ketteiTsuchi"]');
    const shinseiCheckbox = document.querySelector('input[name="shinsei"]');

    const isKetteiChecked = ketteiCheckbox ? ketteiCheckbox.checked : false;
    const isShinseiChecked = shinseiCheckbox ? shinseiCheckbox.checked : false;

    // 両方チェックされていない場合
    if (!isKetteiChecked && !isShinseiChecked) {
        alert('印刷対象が選択されていません。');
        return;
    }
	
    if (!validateForm()) {
        return;
    }

    const form = document.getElementById('kofuForm');
    const formData = new FormData(form);

    fetch('/accommodation-tax/reports/kofuKetteiTsuchiShinsei/pdf', {
        method: 'POST',
        body: formData
    })
        .then(async response => {
            if (!response.ok) {
                await handleResponseError(response, 'PDFの生成に失敗しました');
            }
            return response.blob();
        })
        .then(blob => {
			// ファイル名設定
            let fileName = 'kofu-document.pdf'; // デフォルト
            if (isKetteiChecked && isShinseiChecked) {
                fileName = 'kofu-kettei-tsuchi-shinsei.pdf'; // 両方の場合
            } else if (isKetteiChecked) {
                fileName = 'kofu-kettei-tsuchi.pdf'; // 決定通知書のみの場合
            } else if (isShinseiChecked) {
                fileName = 'kofu-shinsei.pdf'; // 交付申請書のみの場合
            }
			
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = fileName;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            setTimeout(() => window.URL.revokeObjectURL(url), 1000);
        })
        .catch(error => {
            console.error('Error:', error);
            alert(error.message);
        });
}

/**
 * プレビュー
 */
function preview() {
	
    const ketteiCheckbox = document.querySelector('input[name="ketteiTsuchi"]');
    const shinseiCheckbox = document.querySelector('input[name="shinsei"]');

    const isKetteiChecked = ketteiCheckbox ? ketteiCheckbox.checked : false;
    const isShinseiChecked = shinseiCheckbox ? shinseiCheckbox.checked : false;

    // 両方チェックされていない場合
    if (!isKetteiChecked && !isShinseiChecked) {
        alert('印刷対象が選択されていません。');
        return;
    }
	
    if (!validateForm()) {
        return;
    }

    const form = document.getElementById('kofuForm');
    const formData = new FormData(form);

    fetch('/accommodation-tax/reports/kofuKetteiTsuchiShinsei/preview', {
        method: 'POST',
        body: formData
    })
        .then(async response => {
            if (!response.ok) {
                await handleResponseError(response, 'プレビューの生成に失敗しました');
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
            alert(error.message);
        });
}

/**
 * 印刷
 */
function printReport() {
	
    const ketteiCheckbox = document.querySelector('input[name="ketteiTsuchi"]');
    const shinseiCheckbox = document.querySelector('input[name="shinsei"]');

    const isKetteiChecked = ketteiCheckbox ? ketteiCheckbox.checked : false;
    const isShinseiChecked = shinseiCheckbox ? shinseiCheckbox.checked : false;

    // 両方チェックされていない場合
    if (!isKetteiChecked && !isShinseiChecked) {
        alert('印刷対象が選択されていません。');
        return;
    }
	
    if (!validateForm()) {
        return;
    }

    const form = document.getElementById('kofuForm');
    const formData = new FormData(form);

    fetch('/accommodation-tax/reports/kofuKetteiTsuchiShinsei/print', {
        method: 'POST',
        body: formData
    })
        .then(async response => {
            if (!response.ok) {
                await handleResponseError(response, '印刷用PDFの生成に失敗しました');
            }
            return response.blob();
        })
        .then(blob => {
            const url = window.URL.createObjectURL(blob);

            const iframe = document.createElement('iframe');
            iframe.style.display = 'none';
            iframe.src = url;
            document.body.appendChild(iframe);

            iframe.onload = function() {
                setTimeout(() => {
                    try {
                        iframe.contentWindow.print();
                    } catch (e) {
                        console.error('Print error:', e);
                        alert('印刷の実行に失敗しました。');
                    }

                    setTimeout(() => {
                        document.body.removeChild(iframe);
                        window.URL.revokeObjectURL(url);
                    }, 1000);
                }, 500);
            };
        })
        .catch(error => {
            console.error('Error:', error);
            alert(error.message);
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
	
    // 年度（YYYY形式）のバリデーション
    const nendoInput = document.getElementById('nendoInput');
    const nendoValue = nendoInput ? nendoInput.value.trim() : '';

    if (!nendoValue) {
        alert('年度を入力してください。');
        nendoInput.focus();
        return false;
    }

    // 4桁の半角数字であるかチェックする正規表現
    const nendoRegex = /^\d{4}$/;
    if (!nendoRegex.test(nendoValue)) {
        alert('年度は4桁の半角数字（例: 2026）で入力してください。');
        nendoInput.focus();
        return false;
    }
	
    // 発行年月日入力チェック
    const hakkoYmdInput = document.getElementById('hakkoYmdInput');
    const hakkoYmdValue = hakkoYmdInput ? hakkoYmdInput.value.trim() : '';

    if (!hakkoYmdValue) {
        alert('発行年月日を入力してください。');
        hakkoYmdInput.focus();
        return false;
    }

    return true;
}

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
