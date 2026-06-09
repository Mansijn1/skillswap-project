'use strict';

const messageForm = document.querySelector('#message-form');
const messageInput = document.querySelector('#message-input');
const messageList = document.querySelector('#message-list');
const fileInput = document.getElementById('file-upload');

let stompClient = null;
let currentUser = window.currentUser || '';
let recipient = window.recipient || '';

function connect() {
    if (!currentUser || !recipient) {
        console.error('Missing user data. Reload page.');
        return;
    }
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, onConnected, onError);
}

function onConnected() {
    stompClient.subscribe(`/user/${currentUser}/topic/private`, onMessageReceived);
    console.log(`WebSocket connected for ${currentUser}. Ready for chat with ${recipient}.`);
}

function onError(error) {
    console.error('WebSocket connection failed:', error);
    const statusElement = document.createElement('div');
    statusElement.textContent = 'Connection lost. Retrying in 5 seconds...';
    statusElement.classList.add('text-red-500', 'text-center', 'py-2');
    document.body.appendChild(statusElement);  // Add to body for visibility
    setTimeout(() => {
        connect();  // Retry connection
        if (statusElement.parentNode) {
            statusElement.parentNode.removeChild(statusElement);  // Remove the message
        }
    }, 5000);
}

function sendMessage(event) {
    event.preventDefault();

    const messageContent = messageInput.value.trim();
    if (messageContent && stompClient) {
        const chatMessage = {
            senderUsername: currentUser,
            recipientUsername: recipient,
            content: messageContent,
            type: 'TEXT'
        };
        stompClient.send('/app/chat.sendMessage', {}, JSON.stringify(chatMessage));
        messageInput.value = '';  // Clear input
    } else {
        console.error('Cannot send message: WebSocket not connected or message is empty.');
    }
}

function onMessageReceived(payload) {
    const message = JSON.parse(payload.body);
    if ((message.senderUsername === recipient && message.recipientUsername === currentUser) ||
        (message.senderUsername === currentUser && message.recipientUsername === recipient)) {
        displayMessage(message);
    }
}

function displayMessage(message) {
    const messageElement = document.createElement('div');
    messageElement.classList.add('inline-block', 'p-2', 'rounded-lg', 'max-w-xs', 'flex', 'flex-col');

    const isSender = message.senderUsername === currentUser;
    if (isSender) {
        messageElement.classList.add('bg-blue-500', 'text-white', 'ml-auto', 'text-right');
    } else {
        messageElement.classList.add('bg-gray-200', 'text-gray-800', 'mr-auto', 'text-left');
    }

    if (message.type === 'FILE' && message.content && message.content.startsWith('/uploads/')) {
        const fileUrl = message.content;
        const fileName = fileUrl.split('/').pop() || 'File';
        const isImage = /\.(jpg|jpeg|png|gif)$/i.test(fileName);

        if (isImage) {
            const img = document.createElement('img');
            img.src = fileUrl;
            img.alt = 'File Preview';
            img.classList.add('message-img', 'mb-1', 'cursor-pointer');
            img.onclick = () => window.open(fileUrl, '_blank');
            img.onerror = () => {
                img.style.display = 'none';
                if (img.nextElementSibling) {
                    img.nextElementSibling.style.display = 'block';
                }
            };
            messageElement.appendChild(img);

            const fallbackLink = document.createElement('a');
            fallbackLink.href = fileUrl;
            fallbackLink.textContent = `📎 Download ${fileName}`;
            fallbackLink.classList.add('file-link', 'hidden', 'underline', 'font-semibold', 'text-sm');
            fallbackLink.target = '_blank';
            messageElement.appendChild(fallbackLink);
        } else {
            const link = document.createElement('a');
            link.href = fileUrl;
            link.textContent = `📎 Download ${fileName}`;
            link.classList.add('file-link', 'underline', 'font-semibold');
            link.target = '_blank';
            messageElement.appendChild(link);
        }
    } else if (message.type === 'TEXT' || !message.type) {
        const contentElement = document.createElement('p');
        contentElement.textContent = message.content;
        messageElement.appendChild(contentElement);
    } else {
        const contentElement = document.createElement('p');
        contentElement.textContent = message.content || 'Unsupported message';
        contentElement.classList.add('text-gray-500', 'italic');
        messageElement.appendChild(contentElement);
    }

    if (message.timestamp) {
        const timeElement = document.createElement('span');
        timeElement.classList.add('text-xs', 'text-gray-400', isSender ? 'text-right' : 'text-left', 'mt-1');
        let date;
        try {
            date = new Date(message.timestamp);
        } catch (e) {
            date = new Date();
        }
        if (!isNaN(date.getTime())) {
            timeElement.textContent = date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true });
        }
        messageElement.appendChild(timeElement);
    }

    messageList.appendChild(messageElement);
    messageList.scrollTop = messageList.scrollHeight;
}

function openFile(element) {
    const fileUrl = element.getAttribute('data-file-url');
    if (fileUrl) {
        window.open(fileUrl, '_blank');
    }
}

function uploadFile(file) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('recipient', recipient);

    const tempMessageDiv = document.createElement('div');
    tempMessageDiv.textContent = `📎 Uploading ${file.name}...`;
    tempMessageDiv.classList.add('temporary-upload-message', 'text-gray-500', 'italic', 'text-center', 'py-2');
    messageList.appendChild(tempMessageDiv);
    messageList.scrollTop = messageList.scrollHeight;

    fetch('/chat/upload', {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Upload failed.');
        }
        return response.text();
    })
    .then(fileUrl => {
        if (tempMessageDiv) {
            tempMessageDiv.remove();
        }
        const fileMessage = {
            senderUsername: currentUser,
            recipientUsername: recipient,
            content: fileUrl,
            type: 'FILE',
            timestamp: new Date().toISOString()  // Added timestamp for consistency
        };
        // The message will be displayed via WebSocket broadcast
    })
    .catch(error => {
        console.error('Upload error:', error);
        if (tempMessageDiv) {
            tempMessageDiv.remove();
        }
        alert("Upload failed. Please try again.");
    });
}

document.addEventListener('DOMContentLoaded', () => {
    connect();
    if (messageList) {
        messageList.scrollTop = messageList.scrollHeight;
    }
    if (messageForm) {
        messageForm.addEventListener('submit', sendMessage);
    }
    if (fileInput) {
        fileInput.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (file) {
                uploadFile(file);
            }
        });
    }
});