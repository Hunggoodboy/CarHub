// ===================== CHAT WIDGET =====================
const ChatWidget = (() => {

    const carCardPrefix = '__CAR_CARD__';

    let stompClient = null;
    let currentUserId = null;
    let sellerId = null;
    let sellerName = null;
    let isConnected = false;

    async function init(sellerIdParam, sellerNameParam) {
        sellerId = sellerIdParam;
        sellerName = sellerNameParam || 'Người bán';

        try {
            const res = await fetch('/api/users/me');
            if (!res.ok) return;
            currentUserId = await res.json();
        } catch (e) {
            console.log('Chưa đăng nhập');
            return;
        }

        renderWidget();
        document.getElementById('chat-bubble').style.display = 'flex';
    }

    function renderWidget() {
        const existing = document.getElementById('chat-widget');
        if (existing) existing.remove();
        const existingBubble = document.getElementById('chat-bubble');
        if (existingBubble) existingBubble.remove();

        const initial = (sellerName || 'S').charAt(0).toUpperCase();

        document.body.insertAdjacentHTML('beforeend', `
            <div id="chat-bubble" title="Tư vấn với người bán" onclick="ChatWidget.openWidget();">
                <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 12H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"/>
                </svg>
                <div class="chat-badge"></div>
            </div>

            <div id="chat-widget">
                <div class="chat-widget-header">
                    <div class="chat-widget-avatar">${initial}</div>
                    <div class="chat-widget-info">
                        <div class="chat-widget-name">${sellerName}</div>
                        <div class="chat-widget-status" id="cw-status">Đang kết nối...</div>
                    </div>
                    <button class="chat-widget-close" onclick="ChatWidget.closeWidget()">✕</button>
                </div>

                <div class="chat-connect-banner" id="cw-banner">
                    <span id="cw-banner-text">⚡ Chưa kết nối WebSocket</span>
                    <button class="chat-connect-btn" id="cw-banner-btn" onclick="ChatWidget.connect()">Kết nối</button>
                </div>

                <div id="chat-widget-messages">
                    <div class="chat-empty" id="cw-empty">
                        <div class="chat-empty-icon">💬</div>
                        <div class="chat-empty-text">Bắt đầu cuộc trò chuyện với<br><strong>${sellerName}</strong></div>
                    </div>
                </div>

                <div class="quick-replies" id="quick-replies">
                    <button onclick="ChatWidget.sendQuickMsg('Xin chào!!')">Xin chào </button>
                    <button onclick="ChatWidget.sendQuickMsg('Xe này còn không shop?')">Xe này còn không?</button>
                    <button onclick="ChatWidget.sendQuickMsg('Giá có bớt không ạ?')">Có bớt giá không?</button>
                    <button onclick="ChatWidget.sendQuickMsg('Địa chỉ xem xe ở đâu?')">Xem xe ở đâu?</button>
                    <button onclick="ChatWidget.sendQuickMsg('Cho tôi ít thông tin về xe này được không?')">Cho tôi ít thông tin về xe này được không?</button>
                </div>

                <div class="chat-widget-input">
                    <div class="chat-input-row">
                        <textarea id="chat-widget-input-text" rows="1" placeholder="Nhập tin nhắn..."></textarea>
                        <button id="chat-widget-send" onclick="ChatWidget.sendMessage()">
                            <svg viewBox="0 0 24 24"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>
                        </button>
                    </div>
                </div>
            </div>
        `);

        document.getElementById('chat-widget-input-text').addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                ChatWidget.sendMessage();
            }
        });

        document.getElementById('chat-widget-input-text').addEventListener('input', function () {
            this.style.height = 'auto';
            this.style.height = Math.min(this.scrollHeight, 80) + 'px';
        });
    }

    function formatCurrency(value) {
        return Number(value || 0).toLocaleString('vi-VN') + ' đ';
    }

    function buildImagePath(imageUrl) {
        if (!imageUrl) {
            return 'https://via.placeholder.com/160x90?text=Car';
        }

        if (imageUrl.startsWith('http')) {
            return imageUrl;
        }

        return `/${imageUrl.replace('car_images', 'car-images')}`;
    }

    function buildCarCardMessageFromSession() {
        const raw = sessionStorage.getItem('pending_car_info');
        if (!raw) {
            return '';
        }

        try {
            const parsed = JSON.parse(raw);
            if (!parsed || !parsed.carId) {
                return '';
            }

            const payload = {
                carId: Number(parsed.carId),
                carModel: parsed.carModel || 'Xe quan tâm',
                carPrice: Number(parsed.carPrice || 0),
                carImageUrl: parsed.carImageUrl || '',
                carYear: parsed.carYear || '',
                carColor: parsed.carColor || ''
            };

            return carCardPrefix + encodeURIComponent(JSON.stringify(payload));
        } catch (error) {
            console.error('Dữ liệu xe không hợp lệ:', error);
            return '';
        }
    }

    function parseCarCardMessage(content) {
        if (typeof content !== 'string' || !content.startsWith(carCardPrefix)) {
            return null;
        }

        try {
            const encoded = content.slice(carCardPrefix.length);
            const parsed = JSON.parse(decodeURIComponent(encoded));
            if (!parsed || !parsed.carId) {
                return null;
            }

            return {
                carId: Number(parsed.carId),
                carModel: parsed.carModel || 'Xe quan tâm',
                carPrice: Number(parsed.carPrice || 0),
                carImageUrl: parsed.carImageUrl || '',
                carYear: parsed.carYear || '',
                carColor: parsed.carColor || ''
            };
        } catch (error) {
            console.error('Không parse được card xe:', error);
            return null;
        }
    }

    function renderCarCardHtml(cardData) {
        const detailUrl = `/product_detail?id=${encodeURIComponent(cardData.carId)}`;
        const safeModel = escapeHtml(cardData.carModel || 'Xe quan tâm');
        const safeImage = escapeHtml(buildImagePath(cardData.carImageUrl));
        const metaParts = [
            `Mã xe: #${cardData.carId}`,
            `Giá: ${formatCurrency(cardData.carPrice)}`
        ];

        if (cardData.carYear) {
            metaParts.push(`Năm: ${escapeHtml(String(cardData.carYear))}`);
        }
        if (cardData.carColor) {
            metaParts.push(`Màu: ${escapeHtml(String(cardData.carColor))}`);
        }

        return `
            <div class="chat-widget-car-card">
                <img src="${safeImage}" alt="${safeModel}">
                <div class="chat-widget-car-card-content">
                    <h4>${safeModel}</h4>
                    <p>${metaParts.join(' • ')}</p>
                    <a href="${detailUrl}" target="_blank" rel="noopener noreferrer">Xem chi tiết xe</a>
                </div>
            </div>
        `;
    }

    function publishMessage(content) {
        if (!content || !isConnected) {
            return;
        }

        stompClient.publish({
            destination: '/app/chat.private',
            body: JSON.stringify({
                senderId: currentUserId,
                receiverId: sellerId,
                content: content,
                messageType: 'TEXT',
                chatType: 'PRIVATE'
            })
        });
    }

    function openWidget() {
        const widget = document.getElementById('chat-widget');
        const bubble = document.getElementById('chat-bubble');
        if (!widget) return;
        widget.style.display = 'flex';
        bubble.style.display = 'none';
        if (!isConnected) connect();
    }

    function closeWidget() {
        document.getElementById('chat-widget').style.display = 'none';
        document.getElementById('chat-bubble').style.display = 'flex';
    }

    function connect() {
        if (isConnected) return;
        stompClient = new StompJs.Client({
            webSocketFactory: () => new SockJS('/gs-guide-websocket')
        });
        stompClient.onConnect = (frame) => {
            isConnected = true;
            updateConnectionUI(true);
            stompClient.subscribe('/user/queue/private', (message) => {
                const msg = JSON.parse(message.body);
                if (msg.senderId == sellerId || msg.receiverId == sellerId) {
                    showMessage(msg);
                }
            });
            stompClient.subscribe('/user/queue/history', (message) => {
                const messages = JSON.parse(message.body);
                messages.forEach(msg => showMessage(msg));
            });
            stompClient.publish({
                destination: '/app/chat.history',
                body: JSON.stringify({
                    senderId: currentUserId,
                    receiverId: sellerId
                })
            });
        };
        stompClient.onWebSocketError = () => { isConnected = false; updateConnectionUI(false); };
        stompClient.onStompError = () => { isConnected = false; updateConnectionUI(false); };
        stompClient.onDisconnect = () => { isConnected = false; updateConnectionUI(false); };
        stompClient.activate();
    }

    function updateConnectionUI(connected) {
        const banner = document.getElementById('cw-banner');
        const status = document.getElementById('cw-status');
        if (connected) {
            banner.style.display = 'none';
            status.textContent = 'Online';
            status.style.color = '#22c55e';
        } else {
            banner.style.display = 'flex';
            banner.style.background = '#ffebee';
            document.getElementById('cw-banner-text').textContent = '⚠️ Mất kết nối...';
            document.getElementById('cw-banner-btn').textContent = 'Thử lại';
            status.textContent = 'Offline';
            status.style.color = '#ff4d4f';
        }
    }

    function disconnect() {
        if (stompClient) stompClient.deactivate();
        isConnected = false;
        updateConnectionUI(false);
    }

    function sendMessage() {
        const input = document.getElementById('chat-widget-input-text');
        let content = input.value.trim();

        if (!content) return;
        if (!isConnected) {
            alert('Chưa kết nối WebSocket!');
            return;
        }

        const carCardMessage = buildCarCardMessageFromSession();
        if (carCardMessage) {
            publishMessage(carCardMessage);
            sessionStorage.removeItem('pending_car_info');
        }

        publishMessage(content);

        input.value = '';
        input.style.height = 'auto';
    }

    function sendQuickMsg(text) {
        const input = document.getElementById('chat-widget-input-text');
        if (input) {
            input.value = text;
            ChatWidget.sendMessage();
        }
    }

    function showMessage(msg) {
        const container = document.getElementById('chat-widget-messages');
        const empty = document.getElementById('cw-empty');
        if (empty) empty.style.display = 'none';
        const isMe = msg.senderId == currentUserId;
        const carCardData = parseCarCardMessage(msg.content);
        const time = msg.sentAt
            ? new Date(msg.sentAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
            : new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
        const wrapper = document.createElement('div');
        wrapper.className = `chat-msg-wrapper ${isMe ? 'me' : 'them'}`;

        if (carCardData) {
            wrapper.innerHTML = `<div class="chat-msg-bubble car-card-bubble">${renderCarCardHtml(carCardData)}</div><div class="chat-msg-time">${time}</div>`;
        } else {
            wrapper.innerHTML = `<div class="chat-msg-bubble">${escapeHtml(msg.content)}</div><div class="chat-msg-time">${time}</div>`;
        }
        container.appendChild(wrapper);
        container.scrollTop = container.scrollHeight;
        if (document.getElementById('chat-widget').style.display === 'none') {
            document.querySelector('.chat-badge').style.display = 'block';
        }
    }

    function escapeHtml(text) {
        return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    return { init, openWidget, closeWidget, connect, disconnect, sendMessage, sendQuickMsg };
})();