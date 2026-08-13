document.addEventListener('DOMContentLoaded', function () {
    // 編集モードのみ変更検知を行う
    const form = document.getElementById('configForm');
    if (!form || form.dataset.mode !== 'edit') return;

    function checkValue(input) {
        if (input.type === 'checkbox' || input.type === 'radio') return;
        const initial = input.getAttribute('data-initial-value');
        if (initial === null) return;
        const initialStr = (initial === 'null') ? '' : String(initial).trim();
        const currentStr = String(input.value).trim();
        input.style.border = (currentStr !== initialStr) ? '3px solid #ffeb3b' : '';
    }

    document.querySelectorAll('#configForm .form-control, #configForm .form-select').forEach(function (input) {
        checkValue(input);
        input.addEventListener('input', () => checkValue(input));
        input.addEventListener('change', () => checkValue(input));
        input.addEventListener('blur', () => checkValue(input));
    });
});
