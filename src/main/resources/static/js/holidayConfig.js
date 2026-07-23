(() => {
    // 選択中の休業日セット（yyyyMMdd形式）
    const selected = new Set(
        (INITIAL_HOLIDAYS || []).map(d => String(d))
    );

    const nendo = parseInt(NENDO, 10) || new Date().getFullYear();
    // 年度開始月を4月とする
    let currentYear = nendo;
    let currentMonth = 4; // 4月スタート

    function toKey(y, m, d) {
        return `${y}${String(m).padStart(2, '0')}${String(d).padStart(2, '0')}`;
    }

    function toDisp(key) {
        return `${key.slice(0, 4)}/${key.slice(4, 6)}/${key.slice(6, 8)}`;
    }

    function isWeekend(y, m, d) {
        const dow = new Date(y, m - 1, d).getDay();
        return dow === 0 || dow === 6;
    }

    function renderCalendar() {
        const title = document.getElementById('calendarTitle');
        const grid = document.getElementById('calendarGrid');
        title.textContent = `${currentYear}年${currentMonth}月`;
        grid.innerHTML = '';

        const table = document.createElement('table');
        table.style.cssText = 'border-collapse:separate;border-spacing:4px;';

        // 曜日ヘッダー行
        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');
        ['日', '月', '火', '水', '木', '金', '土'].forEach((label, i) => {
            const th = document.createElement('th');
            th.style.cssText = 'width:44px;text-align:center;font-size:0.8rem;padding:2px;';
            th.textContent = label;
            if (i === 0) th.classList.add('text-danger');
            if (i === 6) th.classList.add('text-primary');
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);

        const tbody = document.createElement('tbody');
        const firstDow = new Date(currentYear, currentMonth - 1, 1).getDay();
        const daysInMonth = new Date(currentYear, currentMonth, 0).getDate();

        let tr = document.createElement('tr');

        // 月初の空白セル
        for (let i = 0; i < firstDow; i++) {
            tr.appendChild(document.createElement('td'));
        }

        for (let d = 1; d <= daysInMonth; d++) {
            const key = toKey(currentYear, currentMonth, d);
            const dow = new Date(currentYear, currentMonth - 1, d).getDay();

            const td = document.createElement('td');
            td.style.cssText = 'text-align:center;padding:2px;';

            const btn = document.createElement('button');
            btn.type = 'button';
            btn.style.cssText = 'width:40px;height:36px;font-size:0.85rem;border-radius:4px;border:1px solid #dee2e6;white-space:nowrap;min-width:40px;';
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

            // 土曜または月末で行を閉じる
            if (dow === 6 || d === daysInMonth) {
                // 月末が土曜以外なら残りを空白で埋める
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
        grid.appendChild(table);
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
            const dow = new Date(parseInt(k.slice(0,4)), parseInt(k.slice(4,6))-1, parseInt(k.slice(6,8))).getDay();
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
            const dow = new Date(parseInt(k.slice(0,4)), parseInt(k.slice(4,6))-1, parseInt(k.slice(6,8))).getDay();
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
        if (!confirm('祝日設定を初期化します。よろしいですか？')) return;
        selected.clear();
        renderCalendar();
        renderList();
        renderHiddenInputs();
    };

    function changeNendo(sel) {
        const base = sel.dataset.baseUrl;
        window.location.href = base + sel.value;
    }
    window.changeNendo = changeNendo;

    document.getElementById('prevMonth').addEventListener('click', () => {
        currentMonth--;
        if (currentMonth < 1) { currentMonth = 12; currentYear--; }
        renderCalendar();
    });

    document.getElementById('nextMonth').addEventListener('click', () => {
        currentMonth++;
        if (currentMonth > 12) { currentMonth = 1; currentYear++; }
        renderCalendar();
    });

    renderCalendar();
    renderList();
    renderHiddenInputs();
})();
