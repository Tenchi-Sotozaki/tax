'use strict';

document.addEventListener('DOMContentLoaded', () => {

    const searchForm = document.getElementById('searchForm');

    document.getElementById('resetBtn')?.addEventListener('click', () => {
        searchForm?.reset();
    });

});
