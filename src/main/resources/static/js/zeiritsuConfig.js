document.addEventListener('DOMContentLoaded', function () {
    const isView = document.getElementById('zeiritsuConfigRoot').dataset.isView === 'true';

    const displaySt = document.querySelector('input[name="tekiyoStYmDisplay"]');
    const hiddenSt = document.getElementById('tekiyoStYmHidden');
    const displayEd = document.querySelector('input[name="tekiyoEdYmDisplay"]');
    const hiddenEd = document.getElementById('tekiyoEdYmHidden');

    if (hiddenSt && hiddenSt.value && hiddenSt.value.length === 6) {
        displaySt.value = hiddenSt.value.substring(0, 4) + '-' + hiddenSt.value.substring(4, 6);
    }
    if (hiddenEd && hiddenEd.value && hiddenEd.value.length === 6) {
        displayEd.value = hiddenEd.value.substring(0, 4) + '-' + hiddenEd.value.substring(4, 6);
    }

    if (displaySt && !isView) {
        displaySt.addEventListener('change', function () {
            hiddenSt.value = displaySt.value ? displaySt.value.replace('-', '') : '';
        });
    }
    if (displayEd && !isView) {
        displayEd.addEventListener('change', function () {
            hiddenEd.value = displayEd.value ? displayEd.value.replace('-', '') : '';
        });
    }

    const zeiValueLabel = document.getElementById('zeiValueLabel');
    const conditionLabel = document.getElementById('conditionLabel');
    const zeiValueHint = document.getElementById('zeiValueHint');

    function updateLabel() {
        const checked = document.querySelector('input[name="fukaKbn"]:checked');
        const isTeiritsu = checked && checked.value === '2';
        zeiValueLabel.textContent = (checked && checked.value === '1') ? '税額' : '税率';
        conditionLabel.textContent = isTeiritsu ? '区分名' : '条件';
        zeiValueHint.classList.toggle('d-none', !isTeiritsu);
        document.querySelectorAll('.teigaku-condition').forEach(el => el.style.display = isTeiritsu ? 'none' : '');
        document.querySelectorAll('.teiritsu-condition').forEach(el => el.style.display = isTeiritsu ? '' : 'none');
    }

    document.querySelectorAll('input[name="fukaKbn"]').forEach(r => r.addEventListener('change', updateLabel));
    updateLabel();
});
