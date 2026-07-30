/**
 * 納入書発行画面 JavaScript
 */

/**
 * DOM読み込み完了後の初期化処理
 */
document.addEventListener('DOMContentLoaded', function() {
    
    // 年度フィールドの初期化
    initializeNendoField();
    
    // 対象年月の初期値を設定（当月）
    const taishoYmField = document.getElementById('taishoYm');
    if (taishoYmField && !taishoYmField.value) {
        const today = new Date();
        const year = today.getFullYear();
        const month = String(today.getMonth() + 1).padStart(2, '0');
        taishoYmField.value = `${year}-${month}`;
    }
    
    // 延滞金の初期値を設定
    const entaiField = document.getElementById('entai');
    if (entaiField && !entaiField.value) {
        entaiField.value = '0';
    }
    
    // 指定番号フィールドの変更監視（編集可能な場合）
    const shiteiNoField = document.getElementById('shiteiNo');
    if (shiteiNoField && !shiteiNoField.readOnly) {
        shiteiNoField.addEventListener('blur', function() {
            const shiteiNo = this.value.trim();
            if (shiteiNo) {
                loadTokugimuInfo(shiteiNo);
            }
        });
    }
});

/**
 * 年度フィールドの初期化とイベント設定
 */
function initializeNendoField() {
    const nendoField = document.getElementById('nendo');
    const nendoDisplay = document.getElementById('nendoDisplay');
    
    if (!nendoField || !nendoDisplay) return;
    
    // 初期値を設定（現在の年月）
    if (!nendoField.value) {
        const today = new Date();
        const year = today.getFullYear();
        const month = String(today.getMonth() + 1).padStart(2, '0');
        nendoField.value = `${year}-${month}`;
    }
    
    // 初期表示を更新
    updateNendoDisplay();
    
    // 値変更時のイベントリスナー
    nendoField.addEventListener('change', updateNendoDisplay);
    nendoField.addEventListener('input', updateNendoDisplay);
}

/**
 * 年度表示を更新（YYYYのみ表示）
 */
function updateNendoDisplay() {
    const nendoField = document.getElementById('nendo');
    const nendoDisplay = document.getElementById('nendoDisplay');
    
    if (!nendoField || !nendoDisplay) return;
    
    const value = nendoField.value;
    if (value) {
        // YYYY-MMからYYYYのみを抽出
        const year = value.split('-')[0];
        nendoDisplay.textContent = year + '年度';
    } else {
        nendoDisplay.textContent = '年度を選択してください';
    }
}

/**
 * 指定番号に基づいて特別徴収義務者情報を読み込む
 */
function loadTokugimuInfo(shiteiNo) {
    fetch(`/api/tokugimu/info?shiteiNo=${encodeURIComponent(shiteiNo)}`)
        .then(response => {
            if (response.ok) {
                return response.json();
            }
            throw new Error('情報の取得に失敗しました');
        })
        .then(data => {
            // 取得した情報をフォームに設定
            document.getElementById('tokuName').value = data.name || '';
            document.getElementById('tokuJusho').value = data.jusho || '';
        })
        .catch(error => {
            console.error('特別徴収義務者情報取得エラー:', error);
            // エラー時はフィールドをクリア
            document.getElementById('tokuName').value = '';
            document.getElementById('tokuJusho').value = '';
        });
}

// URLパラメータのerrorをチェックしてアラートを出す
window.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const error = urlParams.get('error');

    if (error === 'pdf_not_found') {
        alert('対象データが見つかりませんでした。');
    } else if (error === 'server_error') {
        alert('PDFの生成中にエラーが発生しました。');
    }
});

/**
 * PDF発行・プレビュー処理
 * @param {string} type 'pdf' または 'preview'
 */
async function submitReport(type) {
    
    if (!validateForm()) {
        return;
    }
    
    const formData = await collectFormData();
    const csrfToken = document.querySelector('input[name="_csrf"]').value;
    const endpoint = `/accommodation-tax/nonyusho/${type}`;

    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': csrfToken
            },
            body: JSON.stringify(formData)
        });

        if (!response.ok) {
            throw new Error('対象データが見つかりませんでした');
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);

        if (type === 'preview') {
            // プレビューの場合は別タブで開く
            window.open(url, '_blank');
        } else {
            // PDFダウンロードの場合はファイルをダウンロードさせる
            const a = document.createElement('a');
            a.href = url;
            a.download = 'nonyusho.pdf';
            document.body.appendChild(a);
            a.click();
            a.remove();
        }
    } catch (error) {
        console.error(`${type} エラー:`, error);
        alert('対象データが見つかりませんでした、または処理に失敗しました。');
    }
}

/**
 * 印刷処理
 */
async function printReport() {
    
    if (!validateForm()) {
        return;
    }
    
    const formData = await collectFormData();
    const csrfToken = document.querySelector('input[name="_csrf"]').value;

    fetch('/accommodation-tax/nonyusho/print', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': csrfToken
        },
        body: JSON.stringify(formData)
    })
        .then(response => {
            if (response.ok) {
                return response.blob();
            }
            throw new Error('対象データが見つかりませんでした');
        })
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            const iframe = document.createElement('iframe');
            iframe.style.display = 'none';
            iframe.src = url;
            document.body.appendChild(iframe);
            iframe.onload = function() {
                iframe.contentWindow.print();
            };
        })
        .catch(error => {
            console.error('印刷エラー:', error);
            showErrorMessage('error.message');
        });
}

/**
 * フォームデータ収集
 */
