const appConfig    = document.getElementById('appConfig').dataset;
const ctxPath      = appConfig.ctxPath;
const taishoYmList = JSON.parse(appConfig.taishoYmList).map(String);

function generatePdf() {
    if (!document.getElementById('b1Ym').value) {
        alert('対象月１を選択してください。');
        return;
    }
    const form = document.getElementById('koseiKetteiForm');
    form.action = ctxPath + 'reports/koseiKetteiTsuchi/pdf';
    form.target = '_blank';
    form.submit();
}

function previewPdf() {
    if (!document.getElementById('b1Ym').value) {
        alert('対象月１を選択してください。');
        return;
    }
    const form = document.getElementById('koseiKetteiForm');
    form.action = ctxPath + 'reports/koseiKetteiTsuchi/preview';
    form.target = '_blank';
    form.submit();
}

function print() {
    if (!document.getElementById('b1Ym').value) {
        alert('対象月１を選択してください。');
        return;
    }

    const form     = document.getElementById('koseiKetteiForm');
    const formData = new FormData(form);

    fetch(ctxPath + 'reports/koseiKetteiTsuchi/print', {
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
            const url         = window.URL.createObjectURL(blob);
            const printWindow = window.open(url, '_blank');

            printWindow.onload = function () {
                setTimeout(() => {
                    printWindow.print();
                    printWindow.onafterprint = function () {
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

function addMonths(ym, n) {
    const year  = parseInt(ym.substring(0, 4));
    const month = parseInt(ym.substring(4, 6)) - 1;
    const d     = new Date(year, month + n, 1);
    const m     = String(d.getMonth() + 1).padStart(2, '0');
    return `${d.getFullYear()}${m}`;
}

function toLabel(ym) {
    return `${ym.substring(0, 4)}年${ym.substring(4, 6)}月`;
}

/** b3の表示・値をb2チェック状態と連動して更新 */
function updateB3(b1, b2Checked) {
    const b3Ym        = b1 ? addMonths(b1, 2) : '';
    const b3Exists    = b3Ym && taishoYmList.includes(b3Ym);
    const b3Container = document.getElementById('b3Container');
    const b3Check     = document.getElementById('b3Check');
    const b3Display   = document.getElementById('b3YmDisplay');
    const b3Input     = document.getElementById('b3Ym');

    if (b3Exists && b2Checked) {
        b3Container.style.display = '';
        b3Check.disabled          = false;
        b3Display.textContent     = toLabel(b3Ym);
        if (b3Check.checked) {
            b3Input.value = b3Ym;
        }
    } else {
        b3Container.style.display = 'none';
        b3Check.checked           = false;
        b3Input.value             = '';
    }
}

/** 対象月１変更時 */
document.getElementById('b1Ym').addEventListener('change', function () {
    const b1 = this.value;

    const b2Ym        = b1 ? addMonths(b1, 1) : '';
    const b2Exists    = b2Ym && taishoYmList.includes(b2Ym);
    const b2Container = document.getElementById('b2Container');
    const b2Check     = document.getElementById('b2Check');
    const b2Display   = document.getElementById('b2YmDisplay');
    const b2Input     = document.getElementById('b2Ym');

    if (b2Exists) {
        b2Container.style.display = '';
        b2Check.checked           = true;
        b2Display.textContent     = toLabel(b2Ym);
        b2Input.value             = b2Ym;
    } else {
        b2Container.style.display = 'none';
        b2Check.checked           = false;
        b2Input.value             = '';
    }

    updateB3(b1, b2Exists && b2Check.checked);
});

/** 対象月２チェック変更時 */
document.getElementById('b2Check').addEventListener('change', function () {
    const b1      = document.getElementById('b1Ym').value;
    const b2Ym    = addMonths(b1, 1);
    const b2Input = document.getElementById('b2Ym');

    b2Input.value = this.checked ? b2Ym : '';
    updateB3(b1, this.checked);
});

/** 対象月３チェック変更時 */
document.getElementById('b3Check').addEventListener('change', function () {
    const b1      = document.getElementById('b1Ym').value;
    const b3Ym    = addMonths(b1, 2);
    const b3Input = document.getElementById('b3Ym');

    b3Input.value = this.checked ? b3Ym : '';
});