// アクティブなアコーディオンを自動展開
document.querySelectorAll('.sidebar-link').forEach(link => {
    if (link.classList.contains('text-white') && link.classList.contains('fw-medium')) {
        const collapse = link.closest('.accordion-collapse');
        if (collapse) {
            collapse.classList.add('show');
            collapse.previousElementSibling?.querySelector('.accordion-button')?.classList.remove('collapsed');
        }
    }
});
// モバイル用サイドバートグル
document.getElementById('sidebarToggle')?.addEventListener('click', function () {
    document.getElementById('sidebar').classList.toggle('d-none');
});
