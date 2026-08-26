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
        if (currentStr !== initialStr) {
            input.classList.add('form-control-edited');
        } else {
			input.classList.remove('form-control-edited');
            input.style.border = '';
        }
    }

    document.querySelectorAll('#configForm .form-control, #configForm .form-select').forEach(function (input) {
        checkValue(input);
        input.addEventListener('input', () => checkValue(input));
        input.addEventListener('change', () => checkValue(input));
        input.addEventListener('blur', () => checkValue(input));
    });
});
