class ChatApplication {
    constructor() {
        // Thuộc tính trạng thái (State)
        this.currentUserId = null;
        this.currentPartnerId = null;
        this.stompClient = null;
        this.currentPage = 0;
        this.pageSize = 20;
        this.reconnectAttempts = 0;
        this.isConnecting = false;

        // Các thành phần DOM (Encapsulation)
        this.ui = {
            recentChatsList: document.getElementById('recent-chats-list'),
            chatMessages: document.getElementById('chat-messages'),
            chatInput: document.getElementById('chat-input'),
            btnSend: document.getElementById('btn-send'),
            btnLike: document.getElementById('btn-like'),
            chatHeader: document.getElementById('chat-header'),
            chatInputArea: document.getElementById('chat-input-area'),
            partnerName: document.getElementById('partner-name'),
            partnerAvatar: document.getElementById('partner-avatar'),
            emptyState: document.getElementById('empty-state'),
            connectionStatus: document.getElementById('connection-status')
        };

        // Ràng buộc (bind) ngữ cảnh this cho các sự kiện
        this.sendMessage = this.sendMessage.bind(this);
        this.handleKeyPress = this.handleKeyPress.bind(this);
    }

    // Phương thức khởi chạy hệ thống
    async start() {
        await this.fetchCurrentUser();
        if (!this.currentUserId) {
            console.error("Chưa đăng nhập. Vui lòng đăng nhập trước.");
            return;
        }

        this.attachEventListeners();
        this.connectWebSocket();
        await this.loadRecentChats();
    }
    filterChats() {
        const searchInput = document.getElementById('chat-search');
        const filter = searchInput.value.toLowerCase();
        const chatItems = this.ui.recentChatsList.querySelectorAll('.chat-item');

        chatItems.forEach(item => {
            const userNameEl = item.querySelector('.user-name');
            if (userNameEl) {
                const userName = userNameEl.innerText.toLowerCase();
                item.style.display = userName.includes(filter) ? "" : "none";
            }
        });
    }

    // Gắn các sự kiện (Listeners)
    attachEventListeners() {
        this.ui.btnSend.addEventListener('click', this.sendMessage);
        this.ui.chatInput.addEventListener('keydown', this.handleKeyPress);
        this.ui.btnLike.addEventListener('click',() => this.sendLike());
        const searchInput = document.getElementById('chat-search');
        if (searchInput) {
            searchInput.addEventListener('input', () => this.filterChats());
        }

        // Bắt sự kiện cuộn chuột để làm chức năng Pagination (Load tin nhắn cũ)
        this.ui.chatMessages.addEventListener('scroll', async () => {
            if (this.ui.chatMessages.scrollTop === 0 && this.currentPartnerId) {
                this.currentPage++;
                await this.loadChatHistory(this.currentPartnerId, true);
            }
        });
    }

    // Fetch API: Lấy thông tin user đang đăng nhập
    async fetchCurrentUser() {
        try {
            const response = await fetch('/api/users/me');
            if (response.ok) {
                this.currentUserId = await response.json();
            }
        } catch (error) {
            console.error("Lỗi fetch user:", error);
        }
    }

    // Fetch API: Lấy danh sách nhắn tin bên trái
    async loadRecentChats() {
        try {
            const response = await fetch('/api/messages/recent');
            if (!response.ok) return;
            const chats = await response.json();

            this.ui.recentChatsList.innerHTML = '';
            chats.forEach(chat => {
                const li = document.createElement('li');
                li.className = 'chat-item';
                li.innerHTML = `
                    <div class="avatar">${chat.partnerName.charAt(0).toUpperCase()}</div>
                    <div class="chat-item-details">
                        <h4 class="user-name">${this.escapeHtml(chat.partnerName)}</h4>
                        <p>${this.escapeHtml(chat.lastMessage || '')}</p>
                    </div>
                `;
                li.addEventListener('click', () => this.selectPartner(chat.partnerId, chat.partnerName, li));
                this.ui.recentChatsList.appendChild(li);
            });
        } catch (error) {
            console.error("Lỗi load recent chats:", error);
        }
    }

    // Xử lý khi click vào 1 người bên sidebar
    async selectPartner(partnerId, partnerName, listItemElement) {
        this.currentPartnerId = partnerId;
        this.currentPage = 0; // Reset trang về 0

        // Highlight mục đang chọn ở sidebar
        document.querySelectorAll('.chat-item').forEach(el => el.classList.remove('active'));
        if(listItemElement) listItemElement.classList.add('active');

        // Hiển thị khung chat bên phải
        this.ui.chatHeader.style.display = 'flex';
        this.ui.chatInputArea.style.display = 'flex';
        this.ui.emptyState.style.display = 'none';
        this.ui.partnerName.textContent = partnerName;
        this.ui.partnerAvatar.textContent = partnerName.charAt(0).toUpperCase();

        // Xóa tin nhắn cũ trên màn hình và load lịch sử mới
        this.ui.chatMessages.innerHTML = '';
        await this.loadChatHistory(partnerId, false);
    }

