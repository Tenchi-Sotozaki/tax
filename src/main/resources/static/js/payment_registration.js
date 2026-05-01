function calculateTotals() {
    const nights1 = parseInt(document.getElementById('taxableNights1').value) || 0;
    const nights2 = parseInt(document.getElementById('taxableNights2').value) || 0;
    const nights3 = parseInt(document.getElementById('taxableNights3').value) || 0;
    const exempt = parseInt(document.getElementById('exemptNights').value) || 0;

    const tax1 = parseInt(document.getElementById('taxAmount1').value) || 0;
    const tax2 = parseInt(document.getElementById('taxAmount2').value) || 0;
    const tax3 = parseInt(document.getElementById('taxAmount3').value) || 0;

    const totalNights = nights1 + nights2 + nights3 + exempt;
    document.getElementById('totalNights').value = totalNights;

    const totalAmount = (nights1 * tax1) + (nights2 * tax2) + (nights3 * tax3);
    document.getElementById('totalPaymentAmount').value = totalAmount;
}

document.addEventListener('DOMContentLoaded', function () {
    ['taxableNights1', 'taxableNights2', 'taxableNights3', 'exemptNights'].forEach(id => {
        document.getElementById(id).addEventListener('input', calculateTotals);
    });

    document.getElementById('paymentForm').addEventListener('submit', function (e) {
        e.preventDefault();
        alert('納入情報を登録します。');
    });

    document.querySelector('[data-bs-target="#monthlyReportModal"]').addEventListener('click', function () {
        console.log('月計表登録モーダルを開きます。');
    });

    calculateTotals();
});
