class ChatApplication {
    constructor() {
        this.currentUserId = null;
        this.currentPartnerId = null;
        this.stompClient = null;
        this.currentPage = 0;
        this.pageSize = 20;
        this.reconnectAttempts = 0;
        this.isConnecting = false;

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

        this.sendMessage = this.sendMessage.bind(this);
        this.handleKeyPress = this.handleKeyPress.bind(this);
    }

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

    attachEventListeners() {
        this.ui.btnSend.addEventListener('click', this.sendMessage);
        this.ui.chatInput.addEventListener('keydown', this.handleKeyPress);
        this.ui.btnLike.addEventListener('click', () => this.sendLike());

        const searchInput = document.getElementById('chat-search');
        if (searchInput) {
            searchInput.addEventListener('input', () => this.filterChats());
        }

        this.ui.chatMessages.addEventListener('scroll', async () => {
            if (this.ui.chatMessages.scrollTop === 0 && this.currentPartnerId) {
                this.currentPage++;
                await this.loadChatHistory(this.currentPartnerId, true);
            }
        });
    }

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
                    <div class="avatar">
                        <img src="${chat.avatar ? '/uploads/' + chat.avatar : '/img/default-avatar.png'}" />
                    </div>
                    <div class="chat-item-details">
                        <h4 class="user-name">${this.escapeHtml(chat.partnerName)}</h4>
                        <p>${this.escapeHtml(chat.lastMessage || '')}</p>
                    </div>
                `;

                li.addEventListener('click', () =>
                    this.selectPartner(chat.partnerId, chat.partnerName, chat.avatar, li)
                );

                this.ui.recentChatsList.appendChild(li);
            });

        } catch (error) {
            console.error("Lỗi load recent chats:", error);
        }
    }

    selectPartner(partnerId, partnerName, avatar, listItemElement) {
        this.currentPartnerId = partnerId;
        this.currentPage = 0;

        document.querySelectorAll('.chat-item')
            .forEach(el => el.classList.remove('active'));

        if (listItemElement) listItemElement.classList.add('active');

        this.ui.chatHeader.style.display = 'flex';
        this.ui.chatInputArea.style.display = 'flex';
        this.ui.emptyState.style.display = 'none';

        this.ui.partnerName.textContent = partnerName;

        this.ui.partnerAvatar.innerHTML = `
            <img src="${avatar ? '/uploads/' + avatar : '/img/default-avatar.png'}" />
        `;

        this.ui.chatMessages.innerHTML = '';
        this.loadChatHistory(partnerId, false);
    }

    async loadChatHistory(partnerId, isPrepend) {
        try {
            const response = await fetch(
                `/api/messages/history/${partnerId}?page=${this.currentPage}&size=${this.pageSize}`
            );
            if (!response.ok) return;

            const messages = await response.json();
            const sortedMessages = messages.reverse();

            const oldScrollHeight = this.ui.chatMessages.scrollHeight;

            sortedMessages.forEach(msg => {
                this.renderMessage(msg, isPrepend);
            });

            if (isPrepend) {
                this.ui.chatMessages.scrollTop =
                    this.ui.chatMessages.scrollHeight - oldScrollHeight;
            } else {
                this.scrollToBottom();
            }

        } catch (error) {
            console.error("Lỗi load history:", error);
        }
    }

    updateStatus(type) {
        const el = this.ui.connectionStatus;
        if (!el) return;

        if (type === 'connected') {
            el.textContent = '🟢 Đã kết nối';
            el.className = 'status connected';
        } else if (type === 'disconnected') {
            el.textContent = '🔴 Mất kết nối';
            el.className = 'status disconnected';
        } else if (type === 'reconnecting') {
            el.textContent = '🟡 Đang kết nối lại...';
            el.className = 'status reconnecting';
        }
    }

    connectWebSocket() {
        if (this.isConnecting) return;
        this.isConnecting = true;

        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.disconnect();
        }

        const socket = new SockJS('/gs-guide-websocket');
        this.stompClient = Stomp.over(socket);
        this.stompClient.debug = null;

        this.stompClient.connect({}, () => {
            this.updateStatus('connected');
            this.reconnectAttempts = 0;
            this.isConnecting = false;

            this.stompClient.subscribe('/user/queue/private', (message) => {
                const msg = JSON.parse(message.body);

                if (msg.senderId === this.currentPartnerId ||
                    msg.receiverId === this.currentPartnerId) {
                    this.renderMessage(msg, false);
                    this.scrollToBottom();
                }

                this.loadRecentChats();
            });

        }, () => {
            this.updateStatus('disconnected');
            this.isConnecting = false;

            if (this.reconnectAttempts < 5) {
                this.reconnectAttempts++;

                setTimeout(() => {
                    this.updateStatus('reconnecting');
                    this.connectWebSocket();
                }, 3000);
            }
        });
    }

    sendMessage() {
        const content = this.ui.chatInput.value.trim();
        if (!content || !this.currentPartnerId || !this.stompClient) return;

        const chatMessageRequest = {
            senderId: this.currentUserId,
            receiverId: this.currentPartnerId,
            content: content
        };

        this.stompClient.send(
            '/app/chat.private',
            {},
            JSON.stringify(chatMessageRequest)
        );

        this.ui.chatInput.value = '';
    }

    handleKeyPress(event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.sendMessage();
        }
    }

    sendLike() {
        if (!this.currentPartnerId || !this.stompClient) return;

        const chatMessageRequest = {
            senderId: this.currentUserId,
            receiverId: this.currentPartnerId,
            content: "👍"
        };

        this.stompClient.send(
            '/app/chat.private',
            {},
            JSON.stringify(chatMessageRequest)
        );
    }

    renderMessage(msg, isPrepend) {
        const isMe = msg.senderId === this.currentUserId;
        const timeStr = new Date(msg.sentAt || Date.now())
            .toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

        const wrapper = document.createElement('div');
        wrapper.className = `msg-wrapper ${isMe ? 'me' : 'them'}`;

        // Kiểm tra xem tin nhắn này có phải là thẻ xe không
        const carCardData = this.parseCarCardMessage(msg.content);

        if (carCardData) {
            // Nếu là thẻ xe -> Render giao diện Card
            wrapper.innerHTML = `
                <div class="msg-bubble" style="padding: 0; background: white; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; max-width: 280px;">
                    ${this.renderCarCardHtml(carCardData)}
                </div>
                <div class="msg-time">${timeStr}</div>
            `;
        } else {
            // Nếu là tin nhắn text bình thường -> Render text
            wrapper.innerHTML = `
                <div class="msg-bubble">${this.escapeHtml(msg.content)}</div>
                <div class="msg-time">${timeStr}</div>
            `;
        }

        if (isPrepend) {
            this.ui.chatMessages.prepend(wrapper);
        } else {
            this.ui.chatMessages.appendChild(wrapper);
        }
    }

    // === CÁC HÀM XỬ LÝ THẺ XE BỔ SUNG === //
    parseCarCardMessage(content) {
        const prefix = '__CAR_CARD__';
        if (typeof content !== 'string' || !content.startsWith(prefix)) return null;

        try {
            const encoded = content.slice(prefix.length);
            return JSON.parse(decodeURIComponent(encoded));
        } catch (error) {
            console.error('Không parse được card xe:', error);
            return null;
        }
    }

    formatCurrency(value) {
        return Number(value || 0).toLocaleString('vi-VN') + ' đ';
    }

    buildImagePath(imageUrl) {
        if (!imageUrl) return 'https://via.placeholder.com/160x90?text=Car';
        if (imageUrl.startsWith('http')) return imageUrl;
        return `/${imageUrl.replace('car_images', 'car-images')}`;
    }

    renderCarCardHtml(cardData) {
        const detailUrl = `/product_detail?id=${encodeURIComponent(cardData.carId)}`;
        const safeModel = this.escapeHtml(cardData.carModel || 'Xe quan tâm');
        const safeImage = this.escapeHtml(this.buildImagePath(cardData.carImageUrl));

        const metaParts = [
            `Mã xe: #${cardData.carId}`,
            `Giá: ${this.formatCurrency(cardData.carPrice)}`
        ];
        if (cardData.carYear) metaParts.push(`Năm: ${this.escapeHtml(String(cardData.carYear))}`);
        if (cardData.carColor) metaParts.push(`Màu: ${this.escapeHtml(String(cardData.carColor))}`);

        // Giao diện Card thiết kế trực tiếp
        return `
            <div style="display: flex; flex-direction: column;">
                <img src="${safeImage}" alt="${safeModel}" style="width: 100%; height: 160px; object-fit: cover;">
                <div style="padding: 12px;">
                    <h4 style="margin: 0 0 8px 0; font-size: 15px; color: #0f172a;">${safeModel}</h4>
                    <p style="margin: 0 0 12px 0; font-size: 12px; color: #64748b; line-height: 1.5;">${metaParts.join(' • ')}</p>
                    <a href="${detailUrl}" target="_blank" style="color: #ef4444; font-weight: bold; font-size: 14px; text-decoration: none;">Xem chi tiết xe</a>
                </div>
            </div>
        `;
    }

    scrollToBottom() {
        this.ui.chatMessages.scrollTop = this.ui.chatMessages.scrollHeight;
    }

    escapeHtml(text) {
        return text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const app = new ChatApplication();
    app.start();
}); 