let currentMode = 'create';

// モーダルで操作中の権限ID
let currentRoleId = null;

function openRoleModal(mode, roleId) {
    currentMode = mode;
    currentRoleId = roleId ?? null;
    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('roleModal'));
    const title = document.getElementById('roleModalTitle');
    const saveBtn = document.getElementById('saveBtn');

    if (mode === 'create') {
        title.textContent = '\u6a29\u9650\u767b\u9332';
        saveBtn.textContent = '登録する';
        saveBtn.style.display = 'block';
        switchToEditBtn.style.display = 'none';

        document.getElementById('roleForm').reset();
        document.getElementById('roleId').value = '';
        document.querySelectorAll('#roleForm input').forEach(input => input.disabled = false);
        document.getElementById('roleModalErrors').classList.add('d-none');
        document.getElementById('roleModalErrorList').innerHTML = '';
        const rn = document.getElementById('roleName');
        rn.classList.remove('is-invalid');
        const fb = rn.nextElementSibling;
        if (fb && fb.classList.contains('invalid-feedback')) fb.remove();

        // 新規登録では削除できない
        document.getElementById('deleteBtn').style.display = 'none';
    } else {
        if (currentRoleId == null) return;

        document.getElementById('roleForm').reset();

        if (mode === 'edit') {
            title.textContent = '権限編集';
            saveBtn.textContent = '更新する';
            saveBtn.style.display = 'block';



            switchToEditBtn.style.display = 'none';
        } else {
            title.textContent = '権限照会';
            saveBtn.style.display = 'none';
            switchToEditBtn.style.display = 'block';
        }

        // 削除は編集時のみ行える
        document.getElementById('deleteBtn').style.display =
            (mode === 'edit') ? 'block' : 'none';

        loadRoleDetail(currentRoleId, mode === 'view');
    }

    modal.show();
}

function loadRoleDetail(roleId, readonly) {
    const ctx = document.querySelector('[data-ctx]')?.dataset.ctx?.replace(/\/$/, '') ?? '';
    fetch(`${ctx}/admin/role/detail/${roleId}`)
        .then(response => response.json())
        .then(data => {
            document.getElementById('roleId').value = data.role.roleId;
            document.getElementById('roleName').value = data.role.name;
            document.getElementById('version').value = data.role.version;

            if (readonly && !data.editable) {
                switchToEditBtn.style.display = 'none';
            }
            if (!data.editable) {
                document.getElementById('deleteBtn').style.display = 'none';
            }

            // パーミッションをリセットしてから設定
            document.querySelectorAll('input[name^="permission_"][value="0"]').forEach(r => r.checked = true);
            if (data.permissions) {
                Object.entries(data.permissions).forEach(([screenId, permission]) => {
                    const radio = document.querySelector(`input[name="permission_${screenId}"][value="${permission}"]`);
                    if (radio) radio.checked = true;
                });
            }

            document.getElementById('roleName').disabled = readonly;
            document.querySelectorAll('#screenPermissions input').forEach(input => input.disabled = readonly);
        });
}

function saveRole() {
    const ctx = document.querySelector('[data-ctx]')?.dataset.ctx?.replace(/\/$/, '') ?? '';
    const form = document.getElementById('roleForm');
    const formData = new FormData(form);

    const name = formData.get('name');
    const errorsEl = document.getElementById('roleModalErrors');
    const errorList = document.getElementById('roleModalErrorList');
    const errorCount = document.getElementById('roleModalErrorCount');

    errorsEl.classList.add('d-none');
    errorList.innerHTML = '';

    const roleNameInput = document.getElementById('roleName');
    roleNameInput.classList.remove('is-invalid');
    let existingFeedback = roleNameInput.nextElementSibling;
    if (existingFeedback && existingFeedback.classList.contains('invalid-feedback')) {
        existingFeedback.remove();
    }

    if (!name || !name.trim()) {
        errorList.innerHTML = '<li>\u6a29\u9650\u540d\u306f\u5fc5\u9808\u3067\u3059</li>';
        errorCount.textContent = '1';
        errorsEl.classList.remove('d-none');
        roleNameInput.classList.add('is-invalid');
        const feedback = document.createElement('div');
        feedback.className = 'invalid-feedback';
        feedback.textContent = '\u6a29\u9650\u540d\u306f\u5fc5\u9808\u3067\u3059';
        roleNameInput.insertAdjacentElement('afterend', feedback);
        return;
    }

    const data = {
        roleId: formData.get('roleId') || null,
        name: formData.get('name'),
        version: formData.get('version') || null,
        screenPermissions: {}
    };

    document.querySelectorAll('input[name^="permission_"]:checked').forEach(radio => {
        const screenId = radio.name.replace('permission_', '');
        const permission = parseInt(radio.value);
        if (permission > 0) {
            data.screenPermissions[screenId] = permission;
        }
    });

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    fetch(ctx + '/admin/role/save', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify(data)
    })
        .then(response => {
            if (!response.ok) throw new Error('HTTP ' + response.status);
            return response.json();
        })
        .then(result => {
            if (result.success) {
                const msg = (currentMode === 'create') ? '権限を登録しました。' : '権限を更新しました。';
                sessionStorage.setItem('flashMessage', msg);
                location.reload();
            } else {
                alert('保存に失敗しました: ' + result.message);
            }
        })
        .catch(err => alert('通信エラー: ' + err.message));
}

