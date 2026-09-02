function submitForm(actionUrl, openNewTab) {
    const form = document.getElementById('bulkPrintForm');
    const formData = new FormData(form);

    // エラー表示領域
    let errorContainer = document.getElementById('dynamicErrorAlert');
    if (!errorContainer) {
        errorContainer = document.createElement('div');
        errorContainer.id = 'dynamicErrorAlert';
        errorContainer.className = 'alert alert-danger alert-dismissible fade show';
        errorContainer.setAttribute('role', 'alert');
        errorContainer.innerHTML = '<span id="dynamicErrorMessage"></span><button type="button" class="btn-close" data-bs-dismiss="alert"></button>';
        form.prepend(errorContainer);
    }
    errorContainer.style.display = 'none';

    fetch(actionUrl, {
        method: 'POST',
        body: formData
    })
    .then(async response => {
        if (!response.ok) {
            // エラーメッセージ（byte[] / 文字列）を取得
            const errorMessage = await response.text();
            document.getElementById('dynamicErrorMessage').textContent = errorMessage;
            errorContainer.style.display = 'block';
            throw new Error(errorMessage);
        }
        return response.blob();
    })
    .then(blob => {
        if (blob) {
            const url = window.URL.createObjectURL(blob);
            if (openNewTab) {
                window.open(url, '_blank');
            } else {
                const a = document.createElement('a');
                a.href = url;
                a.download = 'kofukin.pdf';
                document.body.appendChild(a);
                a.click();
                a.remove();
            }
        }
    })
    .catch(error => {
        console.error('Error:', error);
    });
}