async function collectFormData() {
    const shiteiNo = document.getElementById('shiteiNo')?.value || '';
    const nendoValue = document.getElementById('nendo')?.value || '';
    const taishoYmValue = document.getElementById('taishoYm')?.value || '';
    const entai = document.getElementById('entai')?.value || '0';
    
    // 年度から年のみを抽出（YYYY-MM から YYYY を取得）
    const nendo = nendoValue ? nendoValue.split('-')[0] : '';
    
    // 対象年月をLocalDate形式に変換（YYYY-MM-01）
    const shinkokuYmd = taishoYmValue ? taishoYmValue + '-01' : null;
    
    // 動的にデータを取得
    const dynamicData = await loadDynamicData(shiteiNo, nendo, taishoYmValue);
    
    // 税額と合計額を計算
    const entaiNum = parseInt(entai, 10) || 0;
    const zeigakuNum = parseInt(dynamicData.zeigaku, 10) || 0;
    const kasanNum = parseInt(dynamicData.kasan, 10) || 0;
    const gokei = (zeigakuNum + kasanNum + entaiNum).toString();
    
    return {
        shiteiNo: shiteiNo,
        nendo: nendo,
        shinkokuYmd: shinkokuYmd ? shinkokuYmd : null,
        entai: entai,
        zeigaku: dynamicData.zeigaku,
        kasan: dynamicData.kasan,
        gokei: gokei,
        nokigen: dynamicData.nokigen ? dynamicData.nokigen : null,
        tokuName: document.getElementById('tokuName')?.value || '',
        tokuJusho: document.getElementById('tokuJusho')?.value || '',
        tokuYubinNo: document.getElementById('tokuYubinNo')?.value || '',
        cityName: dynamicData.cityName,
        jichitaiCd: dynamicData.jichitaiCd,
        kozaNo: dynamicData.kozaNo,
        kozaName: dynamicData.cityName,
        nonyuBasho: dynamicData.nonyuBasho,
        shiteiKinyuName: dynamicData.shiteiKinyuName,
        torimatome: dynamicData.torimatome
    };
}

/**
 * 動的データ取得
 */
async function loadDynamicData(shiteiNo, nendo, taishoYmValue) {
    try {
        // パラメーターのバリデーション
        if (!shiteiNo || !nendo) {
            throw new Error('指定番号と年度が必要です');
        }
        
        const url = `/accommodation-tax/nonyusho/data?shiteiNo=${encodeURIComponent(shiteiNo)}&nendo=${encodeURIComponent(nendo)}&shinkokuYm=${encodeURIComponent(taishoYmValue || '')}`;
        
        const response = await fetch(url);
       
        if (!response.ok) {
            const errorText = await response.text();
            console.error('サーバーエラー:', errorText);
            throw new Error(`データの取得に失敗しました (${response.status})`);
        }
        
        const data = await response.json();
       
        // nokigenが空の場合、対象年月の翌月末を設定
        let nokigen = data.nokigen;
        if (!nokigen && taishoYmValue) {
            const taishoDate = new Date(taishoYmValue + '-01');
            taishoDate.setMonth(taishoDate.getMonth() + 2, 0); // 翌月末
            nokigen = taishoDate.toISOString().split('T')[0];
        }
        
        return {
            zeigaku: data.zeigaku || '0',
            kasan: data.kasan || '0',
            nokigen: nokigen || '',
            cityName: data.cityName || '',
            jichitaiCd: data.jichitaiCd || '',
            kozaNo: data.kozaNo || '',
            nonyuBasho: data.nonyuBasho || '',
            shiteiKinyuName: data.shiteiKinyuName || '',
            torimatome: data.torimatome || ''
        };
    } catch (error) {
        console.error('動的データ取得エラー:', error);
        // エラー時はデフォルト値を返す
        let nokigen = '';
        if (taishoYmValue) {
            const taishoDate = new Date(taishoYmValue + '-01');
            taishoDate.setMonth(taishoDate.getMonth() + 2, 0);
            nokigen = taishoDate.toISOString().split('T')[0];
        }
        return {
            zeigaku: '0',
            kasan: '0',
            nokigen: nokigen,
            cityName: '',
            jichitaiCd: '',
            kozaNo: '',
            nonyuBasho: '',
            shiteiKinyuName: '',
            torimatome: ''
        };
    }
}

/**
 * フォームバリデーション
 */
function validateForm() {
    const shiteiNo = document.getElementById('shiteiNo')?.value;
    const nendoValue = document.getElementById('nendo')?.value;
    const taishoYm = document.getElementById('taishoYm')?.value;
    
    if (!shiteiNo) {
        showErrorMessage('指定番号を入力してください。');
        return false;
    }
    
    if (!nendoValue) {
        showErrorMessage('年度を入力してください。');
        return false;
    }
    
    if (!taishoYm) {
        showErrorMessage('対象年月を入力してください。');
        return false;
    }
    
    // 年度の妥当性チェック（YYYY-MM形式から年を抽出）
    const nendo = nendoValue.split('-')[0];
    const nendoNum = parseInt(nendo, 10);
    if (isNaN(nendoNum) || nendoNum < 1900 || nendoNum > 2100) {
        showErrorMessage('年度は1900年から2100年の間で選択してください。');
        return false;
    }
    
    return true;
}

/**
 * エラーメッセージ表示
 */
function showErrorMessage(message) {
    alert(message);
}

/**
 * 成功メッセージ表示
 */
function showSuccessMessage(message) {
}