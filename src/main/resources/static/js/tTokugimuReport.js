/**
 * 特別徴収義務者帳票出力 JavaScript
 */

// 指定番号をグローバル変数として保持
let shiteiNo = '';

/**
 * DOM読み込み完了後の初期化処理
 */
document.addEventListener('DOMContentLoaded', function() {
    // Thymeleafから渡された指定番号を取得
    const shiteiNoElement = document.getElementById('shiteiNoData');
    if (shiteiNoElement) {
        shiteiNo = shiteiNoElement.textContent || shiteiNoElement.innerText || '';
    }

    console.log('ページ読み込み完了。指定番号:', shiteiNo);

    // 特別徴収義務者指定通知書ボタンのクリックイベント
    const btnTokugimuShitei = document.getElementById('btnReportTokugimuShiteiTsuchi');
    if (btnTokugimuShitei) {
        console.log('特別徴収義務者指定通知書ボタンが見つかりました。');
        btnTokugimuShitei.addEventListener('click', function() {
            console.log('特別徴収義務者指定通知書ボタンがクリックされました。指定番号:', shiteiNo);
            if (shiteiNo) {
                const url = '/accommodation-tax/reports/tokugimuShiteiTsuchi?shiteiNo=' + encodeURIComponent(shiteiNo);
                console.log('開くURL:', url);
                window.location.href = url;
            } else {
                alert('指定番号が取得できませんでした。');
            }
        });
    }

    // 特別徴収義務者申請受理通知書ボタンのクリックイベント
    const btnTokugimuJuri = document.getElementById('btnReportTokugimuJuriTsuchi');
    if (btnTokugimuJuri) {
        console.log('特別徴収義務者申請受理通知書ボタンが見つかりました。');
        btnTokugimuJuri.addEventListener('click', function() {
            console.log('特別徴収義務者申請受理通知書ボタンがクリックされました。指定番号:', shiteiNo);
            if (shiteiNo) {
                const url = '/accommodation-tax/reports/tokugimuJuriTsuchi?shiteiNo=' + encodeURIComponent(shiteiNo);
                console.log('開くURL:', url);
                window.location.href = url;
            } else {
                alert('指定番号が取得できませんでした。');
            }
        });
    }

    // 納税管理人承認(不承認)通知書ボタンのクリックイベント
    const btnNozeiKanrinin = document.getElementById('btnReportNozeiKanrinin');
    if (btnNozeiKanrinin) {
        console.log('納税管理人承認通知書ボタンが見つかりました。');
        btnNozeiKanrinin.addEventListener('click', function() {
            console.log('納税管理人承認通知書ボタンがクリックされました。指定番号:', shiteiNo);
            if (shiteiNo) {
                // TODO: 納税管理人登録チェック機能を有効にする場合は以下のコメントアウトを外す
                // checkNozeiKanrininRegistration(shiteiNo);

                // 一時的に直接画面遷移を行う
                const url = '/accommodation-tax/reports/nozeiKanrininShoninTsuchi?shiteiNo=' + encodeURIComponent(shiteiNo);
                console.log('開くURL:', url);
                window.location.href = url;
            } else {
                alert('指定番号が取得できませんでした。');
            }
        });
    }

    // 納税管理人選任免除認定（不認定）通知書ボタンのクリックイベント
    const btnNozeiMenjo = document.getElementById('btnReportNozeiMenjo');
    if (btnNozeiMenjo) {
        btnNozeiMenjo.addEventListener('click', function() {
            if (shiteiNo) {
                const url = '/accommodation-tax/reports/nozeiKanrininNintei?shiteiNo=' + encodeURIComponent(shiteiNo);
                window.location.href = url;
            } else {
                alert('指定番号が取得できませんでした。');
            }
        });
    }

    // 納入申告書の提出期限等の特例適用者指定通知書ボタンのクリックイベント
    const btnTokureiShitei = document.getElementById('btnReportTokureiShitei');
    if (btnTokureiShitei) {
        console.log('納入申告書の提出期限等の特例適用者指定通知書ボタンが見つかりました。');
        btnTokureiShitei.addEventListener('click', function() {
            console.log('納入申告書の提出期限等の特例適用者指定通知書ボタンがクリックされました。指定番号:', shiteiNo);
            if (shiteiNo) {
                const url = '/accommodation-tax/reports/tokureiShitei?shiteiNo=' + encodeURIComponent(shiteiNo);
                window.location.href = url;
            } else {
                alert('指定番号が取得できませんでした。');
            }
        });
    }

    // 納入申告書の提出期限等の特例適用者指定取消通知書ボタンのクリックイベント
    const btnTokureiTorikeshi = document.getElementById('btnReportTokureiTorikeshi');
    if (btnTokureiTorikeshi) {
        btnTokureiTorikeshi.addEventListener('click', function() {
            if (shiteiNo) {
                const url = '/accommodation-tax/reports/tokureiShiteiCancel?shiteiNo=' + encodeURIComponent(shiteiNo);
                window.location.href = url;
            } else {
                alert('指定番号が取得できませんでした。');
            }
        });
    }

    // 宿泊税還付・納入義務の免除決定通知書ボタンのクリックイベント
    const btnKanpu = document.getElementById('btnReportKanpu');
    if (btnKanpu) {
        console.log('宿泊税還付・納入義務の免除決定通知書ボタンが見つかりました。');
        btnKanpu.addEventListener('click', function() {
            console.log('宿泊税還付・納入義務の免除決定通知書ボタンがクリックされました。指定番号:', shiteiNo);
            if (shiteiNo) {
                const url = '/accommodation-tax/kanpuMenjoTsuchi?shiteiNo=' + encodeURIComponent(shiteiNo);
                console.log('開くURL:', url);
                window.location.href = url;
            } else {
                alert('指定番号が取得できませんでした。');
            }
        });
    }

    // 納入書ボタンのクリックイベント
    const btnNonyusho = document.getElementById('btnReportNonyusho');
    if (btnNonyusho) {
        console.log('納入書ボタンが見つかりました。');
        btnNonyusho.addEventListener('click', function() {
            console.log('納入書ボタンがクリックされました。指定番号:', shiteiNo);
            if (shiteiNo) {
                const url = '/accommodation-tax/nonyusho?shiteiNo=' + encodeURIComponent(shiteiNo);
                console.log('開くURL:', url);
                window.location.href = url;
            } else {
                alert('指定番号が取得できませんでした。');
            }
        });
    }

    // 奨励金交付申請書ボタンのクリックイベント
    const btnKofuShinsei = document.getElementById('btnReportShoreikinShinsei');
    if (btnKofuShinsei) {
        console.log('奨励金交付申請書ボタンが見つかりました。');
        btnKofuShinsei.addEventListener('click', function() {
            console.log('奨励金交付申請書ボタンがクリックされました。指定番号:', shiteiNo);
            if (shiteiNo) {
                const url = '/accommodation-tax/reports/kofuShinsei?shiteiNo=' + encodeURIComponent(shiteiNo);
                console.log('開くURL:', url);
                window.location.href = url;
            } else {
                alert('指定番号が取得できませんでした。');
            }
        });
    }

    // 奨励金交付決定通知書ボタンのクリックイベント
    const btnKofuKettei = document.getElementById('btnReportShoreikinKettei');
    if (btnKofuKettei) {
        console.log('奨励金交付決定通知書ボタンが見つかりました。');
        btnKofuKettei.addEventListener('click', function() {
            console.log('奨励金交付決定通知書ボタンがクリックされました。指定番号:', shiteiNo);
            if (shiteiNo) {
                const url = '/accommodation-tax/reports/kofuKetteiTsuchi?shiteiNo=' + encodeURIComponent(shiteiNo);
                console.log('開くURL:', url);
                window.location.href = url;
            } else {
                alert('指定番号が取得できませんでした。');
            }
        });
    }

    // 合算申告納入承認通知書ボタンのクリックイベント
    const btnGassan = document.getElementById('btnReportGassan');
    if (btnGassan) {
        btnGassan.addEventListener('click', function() {
            if (shiteiNo) {
                const url = '/accommodation-tax/reports/gassanNonyuTsuchi?shiteiNo=' + encodeURIComponent(shiteiNo);
                window.location.href = url;
            } else {
                alert('指定番号が取得できませんでした。');
            }
        });
    }
});

/**
 * 納税管理人の登録状況をチェックする関数
 * @param {string} shiteiNo - 指定番号
 * 
 * 使用方法:
 * 1. ボタンクリックイベントで checkNozeiKanrininRegistration(shiteiNo); のコメントアウトを外す
 * 2. 直接画面遷移の部分をコメントアウトする
 */
function checkNozeiKanrininRegistration(shiteiNo) {
    fetch('/accommodation-tax/api/nozeiKanrinin/check?shiteiNo=' + encodeURIComponent(shiteiNo))
        .then(response => response.json())
        .then(data => {
            if (data.isRegistered) {
                // 納税管理人が登録されている場合は画面遷移
                const url = '/accommodation-tax/reports/nozeiKanrininShoninTsuchi?shiteiNo=' + encodeURIComponent(shiteiNo);
                window.location.href = url;
            } else {
                // 納税管理人が登録されていない場合はエラーメッセージ表示
                alert('納税管理人が登録されていません。先に納税管理人の登録を行ってください。');
            }
        })
        .catch(error => {
            console.error('納税管理人登録チェックエラー:', error);
            alert('納税管理人の登録状況を確認できませんでした。');
        });
}
