// 1. Hàm gửi tin nhắn
async function sendMessage() {
    const inputField = document.getElementById('userInput');
    const sendBtn = document.getElementById('sendBtn');
    const text = inputField.value.trim();

    if (!text) return;

    // Hiển thị tin nhắn người dùng
    appendMessage('user', text);

    // Reset ô nhập liệu và khóa nút
    inputField.value = '';
    inputField.disabled = true;
    sendBtn.disabled = true;

    try {
        // Gửi POST request về Spring Boot
        const response = await fetch('/ai', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ message: text })
        });

        if (!response.ok) throw new Error("Lỗi mạng hoặc lỗi Server");

        const data = await response.text();

        // Hiển thị phản hồi của AI
        appendMessage('bot', data);

    } catch (error) {
        appendMessage('bot', 'Lỗi: Không thể kết nối tới Server!');
        console.error(error);
    } finally {
        // Mở khóa lại nút
        inputField.disabled = false;
        sendBtn.disabled = false;
        inputField.focus();
    }
}

// 2. Hàm hỗ trợ hiển thị tin nhắn lên màn hình (BẠN ĐANG THIẾU CÁI NÀY)
function appendMessage(sender, text) {
    const messageContainer = document.getElementById('messages');
    const msgDiv = document.createElement('div');

    // Thêm class css: 'message' và 'user' hoặc 'bot'
    msgDiv.classList.add('message', sender);
    msgDiv.innerText = text;

    messageContainer.appendChild(msgDiv);

    // Tự động cuộn xuống dưới cùng
    messageContainer.scrollTop = messageContainer.scrollHeight;
}

// 3. Hàm xử lý khi nhấn Enter
function handleKeyPress(e) {
    if (e.key === 'Enter') sendMessage();
}