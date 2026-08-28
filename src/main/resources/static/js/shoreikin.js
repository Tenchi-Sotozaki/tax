/**
 * 特別徴収事務交付金画面用JavaScript
 */

function viewKofu(shiteiNo, shisetsuName, shimei, nendo) {
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

    // 指定番号
    const shiteiNoInput = document.createElement('input');
    shiteiNoInput.type = 'hidden';
    shiteiNoInput.name = 'selectedItems';
    shiteiNoInput.value = shiteiNo;
    form.appendChild(shiteiNoInput);

    // 施設名称
    if (shisetsuName) {
        const shisetsuInput = document.createElement('input');
        shisetsuInput.type = 'hidden';
        shisetsuInput.name = 'shisetsuName';
        shisetsuInput.value = shisetsuName;
        form.appendChild(shisetsuInput);
    }

    // 氏名
    if (shimei) {
        const shimeiInput = document.createElement('input');
        shimeiInput.type = 'hidden';
        shimeiInput.name = 'shimei';
        shimeiInput.value = shimei;
        form.appendChild(shimeiInput);
    }

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

function viewKoza(shiteiNo, shisetsuName, shimei) {
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

	// 指定番号
    const shiteiNoInput = document.createElement('input');
    shiteiNoInput.type = 'hidden';
    shiteiNoInput.name = 'selectedItems';
    shiteiNoInput.value = shiteiNo;
    form.appendChild(shiteiNoInput);

	// 施設名称
    if (shisetsuName) {
        const shisetsuInput = document.createElement('input');
        shisetsuInput.type = 'hidden';
        shisetsuInput.name = 'shisetsuName';
        shisetsuInput.value = shisetsuName;
        form.appendChild(shisetsuInput);
    }

	// 氏名
    if (shimei) {
        const shimeiInput = document.createElement('input');
        shimeiInput.type = 'hidden';
        shimeiInput.name = 'shimei';
        shimeiInput.value = shimei;
        form.appendChild(shimeiInput);
    }

    document.body.appendChild(form);
    form.submit();
}

document.addEventListener('DOMContentLoaded', function () {
    document.addEventListener('click', function(e) {
        const kofu = e.target.closest('.btn-kofu');
        if (kofu) {
            viewKofu(kofu.dataset.shiteiNo, kofu.dataset.shisetsuName, kofu.dataset.shimei, kofu.dataset.nendo);
            return;
        }
        const koza = e.target.closest('.btn-koza');
        if (koza) {
            viewKoza(koza.dataset.shiteiNo, koza.dataset.shisetsuName, koza.dataset.shimei);
        }
    });
});
