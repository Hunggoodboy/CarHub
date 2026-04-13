let adminChatState = {
    initialized: false,
    isConnected: false,
    currentUserId: null,
    adminId: null,
    stompClient: null
};

function createAdminChatContainerIfMissing() {
    if (document.getElementById('admin-chat-container')) {
        return;
    }

    const chatContainerHTML = `
        <div id="admin-chat-container" class="chat-hidden">
            <div id="admin-header">
                Chat với Admin
                <span onclick="toggleAdminChat()" style="float:right; cursor:pointer; font-size: 20px;">&times;</span>
            </div>
            <div id="admin-messages">
                <div class="message bot">Xin chào! Vui lòng đợi trong giây lát để kết nối với Admin.</div>
            </div>
            <div id="admin-input-area">
                <input type="text" id="admin-user-input" placeholder="Nhập tin nhắn..." onkeypress="handleAdminKeyPress(event)">
                <button onclick="sendAdminMessage()" id="admin-send-btn">Gửi</button>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', chatContainerHTML);
}

function toggleAdminChat() {
    createAdminChatContainerIfMissing();
    const chatContainer = document.getElementById('admin-chat-container');
    chatContainer.classList.toggle('chat-hidden');

    if (!adminChatState.initialized) {
        initializeAdminChat();
    }
}

async function initializeAdminChat() {
    adminChatState.initialized = true;

    try {
        await fetchCurrentUserAndAdmin();
        if (!adminChatState.currentUserId || !adminChatState.adminId) {
            return;
        }

        clearAdminMessages();
        appendAdminMessage('bot', 'Bạn đang chat trực tiếp với Admin.');
        await loadAdminChatHistory();
        connectAdminWebSocket();
    } catch (error) {
        appendAdminMessage('bot', 'Không thể khởi tạo chat với Admin lúc này.');
        console.error('Admin chat init failed:', error);
    }
}

async function fetchCurrentUserAndAdmin() {
    const currentUserResponse = await fetch('/api/users/me');
    if (!currentUserResponse.ok) {
        appendAdminMessage('bot', 'Vui lòng đăng nhập để chat với Admin.');
        disableAdminInput(true);
        return;
    }

    adminChatState.currentUserId = await currentUserResponse.json();

    const adminResponse = await fetch('/api/users/role/ADMIN');
    if (!adminResponse.ok) {
        appendAdminMessage('bot', 'Không tìm thấy tài khoản Admin để chat.');
        disableAdminInput(true);
        return;
    }

    const admins = await adminResponse.json();
    if (!Array.isArray(admins) || admins.length === 0) {
        appendAdminMessage('bot', 'Hiện chưa có Admin trực tuyến.');
        disableAdminInput(true);
        return;
    }

    const chosenAdmin = admins.find(admin => admin.id !== adminChatState.currentUserId) || admins[0];
    adminChatState.adminId = chosenAdmin.id;
    disableAdminInput(false);
}

async function loadAdminChatHistory() {
    const response = await fetch(`/api/messages/history/${adminChatState.adminId}?page=0&size=30`);
    if (!response.ok) {
        return;
    }

    const messages = await response.json();
    const sortedMessages = messages.reverse();

    sortedMessages.forEach(msg => {
        const isMine = msg.senderId === adminChatState.currentUserId;
        appendAdminMessage(isMine ? 'user' : 'bot', msg.content);
    });
}

function connectAdminWebSocket() {
    if (adminChatState.isConnected || typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
        return;
    }

    const socket = new SockJS('/gs-guide-websocket');
    adminChatState.stompClient = Stomp.over(socket);
    adminChatState.stompClient.debug = null;

    adminChatState.stompClient.connect({}, () => {
        adminChatState.isConnected = true;

        adminChatState.stompClient.subscribe('/user/queue/private', message => {
            const payload = JSON.parse(message.body);
            const isBetweenCurrentAndAdmin =
                (payload.senderId === adminChatState.adminId && payload.receiverId === adminChatState.currentUserId) ||
                (payload.senderId === adminChatState.currentUserId && payload.receiverId === adminChatState.adminId);

            if (!isBetweenCurrentAndAdmin) {
                return;
            }

            const isMine = payload.senderId === adminChatState.currentUserId;
            appendAdminMessage(isMine ? 'user' : 'bot', payload.content);
        });
    }, () => {
        adminChatState.isConnected = false;
        appendAdminMessage('bot', 'Mất kết nối với máy chủ chat. Vui lòng thử lại.');
    });
}

function appendAdminMessage(role, text) {
    const messages = document.getElementById('admin-messages');
    if (!messages) {
        return;
    }

    const bubble = document.createElement('div');
    bubble.className = `message ${role}`;
    bubble.innerHTML = formatAdminMessage(text);
    messages.appendChild(bubble);
    scrollAdminMessagesToBottom();
}

function formatAdminMessage(text) {
    return (text ?? '').toString()
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/\n/g, '<br>');
}

function clearAdminMessages() {
    const messages = document.getElementById('admin-messages');
    if (!messages) {
        return;
    }

    messages.innerHTML = '';
}

function scrollAdminMessagesToBottom() {
    const messages = document.getElementById('admin-messages');
    if (!messages) {
        return;
    }

    messages.scrollTop = messages.scrollHeight;
}

function disableAdminInput(disabled) {
    const input = document.getElementById('admin-user-input');
    const button = document.getElementById('admin-send-btn');

    if (input) {
        input.disabled = disabled;
    }

    if (button) {
        button.disabled = disabled;
    }
}

function sendAdminMessage() {
    const input = document.getElementById('admin-user-input');
    if (!input || !adminChatState.stompClient || !adminChatState.isConnected || !adminChatState.adminId) {
        return;
    }

    const content = input.value.trim();
    if (!content) {
        return;
    }

    adminChatState.stompClient.send('/app/chat.private', {}, JSON.stringify({
        senderId: adminChatState.currentUserId,
        receiverId: adminChatState.adminId,
        content
    }));

    input.value = '';
}

function handleAdminKeyPress(e) {
    if (e.key === 'Enter') {
        sendAdminMessage();
    }
}
