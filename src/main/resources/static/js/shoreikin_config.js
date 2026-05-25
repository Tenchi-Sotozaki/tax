/**
 * 特別徴収事務交付金照会／登録／編集画面用JavaScript
 */

function editMode() {
    document.getElementById('editForm').submit();
}

function updateShoreikin() {
    // 現在の入力値を更新フォームに反映
    const form = document.getElementById('updateForm');
    const kofuZeigaku = document.getElementById('kofuZeigaku').value;
    const kofuRitsu = document.getElementById('kofuRitsu').value;
    const kofuGaku = document.getElementById('kofuGaku').value;
    const kofuYmd = document.getElementById('kofuYmd').value;

    form.querySelector('input[name="kofuZeigaku"]').value = kofuZeigaku;
    form.querySelector('input[name="kofuRitsu"]').value = kofuRitsu;
    form.querySelector('input[name="kofuGaku"]').value = kofuGaku;
    form.querySelector('input[name="kofuYmd"]').value = kofuYmd;

    if (confirm('交付金情報を更新しますか？')) {
        form.submit();
    }
}

function calculateShoreikin() {
    // 現在の入力値を算出フォームに反映
    const form = document.getElementById('calculateForm');
    const nendo = document.getElementById('nendo').value;
    const kofuRitsu = document.getElementById('kofuRitsu').value;
    const kofuYmd = document.getElementById('kofuYmd').value;

    form.querySelector('input[name="nendo"]').value = nendo;
    form.querySelector('input[name="kofuRitsu"]').value = kofuRitsu;
    form.querySelector('input[name="kofuYmd"]').value = kofuYmd;

    form.submit();
}