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

function appendMessage(role, text) {
    const messages = document.getElementById('messages');
    if (!messages) return;

    const safeText = (text ?? '').toString();
    messages.innerHTML += `<div class="message ${role}">${safeText}</div>`;
    messages.scrollTop = messages.scrollHeight;
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

    try {
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
        appendMessage('bot', answer || 'Mình chưa có câu trả lời phù hợp lúc này.');
    } catch (error) {
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