let users = [];
let editingUserId = null;
let currentAdminUserId = null;

function escapeHtml(text) {
    return String(text || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function loadUsers() {
    fetch('/api/admin/users')
        .then(res => {
            if (!res.ok) {
                throw new Error('Không thể tải danh sách người dùng');
            }
            return res.json();
        })
        .then(data => {
            users = Array.isArray(data) ? data : [];
            renderUsers(users);
        })
        .catch(err => {
            console.error(err);
            alert('Không tải được danh sách người dùng.');
        });
}

function loadCurrentAdminUserId() {
    fetch('/api/users/me')
        .then(res => {
            if (!res.ok) {
                throw new Error('Không thể tải thông tin người dùng hiện tại');
            }
            return res.json();
        })
        .then(id => {
            currentAdminUserId = Number(id);
            renderUsers(users);
        })
        .catch(err => console.error(err));
}

function renderUsers(list) {
    const tbody = document.getElementById('user-table-body');
    if (!tbody) return;

    if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="8">Không có dữ liệu người dùng.</td></tr>';
        return;
    }

    tbody.innerHTML = list.map(user => `
        <tr>
            <td>${user.id ?? ''}</td>
            <td>${escapeHtml(user.username)}</td>
            <td>${escapeHtml(user.fullName)}</td>
            <td>${escapeHtml(user.email)}</td>
            <td>${escapeHtml(user.phoneNumber)}</td>
            <td>${escapeHtml(user.address)}</td>
            <td>${escapeHtml(user.role)}</td>
            <td>
                <div class="actions">
                    <button class="edit-btn" data-id="${user.id}">Sửa</button>
                    <button class="delete-btn" data-id="${user.id}" ${Number(user.id) === Number(currentAdminUserId) ? 'disabled title="Không thể xóa chính tài khoản của bạn"' : ''}>Xóa</button>
                </div>
            </td>
        </tr>
    `).join('');
}

function openEditModal(userId) {
    const user = users.find(u => Number(u.id) === Number(userId));
    if (!user) return;

    editingUserId = user.id;
    document.getElementById('edit-fullName').value = user.fullName || '';
    document.getElementById('edit-email').value = user.email || '';
    document.getElementById('edit-phone').value = user.phoneNumber || '';
    document.getElementById('edit-address').value = user.address || '';
    document.getElementById('edit-role').value = user.role || 'CUSTOMER';

    const modal = document.getElementById('edit-modal');
    if (modal) modal.style.display = 'flex';
}

function closeEditModal() {
    editingUserId = null;
    const modal = document.getElementById('edit-modal');
    if (modal) modal.style.display = 'none';
}

function saveUser() {
    if (!editingUserId) return;

    const payload = {
        fullName: document.getElementById('edit-fullName').value.trim(),
        email: document.getElementById('edit-email').value.trim(),
        phoneNumber: document.getElementById('edit-phone').value.trim(),
        address: document.getElementById('edit-address').value.trim(),
        role: document.getElementById('edit-role').value
    };

    fetch(`/api/admin/users/${editingUserId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(res => {
            if (!res.ok) {
                throw new Error('Cập nhật thất bại');
            }
            return res.json();
        })
        .then(() => {
            closeEditModal();
            loadUsers();
        })
        .catch(err => {
            console.error(err);
            alert('Không thể cập nhật user.');
        });
}

function deleteUser(userId) {
    if (!confirm('Bạn có chắc chắn muốn xóa người dùng này?')) return;

    fetch(`/api/admin/users/${userId}`, { method: 'DELETE' })
        .then(res => {
            if (!res.ok) {
                throw new Error('Xóa thất bại');
            }
            loadUsers();
        })
        .catch(err => {
            console.error(err);
            alert('Không thể xóa user.');
        });
}

function bindEvents() {
    const reloadBtn = document.getElementById('reload-btn');
    if (reloadBtn) reloadBtn.addEventListener('click', loadUsers);

    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('input', () => {
            const key = searchInput.value.trim().toLowerCase();
            if (!key) {
                renderUsers(users);
                return;
            }

            const filtered = users.filter(user => {
                const source = [
                    user.username,
                    user.fullName,
                    user.email,
                    user.phoneNumber,
                    user.address,
                    user.role
                ].join(' ').toLowerCase();

                return source.includes(key);
            });

            renderUsers(filtered);
        });
    }

    const tbody = document.getElementById('user-table-body');
    if (tbody) {
        tbody.addEventListener('click', event => {
            const editBtn = event.target.closest('.edit-btn');
            if (editBtn) {
                openEditModal(editBtn.dataset.id);
                return;
            }

            const deleteBtn = event.target.closest('.delete-btn');
            if (deleteBtn) {
                if (Number(deleteBtn.dataset.id) === Number(currentAdminUserId)) {
                    alert('Bạn không thể xóa chính tài khoản của mình.');
                    return;
                }
                deleteUser(deleteBtn.dataset.id);
            }
        });
    }

    const saveBtn = document.getElementById('save-btn');
    if (saveBtn) saveBtn.addEventListener('click', saveUser);

    const cancelBtn = document.getElementById('cancel-btn');
    if (cancelBtn) cancelBtn.addEventListener('click', closeEditModal);
}

document.addEventListener('DOMContentLoaded', () => {
    bindEvents();
    loadUsers();
    loadCurrentAdminUserId();
});
