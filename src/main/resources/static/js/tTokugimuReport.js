/**
 * 特別徴収義務者帳票出力 JavaScript
 * 指定番号はセッションから取得するため、URLパラメータへの付与は不要
 */

document.addEventListener('DOMContentLoaded', function() {

    function go(btnId, url) {
        document.getElementById(btnId)?.addEventListener('click', function() {
            window.location.href = url;
        });
    }

    go('btnReportTokugimuShiteiTsuchi',         '/accommodation-tax/reports/tokugimuShiteiTsuchi');
    go('btnReportTokugimuJuriTsuchi',           '/accommodation-tax/reports/tokugimuJuriTsuchi');
    go('btnReportNozeiKanrinin',                '/accommodation-tax/reports/nozeiKanrininShoninTsuchi');
    go('btnReportNozeiMenjo',                   '/accommodation-tax/reports/nozeiKanrininNintei');
    go('btnReportTokureiShitei',                '/accommodation-tax/reports/tokureiShitei');
    go('btnReportTokureiTorikeshi',             '/accommodation-tax/reports/tokureiShiteiCancel');
    go('btnReportKanpu',                        '/accommodation-tax/kanpuMenjoTsuchi');
    go('btnReportNonyusho',                     '/accommodation-tax/nonyusho');
    go('btnReportShoreikinKetteiTsuchiShinsei', '/accommodation-tax/reports/kofuKetteiTsuchiShinsei');
    go('btnReportGassan',                       '/accommodation-tax/tokugimu/report/gassan');

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
    const btnkofuKetteiTsuchiShinsei = document.getElementById('btnReportShoreikinKetteiTsuchiShinsei');
    if (btnkofuKetteiTsuchiShinsei) {
        console.log('奨励金決定通知書・交付申請書ボタンが見つかりました。');
        btnkofuKetteiTsuchiShinsei.addEventListener('click', function() {
            console.log('奨励金決定通知書・交付申請書ボタンがクリックされました。指定番号:', shiteiNo);
            if (shiteiNo) {
                const url = '/accommodation-tax/reports/kofuKetteiTsuchiShinsei?shiteiNo=' + encodeURIComponent(shiteiNo);
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
                const url = '/accommodation-tax/tokugimu/report/' + encodeURIComponent(shiteiNo) + '/gassan';
                window.location.href = url;
            } else {
                alert('指定番号が取得できませんでした。');
            }
        });
    }

    // 宿泊税更正・決定通知書ボタンのクリックイベント
    const btnKosei = document.getElementById('btnReportKosei');
    if (btnKosei) {
        btnKosei.addEventListener('click', async function() {
            if (shiteiNo) {
                await selectShiteiGassanByShiteiNo(shiteiNo);
                window.location.href = '/accommodation-tax/reports/koseiKetteiTsuchi';
            } else {
                alert('指定番号が取得できませんでした。');
            }
        });
    }

    // 宿泊税更正・決定通知書（セッション保存が必要なため別処理）
    document.getElementById('btnReportKosei')?.addEventListener('click', async function() {
        window.location.href = '/accommodation-tax/reports/koseiKetteiTsuchi';
    });
});
