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

    go('btnReportTokugimuShiteiTsuchi',         '/accommodation-tax/tokugimu/report/tokugimuShiteiTsuchi');
    go('btnReportTokugimuJuriTsuchi',           '/accommodation-tax/tokugimu/report/tokugimuJuriTsuchi');
    go('btnReportNozeiKanrinin',                '/accommodation-tax/reports/nozeiKanrininShoninTsuchi');
    go('btnReportNozeiMenjo',                   '/accommodation-tax/reports/nozeiKanrininNintei');
    go('btnReportTokureiShitei',                '/accommodation-tax/reports/tokureiShitei');
    go('btnReportTokureiTorikeshi',             '/accommodation-tax/reports/tokureiShiteiCancel');
    go('btnReportKanpu',                        '/accommodation-tax/kanpuMenjoTsuchi');
    go('btnReportNonyusho',                     '/accommodation-tax/nonyusho');
    go('btnReportShoreikinKetteiTsuchiShinsei', '/accommodation-tax/reports/kofuKetteiTsuchiShinsei');
    go('btnReportGassan',                       '/accommodation-tax/tokugimu/report/gassan');

    // 宿泊税更正・決定通知書（セッション保存が必要なため別処理）
    document.getElementById('btnReportKosei')?.addEventListener('click', async function() {
        window.location.href = '/accommodation-tax/reports/koseiKetteiTsuchi';
    });
});
