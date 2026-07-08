let currentMode = 'create';

function updateButtons() {
    const checked = document.querySelectorAll('.role-checkbox:checked');
    const editBtn = document.getElementById('editBtn');
    const viewBtn = document.getElementById('viewBtn');
    const usersBtn = document.getElementById('usersBtn');
    const deleteBtn = document.getElementById('deleteBtn');

    const singleSelection = checked.length === 1;
    const anySelection = checked.length >= 1;
	
	const includesAdminRole = Array.from(checked).some(cb => cb.value === '1');

	editBtn.disabled = !singleSelection || includesAdminRole;
	viewBtn.disabled = !singleSelection;
	usersBtn.disabled = !singleSelection;
	deleteBtn.disabled = !anySelection || includesAdminRole;
}

function openRoleModal(mode) {
    currentMode = mode;
    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('roleModal'));
    const title = document.getElementById('roleModalTitle');
    const saveBtn = document.getElementById('saveBtn');

    if (mode === 'create') {
        title.textContent = '\u6a29\u9650\u767b\u9332';
        saveBtn.style.display = 'block';
        document.getElementById('roleForm').reset();
        document.getElementById('roleId').value = '';
        document.querySelectorAll('#roleForm input').forEach(input => input.disabled = false);
        document.getElementById('roleModalErrors').classList.add('d-none');
        document.getElementById('roleModalErrorList').innerHTML = '';
        const rn = document.getElementById('roleName');
        rn.classList.remove('is-invalid');
        const fb = rn.nextElementSibling;
        if (fb && fb.classList.contains('invalid-feedback')) fb.remove();
    } else {
        const checked = document.querySelector('.role-checkbox:checked');
        if (!checked) return;

        const roleId = checked.value;
        document.getElementById('roleForm').reset();

        if (mode === 'edit') {
            title.textContent = '権限編集';
            saveBtn.style.display = 'block';
        } else {
            title.textContent = '権限照会';
            saveBtn.style.display = 'none';
        }

        loadRoleDetail(roleId, mode === 'view');
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
                location.reload();
            } else {
                alert('保存に失敗しました: ' + result.message);
            }
        })
        .catch(err => alert('通信エラー: ' + err.message));
}

let currentUsersRoleId = null;

function viewAssignedUsers() {
    const ctx = document.querySelector('[data-ctx]')?.dataset.ctx?.replace(/\/$/, '') ?? '';
    const checked = document.querySelector('.role-checkbox:checked');
    if (!checked) return;
    currentUsersRoleId = checked.value;

    fetch(`${ctx}/admin/role/users/${currentUsersRoleId}`)
        .then(response => response.json())
        .then(data => {
            document.getElementById('usersModalRoleName').textContent = data.roleName;
            const tbody = document.getElementById('usersModalBody');
            tbody.innerHTML = '';
            data.users.forEach(user => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td><input type="checkbox" class="user-assign-checkbox" value="${user.id}" ${user.assigned ? 'checked' : ''}></td>
                    <td>${user.name}</td>`;
                tbody.appendChild(tr);
            });
            bootstrap.Modal.getOrCreateInstance(document.getElementById('usersModal')).show();
        });
}

function updateAssignedUsers() {
    const ctx = document.querySelector('[data-ctx]')?.dataset.ctx?.replace(/\/$/, '') ?? '';
    const userIds = Array.from(document.querySelectorAll('.user-assign-checkbox:checked')).map(cb => cb.value);
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    fetch(`${ctx}/admin/role/users/${currentUsersRoleId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
        body: JSON.stringify({ userIds })
    })
        .then(r => r.json())
        .then(result => {
            if (result.success) {
                bootstrap.Modal.getInstance(document.getElementById('usersModal')).hide();
                sessionStorage.setItem('flashMessage', '付与ユーザーを更新しました。');
                location.reload();
            } else {
                alert('更新に失敗しました: ' + result.message);
            }
        })
        .catch(err => alert('通信エラー: ' + err.message));
}

function deleteRoles() {
    const ctx = document.querySelector('[data-ctx]')?.dataset.ctx?.replace(/\/$/, '') ?? '';
    const checked = document.querySelectorAll('.role-checkbox:checked');
    if (checked.length === 0) return;

    if (!confirm(`チェックした${checked.length}件の権限を削除します。\n対象権限のユーザーはデフォルト権限に変更されます。\nよろしいですか？`)) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    const requests = Array.from(checked).map(cb =>
        fetch(`${ctx}/admin/role/delete/${cb.value}`, {
            method: 'POST',
            headers: { [csrfHeader]: csrfToken }
        }).then(r => r.json())
    );

    Promise.all(requests)
        .then(results => {
            const failed = results.filter(r => !r.success);
            if (failed.length > 0) {
                alert(failed.map(r => r.message).join(', '));
            }
            location.reload();
        })
        .catch(err => alert('通信エラー: ' + err.message));

    const msg = sessionStorage.getItem('flashMessage');
    if (msg) {
        document.getElementById('flashMessageText').textContent = msg;
        document.getElementById('flashMessage').classList.remove('d-none');
        sessionStorage.removeItem('flashMessage');
    }
}