    // Fetch API: Lấy lịch sử đoạn chat với phân trang
    async loadChatHistory(partnerId, isPrepend) {
        try {
            const response = await fetch(`/api/messages/history/${partnerId}?page=${this.currentPage}&size=${this.pageSize}`);
            if (!response.ok) return;

            const messages = await response.json();
            // Vì API trả về tin nhắn mới nhất trước (DESC), ta cần đảo ngược mảng để render từ trên xuống dưới
            const sortedMessages = messages.reverse();

            const oldScrollHeight = this.ui.chatMessages.scrollHeight;

            sortedMessages.forEach(msg => {
                this.renderMessage(msg, isPrepend);
            });

            // Nếu là cuộn lên để load cũ (prepend), giữ nguyên vị trí cuộn
            if (isPrepend) {
                this.ui.chatMessages.scrollTop = this.ui.chatMessages.scrollHeight - oldScrollHeight;
            } else {
                this.scrollToBottom();
            }
        } catch (error) {
            console.error("Lỗi load history:", error);
        }
    }
    updateStatus(type){
        const el=this.ui.connectionStatus;
        if(!el) return;
        if (type === 'connected'){
            el.textContent = '🟢 Đã kết nối';
            el.className = 'status connected';
        } else if (type === 'disconnected'){
            el.textContent = '🔴 Mất kết nối';
            el.className = 'status disconnected';
        } else if (type === 'reconnecting'){
            el.textContent = '🟡 Đang kết nối lại...';
            el.className = 'status reconnecting';
        }
    }

    // WebSocket: Kết nối và cấu hình kênh
    connectWebSocket() {
        if (this.isConnecting) return;
        this.isConnecting = true;
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.disconnect();
        }
        const socket = new SockJS('/gs-guide-websocket'); // Đổi endpoint cho đúng với config backend
        this.stompClient = Stomp.over(socket);
        this.stompClient.debug = null; // Tắt log rác của Stomp

        this.stompClient.connect({}, (frame) => {
            console.log("Đã kết nối WebSocket");
            this.updateStatus('connected');
            this.reconnectAttempts = 0;
            this.isConnecting = false;
            // Lắng nghe tin nhắn tới
            this.stompClient.subscribe('/user/queue/private', (message) => {
                const msg = JSON.parse(message.body);

                // Chỉ hiển thị tin nhắn nếu đang mở khung chat với người đó
                if (msg.senderId === this.currentPartnerId || msg.receiverId === this.currentPartnerId) {
                    this.renderMessage(msg, false);
                    this.scrollToBottom();
                }

                // Cập nhật lại sidebar bên trái bất kể đang mở chat với ai
                this.loadRecentChats();
            });
        }, (error) => {
            console.error("Mất kết nối WebSocket:", error);
            this.updateStatus('disconnected')
            this.isConnecting = false;
            if (this.reconnectAttempts < 5) {
                this.reconnectAttempts++;

                setTimeout(() => {
                    console.log("Đang kết nối lại ....");
                    this.updateStatus('reconnecting');
                    this.connectWebSocket();
                }, 3000);
            }
            // Có thể viết thêm logic auto-reconnect ở đây
        });
    }

    // Xử lý gửi tin nhắn
    sendMessage() {
        const content = this.ui.chatInput.value.trim();
        if (!content || !this.currentPartnerId || !this.stompClient) return;

        const chatMessageRequest = {
            senderId: this.currentUserId,
            receiverId: this.currentPartnerId,
            content: content
        };

        this.stompClient.send('/app/chat.private', {}, JSON.stringify(chatMessageRequest));
        this.ui.chatInput.value = '';
    }

    handleKeyPress(event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.sendMessage();
        }
    }
    sendLike(){
        if(!this.currentPartnerId || !this.stompClient) return;
        const chatMessageRequest= {
            senderId: this.currentUserId,
            receiverId: this.currentPartnerId,
            content :"👍",
            
        };
        this.stompClient.send('/app/chat.private',{},JSON.stringify(chatMessageRequest));
    }

    // Render 1 bong bóng tin nhắn ra màn hình
    renderMessage(msg, isPrepend) {
        const isMe = msg.senderId === this.currentUserId;
        const timeStr = new Date(msg.sentAt || Date.now()).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

        const wrapper = document.createElement('div');
        wrapper.className = `msg-wrapper ${isMe ? 'me' : 'them'}`;
        wrapper.innerHTML = `
            <div class="msg-bubble">${this.escapeHtml(msg.content)}</div>
            <div class="msg-time">${timeStr}</div>
        `;

        if (isPrepend) {
            this.ui.chatMessages.prepend(wrapper);
        } else {
            this.ui.chatMessages.appendChild(wrapper);
        }
    }

    scrollToBottom() {
        this.ui.chatMessages.scrollTop = this.ui.chatMessages.scrollHeight;
    }

    escapeHtml(text) {
        return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }
}

// Khởi tạo và chạy ứng dụng khi DOM đã sẵn sàng
document.addEventListener('DOMContentLoaded', () => {
    const app = new ChatApplication();
    app.start();
});
