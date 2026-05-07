let currentMode = 'create';

function updateButtons() {
    const checked = document.querySelectorAll('.role-checkbox:checked');
    const editBtn = document.getElementById('editBtn');
    const viewBtn = document.getElementById('viewBtn');
    const usersBtn = document.getElementById('usersBtn');

    const singleSelection = checked.length === 1;

    editBtn.disabled = !singleSelection;
    viewBtn.disabled = !singleSelection;
    usersBtn.disabled = !singleSelection;
}

function openRoleModal(mode) {
    currentMode = mode;
    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('roleModal'));
    const title = document.getElementById('roleModalTitle');
    const saveBtn = document.getElementById('saveBtn');

    if (mode === 'create') {
        title.textContent = '権限登録';
        saveBtn.style.display = 'block';
        document.getElementById('roleForm').reset();
        document.getElementById('roleId').value = '';
        document.querySelectorAll('#roleForm input').forEach(input => input.disabled = false);
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

function viewAssignedUsers() {
    const ctx = document.querySelector('[data-ctx]')?.dataset.ctx?.replace(/\/$/, '') ?? '';
    const checked = document.querySelector('.role-checkbox:checked');
    if (!checked) return;
    window.location.href = `${ctx}/admin/role/users/${checked.value}`;
}
