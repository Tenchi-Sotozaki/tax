/**
 * 金融機関コード取込 JavaScript
 *
 * 同期処理のため完了までレスポンスを待つ。
 * 二重送信を防ぎ、処理中であることが分かるようにボタンの表示を切り替える。
 */
document.addEventListener('DOMContentLoaded', function () {

    const form = document.getElementById('bankImportForm');
    const button = document.getElementById('importButton');
    const fileInput = document.getElementById('file');

    if (!form || !button || !fileInput) {
        return;
    }

    form.addEventListener('submit', function (event) {
        if (!fileInput.value) {
            event.preventDefault();
            alert('zipファイルを選択してください。');
            return;
        }

        if (!confirm('金融機関マスタ・支店マスタの既存データをすべて置き換えます。よろしいですか。')) {
            event.preventDefault();
            return;
        }

        button.disabled = true;
        button.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> 取込中...';
    });
});
