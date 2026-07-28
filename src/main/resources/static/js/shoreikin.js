/**
 * 特別徴収事務交付金画面用JavaScript
 */

function viewKofu(shiteiNo, nendo) {
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/accommodation-tax/shoreikin/viewKofu';

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    if (csrfToken) {
        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = '_csrf';
        csrfInput.value = csrfToken;
        form.appendChild(csrfInput);
    }

    const shiteiNoInput = document.createElement('input');
    shiteiNoInput.type = 'hidden';
    shiteiNoInput.name = 'selectedItems';
    shiteiNoInput.value = shiteiNo;
    form.appendChild(shiteiNoInput);

    if (nendo && nendo !== 'null') {
        const nendoInput = document.createElement('input');
        nendoInput.type = 'hidden';
        nendoInput.name = 'nendo';
        nendoInput.value = nendo;
        form.appendChild(nendoInput);
    }

    document.body.appendChild(form);
    form.submit();
}

function viewKoza(shiteiNo) {
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/accommodation-tax/shoreikin/viewKoza';

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    if (csrfToken) {
        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = '_csrf';
        csrfInput.value = csrfToken;
        form.appendChild(csrfInput);
    }

    const shiteiNoInput = document.createElement('input');
    shiteiNoInput.type = 'hidden';
    shiteiNoInput.name = 'selectedItems';
    shiteiNoInput.value = shiteiNo;
    form.appendChild(shiteiNoInput);

    document.body.appendChild(form);
    form.submit();
}

document.addEventListener('DOMContentLoaded', function () {
    document.addEventListener('click', function (e) {
        const kofu = e.target.closest('.btn-kofu');
        if (kofu) {
            viewKofu(kofu.dataset.shiteiNo, kofu.dataset.nendo);
            return;
        }
        const koza = e.target.closest('.btn-koza');
        if (koza) {
            viewKoza(koza.dataset.shiteiNo);
        }
    });
});
