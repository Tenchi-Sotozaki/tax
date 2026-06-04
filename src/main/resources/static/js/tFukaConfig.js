document.addEventListener('DOMContentLoaded', function () {

    const fukaKbnEl = document.getElementById('fukaKbnHidden');
    const fukaKbn = fukaKbnEl ? fukaKbnEl.value : '';

    const triggerFlag = document.getElementById('modalTriggerFlag');
    if (triggerFlag && triggerFlag.value === 'true') {
        const modalElement = document.getElementById('taxWarningModal');
        if (typeof bootstrap !== 'undefined' && modalElement) {
            new bootstrap.Modal(modalElement).show();
        } else {
            alert("金額のズレを検知しました。「そのまま保存する」を押すと登録を続行します。");
        }
    }

    const btnForceSave = document.getElementById('btnForceSave');
    const taxCheckBypassed = document.getElementById('taxCheckBypassed');
    if (btnForceSave && taxCheckBypassed) {
        btnForceSave.addEventListener('click', function (e) {
            e.preventDefault(); 
            taxCheckBypassed.value = 'true';
            const form = this.closest('form') || document.getElementById('fukaDeclarationForm');
            if (form) form.submit();
        });
    }

    const monthlyTallyModal = document.getElementById('monthlyTallyModal');
    if (monthlyTallyModal) {
        monthlyTallyModal.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                const btnApplyTally = document.getElementById('btnApplyTally');
                if (btnApplyTally) btnApplyTally.click();
            }
        });
    }

    const modCategorySelect = document.getElementById('modificationCategory');
    const regDateLabel = document.getElementById('registrationDateLabel');
    if (modCategorySelect && regDateLabel) {
        const updateDateLabel = () => {
            const val = modCategorySelect.value;
            regDateLabel.textContent = (val === '1') ? '更生年月日' : (val === '2') ? '修正年月日' : '登録日';
        };
        updateDateLabel();
        modCategorySelect.addEventListener('change', updateDateLabel);
    }

    const parseIntSafe = (val) => {
        const num = parseInt(val, 10);
        return isNaN(num) ? 0 : num;
    };

    // 【定額制】の計算
    const tableTeigaku = document.getElementById('monthlyTallyTableTeigaku');
    const calculateTeigaku = () => {
        if (!tableTeigaku) return;
        const rows = tableTeigaku.querySelectorAll('tbody tr');
        let columnTotals = [];
        let totalExempt = 0;

        rows.forEach(row => {
            let rowSum = 0;
            const seqInputs = row.querySelectorAll('input[class*="seq"]');
            seqInputs.forEach((input, index) => {
                const val = parseIntSafe(input.value);
                rowSum += val;
                if (columnTotals[index] === undefined) columnTotals[index] = 0;
                columnTotals[index] += val;
            });
            const exemptInput = row.querySelector('.exempt');
            if (exemptInput) totalExempt += parseIntSafe(exemptInput.value);
            const rowTotalInput = row.querySelector('.row-total');
            if (rowTotalInput) rowTotalInput.value = rowSum;
        });

        let totalAllCategories = 0;
        let totalTaxAmount = 0;
        columnTotals.forEach((total, index) => {
            const totalCountInput = document.getElementById(`modal-total-count-${index}`);
            if (totalCountInput) totalCountInput.value = total;
            totalAllCategories += total;

            const rateInput = document.getElementById(`modal-tax-rate-${index}`);
            const taxAmountInput = document.getElementById(`modal-tax-amount-${index}`);
            if (rateInput && taxAmountInput) {
                const rate = parseIntSafe(rateInput.value);
                const taxAmount = total * rate;
                taxAmountInput.value = taxAmount;
                totalTaxAmount += taxAmount;
            }
        });

        const modalTotalExempt = document.getElementById('modal-total-exempt');
        if (modalTotalExempt) modalTotalExempt.value = totalExempt;

        const modalTotalAll = document.getElementById('modal-total-all');
        if (modalTotalAll) modalTotalAll.value = totalAllCategories + totalExempt;

        const modalTotalTax = document.getElementById('modal-total-tax');
        if (modalTotalTax) modalTotalTax.value = totalTaxAmount;
    };

    // 【定率制】の計算（複数区分・動的ループ対応）
    const tableTeiritsu = document.getElementById('monthlyTallyTableTeiritsu');
    const calculateTeiritsu = () => {
        if (!tableTeiritsu) return;
        const rows = tableTeiritsu.querySelectorAll('tbody tr');
        
        // 隠し税率項目の数から、何区分（何列）あるかを動的に取得
        const rateInputs = tableTeiritsu.querySelectorAll('input[id^="modal-teiritsu-tax-rate-"]');
        const tierCount = rateInputs.length;
        
        let columnTotals = {
            hakusu: Array(tierCount).fill(0),
            ryokin: Array(tierCount).fill(0)
        };
        let totalMenjoHakusu = 0;
        let totalMenjoRyokin = 0;

        rows.forEach(row => {
            for (let i = 0; i < tierCount; i++) {
                const hakusuInput = row.querySelector(`.teiritsu-kazei-hakusu-${i}`);
                if (hakusuInput) columnTotals.hakusu[i] += parseIntSafe(hakusuInput.value);

                const ryokinInput = row.querySelector(`.teiritsu-kazei-ryokin-${i}`);
                if (ryokinInput) columnTotals.ryokin[i] += parseIntSafe(ryokinInput.value);
            }
            const menjoHakusu = row.querySelector('.teiritsu-menjo-hakusu');
            if (menjoHakusu) totalMenjoHakusu += parseIntSafe(menjoHakusu.value);

            const menjoRyokin = row.querySelector('.teiritsu-menjo-ryokin');
            if (menjoRyokin) totalMenjoRyokin += parseIntSafe(menjoRyokin.value);
        });

        // 合計と税額の計算反映
        for (let i = 0; i < tierCount; i++) {
            const totalHakusuInput = document.getElementById(`modal-teiritsu-total-kazei-hakusu-${i}`);
            if (totalHakusuInput) totalHakusuInput.value = columnTotals.hakusu[i];

            const totalRyokinInput = document.getElementById(`modal-teiritsu-total-kazei-ryokin-${i}`);
            if (totalRyokinInput) totalRyokinInput.value = columnTotals.ryokin[i];

            const rateInput = document.getElementById(`modal-teiritsu-tax-rate-${i}`);
            const taxAmountInput = document.getElementById(`modal-teiritsu-total-tax-${i}`);

            if (rateInput && taxAmountInput) {
                const ratePercent = parseFloat(rateInput.value) || 0;
                // 定率計算：料金合計 × (税率/100) の切り捨て
                taxAmountInput.value = Math.floor(columnTotals.ryokin[i] * (ratePercent / 100));
            }
        }

        const totalMenjoHakusuInput = document.getElementById('modal-teiritsu-total-menjo-hakusu');
        if (totalMenjoHakusuInput) totalMenjoHakusuInput.value = totalMenjoHakusu;

        const totalMenjoRyokinInput = document.getElementById('modal-teiritsu-total-menjo-ryokin');
        if (totalMenjoRyokinInput) totalMenjoRyokinInput.value = totalMenjoRyokin;
    };

    // イベントバインド
    if (tableTeigaku) {
        tableTeigaku.querySelector('tbody').addEventListener('input', function (e) {
            if (e.target && e.target.tagName === 'INPUT') calculateTeigaku();
        });
    }
    if (tableTeiritsu) {
        tableTeiritsu.querySelector('tbody').addEventListener('input', function (e) {
            if (e.target && e.target.tagName === 'INPUT') calculateTeiritsu();
        });
    }

    if (monthlyTallyModal) {
        monthlyTallyModal.addEventListener('shown.bs.modal', function () {
            if (fukaKbn === '1') calculateTeigaku();
            else if (fukaKbn === '2') calculateTeiritsu();
        });
    }
});