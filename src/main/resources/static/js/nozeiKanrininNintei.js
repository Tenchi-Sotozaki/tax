/**
 * 納税管理人選任免除認定（不認定）通知書 JavaScript
 */

function generatePdf() {
    if (!validateForm()) return false;
    const form = document.getElementById('tsuchiForm');
    form.action = '/accommodation-tax/reports/nozeiKanrininNintei/pdf';
    form.target = '_blank';
    form.submit();
}

function preview() {
    if (!validateForm()) return false;
    const form = document.getElementById('tsuchiForm');
    form.action = '/accommodation-tax/reports/nozeiKanrininNintei/preview';
    form.target = '_blank';
    form.submit();
}

function print() {
    if (!validateForm()) return false;
    const form = document.getElementById('tsuchiForm');
    form.action = '/accommodation-tax/reports/nozeiKanrininNintei/print';
    form.target = '_blank';
    form.submit();
}

function validateForm() {
    const hakkoYmd = document.getElementById('hakkoYmd');
    if (!hakkoYmd || !hakkoYmd.value.trim()) {
        alert('発行日を入力してください。');
        if (hakkoYmd) hakkoYmd.focus();
        return false;
    }
    return true;
}
