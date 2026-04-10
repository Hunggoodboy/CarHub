class ChatApplication {
    constructor() {
        this.carCardPrefix = '__CAR_CARD__';
        // Thuộc tính trạng thái (State)
        this.currentUserId = null;
        this.currentPartnerId = null;
        this.stompClient = null;
        this.currentPage = 0;
        this.pageSize = 20;
        this.reconnectAttempts = 0;
        this.isConnecting = false;
        this.scheduleIntent = false;
        this.scheduleCarId = null;
        this.scheduleCarModel = '';
        this.scheduleCarPrice = 0;
        this.scheduleCarImageUrl = '';

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
            connectionStatus: document.getElementById('connection-status'),
            scheduler: document.getElementById('consultation-scheduler'),
            schedulerDateTime: document.getElementById('consultation-datetime'),
            schedulerConfirm: document.getElementById('btn-schedule-confirm'),
            schedulerCancel: document.getElementById('btn-schedule-cancel')
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

        this.scheduleIntent = this.getScheduleIntentFromUrl();
        this.loadScheduleCarInfoFromUrl();
        this.attachEventListeners();
        this.connectWebSocket();
        await this.loadRecentChats();

        const partnerIdFromUrl = this.getPartnerIdFromUrl();
        if (partnerIdFromUrl) {
            await this.openPartnerFromUrl(partnerIdFromUrl);
        }
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
        this.ui.btnLike.addEventListener('click', () => this.sendLike());
        if (this.ui.schedulerConfirm) {
            this.ui.schedulerConfirm.addEventListener('click', () => this.confirmConsultationSchedule());
        }
        if (this.ui.schedulerCancel) {
            this.ui.schedulerCancel.addEventListener('click', () => this.hideSchedulePanel());
        }
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

    getPartnerIdFromUrl() {
        const params = new URLSearchParams(window.location.search);
        const partnerId = Number(params.get('partnerId'));
        return Number.isFinite(partnerId) && partnerId > 0 ? partnerId : null;
    }

    getScheduleIntentFromUrl() {
        const params = new URLSearchParams(window.location.search);
        return params.get('intent') === 'schedule';
    }

    loadScheduleCarInfoFromUrl() {
        const params = new URLSearchParams(window.location.search);
        const rawCarId = Number(params.get('carId'));
        const rawCarModel = (params.get('carModel') || '').trim();
        const rawCarPrice = Number(params.get('carPrice'));
        const rawCarImageUrl = (params.get('carImageUrl') || '').trim();

        this.scheduleCarId = Number.isFinite(rawCarId) && rawCarId > 0 ? rawCarId : null;
        this.scheduleCarModel = rawCarModel;
        this.scheduleCarPrice = Number.isFinite(rawCarPrice) && rawCarPrice >= 0 ? rawCarPrice : 0;
        this.scheduleCarImageUrl = rawCarImageUrl;
    }

    buildImagePath(imageUrl) {
        if (!imageUrl) {
            return 'https://via.placeholder.com/160x90?text=Car';
        }

        if (imageUrl.startsWith('http')) {
            return imageUrl;
        }

        return `/${imageUrl.replace('car_images', 'car-images')}`;
    }

    formatCurrency(value) {
        return Number(value || 0).toLocaleString('vi-VN') + ' đ';
    }

    buildCarCardMessage() {
        if (!this.scheduleCarId) {
            return '';
        }

        const payload = {
            carId: this.scheduleCarId,
            carModel: this.scheduleCarModel || 'Xe quan tâm',
            carPrice: this.scheduleCarPrice || 0,
            carImageUrl: this.scheduleCarImageUrl || ''
        };

        return this.carCardPrefix + encodeURIComponent(JSON.stringify(payload));
    }

    parseCarCardMessage(content) {
        if (typeof content !== 'string' || !content.startsWith(this.carCardPrefix)) {
            return null;
        }

        const encodedPayload = content.slice(this.carCardPrefix.length);
        if (!encodedPayload) {
            return null;
        }

        try {
            const parsed = JSON.parse(decodeURIComponent(encodedPayload));
            if (!parsed || !parsed.carId) {
                return null;
            }

            return {
                carId: Number(parsed.carId),
                carModel: parsed.carModel || 'Xe quan tâm',
                carPrice: Number(parsed.carPrice || 0),
                carImageUrl: parsed.carImageUrl || ''
            };
        } catch (error) {
            console.error('Không parse được card xe:', error);
            return null;
        }
    }

    renderCarCardHtml(cardData) {
        const safeModel = this.escapeHtml(cardData.carModel || 'Xe quan tâm');
        const detailUrl = `/product_detail?id=${encodeURIComponent(cardData.carId)}`;
        const imageUrl = this.escapeHtml(this.buildImagePath(cardData.carImageUrl));
        const summaryText = `Mã xe: #${cardData.carId} • Giá tham khảo: ${this.formatCurrency(cardData.carPrice)}`;

        return `
            <div class="car-link-card">
                <img src="${imageUrl}" alt="${safeModel}">
                <div class="car-link-card-content">
                    <h4>${safeModel}</h4>
                    <p>${this.escapeHtml(summaryText)}</p>
                    <a href="${detailUrl}" target="_blank" rel="noopener noreferrer">Xem chi tiết xe</a>
                </div>
            </div>
        `;
    }

    showSchedulePanel() {
        if (!this.ui.scheduler || !this.ui.schedulerDateTime) {
            return;
        }

        const now = new Date();
        now.setMinutes(now.getMinutes() + 30);
        now.setSeconds(0, 0);

        const minValue = now.toISOString().slice(0, 16);
        this.ui.schedulerDateTime.min = minValue;
        if (!this.ui.schedulerDateTime.value) {
            this.ui.schedulerDateTime.value = minValue;
        }

        this.ui.scheduler.style.display = 'block';
    }

    hideSchedulePanel() {
        if (this.ui.scheduler) {
            this.ui.scheduler.style.display = 'none';
        }

        this.scheduleIntent = false;
        const url = new URL(window.location.href);
        url.searchParams.delete('intent');
        window.history.replaceState({}, '', url.toString());
    }

    confirmConsultationSchedule() {
        if (!this.currentPartnerId || !this.ui.schedulerDateTime) {
            return;
        }

        const selectedValue = this.ui.schedulerDateTime.value;
        if (!selectedValue) {
            alert('Vui lòng chọn ngày giờ tư vấn.');
            return;
        }

        const selectedDate = new Date(selectedValue);
        const now = new Date();
        if (Number.isNaN(selectedDate.getTime()) || selectedDate <= now) {
            alert('Vui lòng chọn thời gian hợp lệ trong tương lai.');
            return;
        }

        const dateText = selectedDate.toLocaleDateString('vi-VN');
        const timeText = selectedDate.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
        const carInfoParts = [];
        if (this.scheduleCarModel) {
            carInfoParts.push(`xe ${this.scheduleCarModel}`);
        }
        if (this.scheduleCarId) {
            carInfoParts.push(`mã #${this.scheduleCarId}`);
        }

        const carInfoText = carInfoParts.length > 0 ? ` cho ${carInfoParts.join(' - ')}` : '';
        const scheduleMessage = `Em muốn đặt lịch tư vấn trực tiếp${carInfoText} vào lúc ${timeText} ngày ${dateText}.`;

        const carCardMessage = this.buildCarCardMessage();
        if (carCardMessage) {
            this.sendTextMessage(carCardMessage);
        }
        this.sendTextMessage(scheduleMessage);
        this.hideSchedulePanel();
    }

    async openPartnerFromUrl(partnerId) {
        try {
            const response = await fetch(`/api/users/${partnerId}`);
            if (!response.ok) {
                return;
            }

            const partner = await response.json();
            const partnerName = partner.fullName || partner.username || `Người bán ${partnerId}`;
            let listItemElement = document.querySelector(`.chat-item[data-partner-id="${partnerId}"]`);

            if (!listItemElement) {
                listItemElement = document.createElement('li');
                listItemElement.className = 'chat-item';
                listItemElement.dataset.partnerId = String(partnerId);
                listItemElement.innerHTML = `
                    <div class="avatar">${partnerName.charAt(0).toUpperCase()}</div>
                    <div class="chat-item-details">
                        <h4 class="user-name">${this.escapeHtml(partnerName)}</h4>
                        <p>Cuộc trò chuyện mới</p>
                    </div>
                `;
                this.ui.recentChatsList.prepend(listItemElement);
                listItemElement.addEventListener('click', () => this.selectPartner(partnerId, partnerName, listItemElement));
            }

            await this.selectPartner(partnerId, partnerName, listItemElement);
            if (this.scheduleIntent) {
                this.showSchedulePanel();
            }
        } catch (error) {
            console.error('Không mở được cuộc trò chuyện từ URL:', error);
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
                li.dataset.partnerId = String(chat.partnerId);
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
        if (listItemElement) listItemElement.classList.add('active');

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
        if (!content) return;

        this.sendTextMessage(content);
        this.ui.chatInput.value = '';
    }

    sendTextMessage(content) {
        if (!content || !this.currentPartnerId || !this.stompClient) {
            return;
        }

        const chatMessageRequest = {
            senderId: this.currentUserId,
            receiverId: this.currentPartnerId,
            content: content
        };

        this.stompClient.send('/app/chat.private', {}, JSON.stringify(chatMessageRequest));
    }

    handleKeyPress(event) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.sendMessage();
        }
    }
    sendLike() {
        if (!this.currentPartnerId || !this.stompClient) return;
        this.sendTextMessage('👍');
    }

    // Render 1 bong bóng tin nhắn ra màn hình
    renderMessage(msg, isPrepend) {
        const isMe = msg.senderId === this.currentUserId;
        const timeStr = new Date(msg.sentAt || Date.now()).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
        const carCardData = this.parseCarCardMessage(msg.content);

        const wrapper = document.createElement('div');
        wrapper.className = `msg-wrapper ${isMe ? 'me' : 'them'}`;

        if (carCardData) {
            wrapper.innerHTML = `
                <div class="msg-bubble car-card-bubble">${this.renderCarCardHtml(carCardData)}</div>
                <div class="msg-time">${timeStr}</div>
            `;
        } else {
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