function switchToEditMode() {
	
    currentMode = 'edit';

    // タイトルとボタンの表示を切り替え
    document.getElementById('roleModalTitle').textContent = '権限編集';
    document.getElementById('saveBtn').textContent = '更新する';
    document.getElementById('saveBtn').style.display = 'block';
    document.getElementById('switchToEditBtn').style.display = 'none';

    // 削除は編集時のみ行える
    document.getElementById('deleteBtn').style.display = 'block';

    // フォームのロックを解除
    document.getElementById('roleName').disabled = false;
    document.querySelectorAll('#screenPermissions input').forEach(input => input.disabled = false);

    // 照会から編集に切り替わった時点の値を、変更検知用の新しい初期値として再セット
    const roleNameInput = document.getElementById('roleName');
    if (roleNameInput) {

        roleNameInput.style.removeProperty('border');
    }

    // ラジオボタンの背景色リセット
    document.querySelectorAll('#screenPermissions td').forEach(td => td.style.removeProperty('background-color'));

    // モーダルの位置を一番上へリセット
    const modalEl = document.getElementById('roleModal');
    if (modalEl) {
		// 上にスクロールさせる
        modalEl.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

function viewAssignedUsers(roleId) {
    const ctx = document.querySelector('[data-ctx]')?.dataset.ctx?.replace(/\/$/, '') ?? '';
    if (roleId == null) return;

    fetch(`${ctx}/admin/role/users/${roleId}`)
        .then(response => response.json())
        .then(data => {
            if (data.error) {
                alert(data.message);
                return;
            }
            document.getElementById('usersModalRoleName').textContent = data.roleName;
            const tbody = document.getElementById('usersModalBody');
            tbody.innerHTML = '';
            if (data.users.length === 0) {
                const tr = document.createElement('tr');
                tr.innerHTML = '<td class="text-muted">この権限が付与されているユーザーはありません。</td>';
                tbody.appendChild(tr);
            } else {
                data.users.forEach(user => {
                    const tr = document.createElement('tr');
                    const td = document.createElement('td');
                    td.textContent = user.name;
                    tr.appendChild(td);
                    tbody.appendChild(tr);
                });
            }
            bootstrap.Modal.getOrCreateInstance(document.getElementById('usersModal')).show();
        });
}

function deleteRole() {
    if (currentRoleId == null) return;
    bootstrap.Modal.getOrCreateInstance(document.getElementById('deleteConfirmModal')).show();
}

document.getElementById('deleteForm').addEventListener('submit', function(e) {
    e.preventDefault();
    bootstrap.Modal.getInstance(document.getElementById('deleteConfirmModal')).hide();

    if (currentRoleId == null) return;

    const ctx = document.querySelector('[data-ctx]')?.dataset.ctx?.replace(/\/$/, '') ?? '';
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    fetch(`${ctx}/admin/role/delete/${currentRoleId}`, {
        method: 'POST',
        headers: { [csrfHeader]: csrfToken }
    })
        .then(r => r.json())
        .then(result => {
            if (result.success) {
                sessionStorage.setItem('flashMessage', '権限を削除しました。');
                location.reload();
            } else {
                // 付与ユーザーが存在する場合などはメッセージを表示して削除しない
                alert(result.message);
            }
        })
        .catch(err => alert('通信エラー: ' + err.message));
});

document.addEventListener('DOMContentLoaded', function() {

    // 全件を描画済みのため、クライアント側でページングする
    const rows = Array.from(document.querySelectorAll('#roleTableBody tr')).filter(tr => tr.querySelector('td'));
    const pageSizeSelect = document.getElementById('pageSizeSelect');
    const pager = new Pagination(rows, pageSizeSelect, document.getElementById('pagination'));
    if (rows.length > 0) {
        pager.render(1);
        pageSizeSelect?.addEventListener('change', () => pager.render(1));
    }

    const flash = sessionStorage.getItem('flashMessage');
    if (flash) {
        document.getElementById('flashMessageText').textContent = flash;
        document.getElementById('flashMessage').classList.remove('d-none');
        sessionStorage.removeItem('flashMessage');
    }

    // 画面表示時の初期状態を記憶するためのマップ
    const initialValues = {};

    // ラジオボタンをnameごとに処理
    const allRadios = document.querySelectorAll('#screenPermissions input[type="radio"]');

    allRadios.forEach(radio => {

        // 最初にcheckedがついているもののvalueを記憶
        if (radio.checked) {
            initialValues[radio.name] = radio.value;
        }

        // ラジオボタンがクリックされた時のイベント
        radio.addEventListener('change', function() {
            checkRadioChange(this.name);
        });
    });

    // 変更があったか判定して色を変える
    function checkRadioChange(name) {

        // 現在チェックされているラジオボタンを取得
        const checkedRadio = document.querySelector(`input[name="${name}"]:checked`);
        const currentValue = checkedRadio ? checkedRadio.value : '';

        // 記憶しておいた初期値を取得
        const initialValue = initialValues[name];

        // 対象のラジオボタンが存在する行（tr）を取得
        const tr = document.querySelector(`input[name="${name}"]`).closest('tr');
        if (!tr) return;

        // 初期値と現在の値が違っていれば背景色を黄色にする
        if (currentValue !== initialValue) {
            tr.style.backgroundColor = '#fff9c4';
        } else {
            tr.style.backgroundColor = '';
        }
    }
});

// モーダル内のラジオボタン変更検知＆色変更ロジック
(function() {

    // 各初期値を保持する変数
    let initialRadioValues = {};
    let initialRoleName = '';

    const roleModal = document.getElementById('roleModal');
    const roleNameInput = document.getElementById('roleName');

    if (roleModal) {
        roleModal.addEventListener('shown.bs.modal', function() {
            // ラジオボタンの初期化
            initialRadioValues = {};
            const radios = document.querySelectorAll('#screenPermissions input[type="radio"]');
            radios.forEach(radio => {
                if (radio.checked) {
                    initialRadioValues[radio.name] = radio.value;
                }
                const tdList = radio.closest('tr').querySelectorAll('td');
                tdList.forEach(td => td.style.removeProperty('background-color'));
            });

            // 権限名の初期化
            if (roleNameInput) {
                initialRoleName = roleNameInput.value;
                // 枠線の色とシャドウを初期状態に戻す
                roleNameInput.style.removeProperty('border-color');
                roleNameInput.style.removeProperty('box-shadow');
            }
        });
    }

    // ラジオボタンの変更イベント
    const screenPermissions = document.getElementById('screenPermissions');
    if (screenPermissions) {
        screenPermissions.addEventListener('change', function(event) {
            if (event.target && event.target.type === 'radio') {
                const clickedRadio = event.target;
                const name = clickedRadio.name;
                const currentValue = clickedRadio.value;
                const initialValue = initialRadioValues[name];

                const tr = clickedRadio.closest('tr');
                const tdList = tr.querySelectorAll('td');

                if (currentValue !== initialValue) {
                    tdList.forEach(td => {
                        td.style.setProperty('background-color', '#fff9c4', 'important');
                    });
                } else {
                    tdList.forEach(td => {
                        td.style.removeProperty('background-color');
                    });
                }
            }
        });
    }

    // 権限名の変更イベント
    if (roleNameInput) {
        roleNameInput.addEventListener('input', function() {
            // エラー表示中はBootstrapの赤枠(is-invalid)を優先するため、判定から除外する
            if (this.classList.contains('is-invalid')) {
                this.style.removeProperty('border');
                return;
            }

            if (this.value !== initialRoleName) {
                // 黄色い枠を適用
                this.style.setProperty('border', '3px solid #ffeb3b', 'important');
            } else {
                // 元に戻ったらスタイルを削除
                this.style.removeProperty('border');
            }
        });
    }
})();