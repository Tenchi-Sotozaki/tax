(() => {
    const selected = new Set(
        (INITIAL_HOLIDAYS || []).map(d => String(d))
    );

    const nen = parseInt(NEN, 10) || new Date().getFullYear();
    // 表示開始月（1,5,9のいずれか）
    let startMonth = 1;

    function toKey(y, m, d) {
        return `${y}${String(m).padStart(2, '0')}${String(d).padStart(2, '0')}`;
    }

    function toDisp(key) {
        return `${key.slice(0, 4)}/${key.slice(4, 6)}/${key.slice(6, 8)}`;
    }

    function renderCalendar() {
        const grid = document.getElementById('calendarGrid');
        const title = document.getElementById('calendarTitle');
        const endMonth = startMonth + 3;

        title.textContent = `${startMonth}月 〜 ${endMonth}月`;

        document.getElementById('prevQuarter').disabled = startMonth <= 1;
        document.getElementById('nextQuarter').disabled = endMonth >= 12;

        grid.innerHTML = '';
        for (let m = startMonth; m <= endMonth; m++) {
            grid.appendChild(buildMonthCalendar(nen, m));
        }
    }

    function buildMonthCalendar(y, m) {
        const wrapper = document.createElement('div');
        wrapper.style.cssText = 'min-width:0;overflow-x:auto;';

        // 月タイトル
        const monthTitle = document.createElement('div');
        monthTitle.className = 'fw-bold text-center mb-2';
        monthTitle.textContent = `${y}年${m}月`;
        wrapper.appendChild(monthTitle);

        const table = document.createElement('table');
        table.style.cssText = 'border-collapse:separate;border-spacing:4px;width:100%;table-layout:fixed;';

        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');
        ['日', '月', '火', '水', '木', '金', '土'].forEach((label, i) => {
            const th = document.createElement('th');
            th.style.cssText = 'text-align:center;font-size:0.8rem;padding:2px;';
            th.textContent = label;
            if (i === 0) th.classList.add('text-danger');
            if (i === 6) th.classList.add('text-primary');
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);

        const tbody = document.createElement('tbody');
        const firstDow = new Date(y, m - 1, 1).getDay();
        const daysInMonth = new Date(y, m, 0).getDate();

        let tr = document.createElement('tr');
        for (let i = 0; i < firstDow; i++) {
            tr.appendChild(document.createElement('td'));
        }

        for (let d = 1; d <= daysInMonth; d++) {
            const key = toKey(y, m, d);
            const dow = new Date(y, m - 1, d).getDay();

            const td = document.createElement('td');
            td.style.cssText = 'text-align:center;padding:2px;';

            const btn = document.createElement('button');
            btn.type = 'button';
            btn.style.cssText = 'width:100%;height:36px;font-size:0.85rem;border-radius:4px;border:1px solid #dee2e6;';
            btn.textContent = d;

            if (dow === 0) {
                btn.classList.add('btn', 'btn-outline-danger');
                btn.disabled = true;
            } else if (dow === 6) {
                btn.classList.add('btn', 'btn-outline-primary');
                btn.disabled = true;
            } else if (selected.has(key)) {
                btn.classList.add('btn', 'btn-warning');
            } else {
                btn.classList.add('btn', 'btn-outline-secondary');
            }

            if (IS_EDIT && dow !== 0 && dow !== 6) {
                btn.addEventListener('click', () => toggleDay(key, btn));
            } else if (!IS_EDIT && dow !== 0 && dow !== 6) {
                btn.disabled = true;
            }

            td.appendChild(btn);
            tr.appendChild(td);

            if (dow === 6 || d === daysInMonth) {
                if (d === daysInMonth && dow !== 6) {
                    for (let i = dow + 1; i <= 6; i++) {
                        tr.appendChild(document.createElement('td'));
                    }
                }
                tbody.appendChild(tr);
                tr = document.createElement('tr');
            }
        }

        table.appendChild(tbody);
        wrapper.appendChild(table);
        return wrapper;
    }

    function toggleDay(key, btn) {
        if (selected.has(key)) {
            selected.delete(key);
            btn.classList.remove('btn-warning');
            btn.classList.add('btn-outline-secondary');
        } else {
            selected.add(key);
            btn.classList.remove('btn-outline-secondary');
            btn.classList.add('btn-warning');
        }
        renderList();
        renderHiddenInputs();
    }

    function renderList() {
        const list = document.getElementById('holidayList');
        list.innerHTML = '';
        const sorted = [...selected].filter(k => {
            const dow = new Date(parseInt(k.slice(0, 4)), parseInt(k.slice(4, 6)) - 1, parseInt(k.slice(6, 8))).getDay();
            return dow !== 0 && dow !== 6;
        }).sort();

        if (sorted.length === 0) {
            list.innerHTML = '<span class="text-muted small">休業日が設定されていません。</span>';
            return;
        }
        sorted.forEach(key => {
            const badge = document.createElement('span');
            badge.className = 'badge bg-warning text-dark fs-6 holiday-badge';
            badge.dataset.dt = key;
            badge.textContent = toDisp(key);
            list.appendChild(badge);
        });
    }

    function renderHiddenInputs() {
        const container = document.getElementById('hiddenInputs');
        container.innerHTML = '';
        const sorted = [...selected].filter(k => {
            const dow = new Date(parseInt(k.slice(0, 4)), parseInt(k.slice(4, 6)) - 1, parseInt(k.slice(6, 8))).getDay();
            return dow !== 0 && dow !== 6;
        }).sort();
        sorted.forEach(key => {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = 'holidayDts';
            input.value = key;
            container.appendChild(input);
        });
    }

    window.initHolidays = function () {
        const modal = bootstrap.Modal.getInstance(document.getElementById('initConfirmModal'));
        if (modal) modal.hide();
        fetch(INIT_URL + NEN)
            .then(r => { if (!r.ok) throw new Error(r.status); return r.json(); })
            .then(dates => {
                console.log('init dates:', dates);
                selected.clear();
                (dates || []).forEach(d => selected.add(String(d)));
                renderCalendar();
                renderList();
                renderHiddenInputs();
            })
            .catch(e => console.error('init error:', e));
    };

    document.getElementById('initConfirmBtn')?.addEventListener('click', () => initHolidays());

    window.changeNen = function (sel) {
        const baseUrl = sel.dataset.baseUrl;
        const mode = sel.dataset.mode;
        const url = mode === 'edit'
            ? baseUrl.replace('/view/', '/edit/') + sel.value
            : baseUrl + sel.value;
        window.location.href = url;
    };

    // 年セレクタを当年±3年で生成
    (function buildNenSelect() {
        const sel = document.getElementById('nenSelect');
        const currentYear = new Date().getFullYear();
        for (let y = currentYear - 3; y <= currentYear + 3; y++) {
            const opt = document.createElement('option');
            opt.value = y;
            opt.textContent = y + '年';
            if (String(y) === String(nen)) opt.selected = true;
            sel.appendChild(opt);
        }
    })();

    document.getElementById('prevQuarter').addEventListener('click', () => {
        startMonth -= 4;
        if (startMonth < 1) startMonth = 1;
        renderCalendar();
    });

    document.getElementById('nextQuarter').addEventListener('click', () => {
        startMonth += 4;
        if (startMonth > 9) startMonth = 9;
        renderCalendar();
    });

    renderCalendar();
    renderList();
    renderHiddenInputs();
})();
