function createChatContainerIfMissing() {
    if (document.getElementById('chat-container')) {
        return;
    }

    // Only create the chat panel once. Trigger button is managed by each page.
    const chatContainerHTML = `
        <div id="chat-container" class="chat-hidden">
            <div id="header">
                Spring AI Assistant
                <span onclick="toggleChat()" style="float:right; cursor:pointer; font-size: 20px;">&times;</span>
            </div>
            <div id="messages">
                <div class="message bot">Xin chào! CarHub có thể giúp gì cho bạn?</div>
            </div>
            <div id="input-area">
                <input type="text" id="userInput" placeholder="Nhập tin nhắn..." onkeypress="handleKeyPress(event)">
                <button onclick="sendMessage()" id="sendBtn">Gửi</button>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', chatContainerHTML);
}

document.addEventListener('DOMContentLoaded', createChatContainerIfMissing);

// Hàm quan trọng để hiện/ẩn khung chat
function toggleChat() {
    createChatContainerIfMissing();
    const chatContainer = document.getElementById('chat-container');
    chatContainer.classList.toggle('chat-hidden');
}

function scrollMessagesToBottom() {
    const messages = document.getElementById('messages');
    if (!messages) {
        return;
    }

    messages.scrollTop = messages.scrollHeight;
}

function createMessageElement(role) {
    const messages = document.getElementById('messages');
    if (!messages) {
        return null;
    }

    const bubble = document.createElement('div');
    bubble.className = `message ${role}`;
    messages.appendChild(bubble);
    scrollMessagesToBottom();
    return bubble;
}

function appendMessage(role, text) {
    const bubble = createMessageElement(role);
    if (!bubble) {
        return;
    }

    bubble.innerHTML = formatMessage(text);
    scrollMessagesToBottom();
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function createTypingIndicator() {
    const bubble = createMessageElement('bot');
    if (!bubble) {
        return null;
    }

    bubble.classList.add('typing-indicator');
    bubble.innerHTML = '<span></span><span></span><span></span>';
    return bubble;
}

async function typeBotMessage(text) {
    const content = (text ?? '').toString();
    const bubble = createMessageElement('bot');
    if (!bubble) {
        return;
    }

    const delay = content.length > 300 ? 8 : 16;
    const step = content.length > 300 ? 2 : 1;

    for (let i = 0; i < content.length; i += step) {
        bubble.innerHTML = formatMessage(content.slice(0, i + step));
        scrollMessagesToBottom();
        await sleep(delay);
    }
}

function formatMessage(text) {
    return (text ?? '').toString()
        .replace(/\n/g, '<br>')
        .replace(
            /(https?:\/\/[^\s<]+)/g,
            '<a href="$1" target="_blank" style="color:#3b82f6; text-decoration:underline;">Xem chi tiết sản phẩm</a>'
        );
}

async function sendMessage() {
    const input = document.getElementById('userInput');
    const sendBtn = document.getElementById('sendBtn');
    if (!input) return;

    const question = input.value.trim();
    if (!question) return;

    appendMessage('user', question);
    input.value = '';
    input.disabled = true;
    if (sendBtn) sendBtn.disabled = true;
    let typingIndicator = null;

    try {
        typingIndicator = createTypingIndicator();
        const response = await fetch('/ChatAI', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ question })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const answer = await response.text();

        if (typingIndicator) {
            await sleep(220);
            typingIndicator.remove();
            typingIndicator = null;
        }

        await typeBotMessage(answer);
    } catch (error) {
        if (typingIndicator) {
            typingIndicator.remove();
        }
        appendMessage('bot', 'Xin loi, khong the ket noi den tro ly AI luc nay.');
        console.error('ChatAI request failed:', error);
    } finally {
        input.disabled = false;
        if (sendBtn) sendBtn.disabled = false;
        input.focus();
    }
}

function handleKeyPress(e) {
    if (e.key === 'Enter') {
        sendMessage();
    }
}