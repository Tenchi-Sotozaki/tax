document.addEventListener('DOMContentLoaded', function () {

    function calcMonth(monthIndex) {
        let totalCount = 0;
        let totalTax   = 0;
        document.querySelectorAll('.tier-count[data-month="' + monthIndex + '"]').forEach(function (input) {
            const count = parseInt(input.value) || 0;
            const rate  = parseInt(input.dataset.rate) || 0;
            const tax   = count * rate;
            const tier  = input.name.match(/guestCountTier(\d)/)?.[1];
            if (tier) {
                const disp = document.getElementById('taxDisplay_' + monthIndex + '_' + tier);
                if (disp) disp.textContent = tax.toLocaleString();
                const hidden = document.querySelector('input[name="months[' + monthIndex + '].taxAmountTier' + tier + ']"]');
                if (hidden) hidden.value = tax;
            }
            totalCount += count;
            totalTax   += tax;
        });
        const exempt = parseInt(document.querySelector('.exempt-count[data-month="' + monthIndex + '"]')?.value) || 0;
        totalCount += exempt;
        const tc = document.getElementById('totalCount_' + monthIndex);
        const tt = document.getElementById('totalTax_'   + monthIndex);
        if (tc) tc.textContent = totalCount.toLocaleString();
        if (tt) tt.textContent = totalTax.toLocaleString();
    }

    document.querySelectorAll('.tier-count').forEach(function (input) {
        input.addEventListener('input', function () {
            if (parseInt(this.value) < 0) this.value = 0;
            calcMonth(parseInt(this.dataset.month));
        });
    });

    document.querySelectorAll('.exempt-count').forEach(function (input) {
        input.addEventListener('input', function () {
            if (parseInt(this.value) < 0) this.value = 0;
            calcMonth(parseInt(this.dataset.month));
        });
    });

    // 更正請求エリアの表示切替
    function toggleCorrectionArea() {
        const checked = document.getElementById('isCorrection').checked;
        document.getElementById('correctionReasonArea').style.display = checked ? 'block' : 'none';
    }
    document.getElementById('isCorrection').addEventListener('change', toggleCorrectionArea);
    toggleCorrectionArea();

    // 初期計算
    for (let i = 0; i < 3; i++) calcMonth(i);

    // PDF URL 生成
    function buildPdfUrl() {
        const obligorId = document.querySelector('input[name="obligorId"]')?.value ?? '';
        return '/accommodation-tax/declaration/pdf/' + obligorId;
    }

    document.getElementById('btnPreview').addEventListener('click', function () {
        window.open(buildPdfUrl(), '_blank');
    });

    document.getElementById('btnPrint').addEventListener('click', function () {
        const frame = document.getElementById('printFrame');
        frame.onload = function () {
            frame.contentWindow.focus();
            frame.contentWindow.print();
        };
        frame.src = buildPdfUrl();
    });
});
