chrome.runtime.onMessageExternal.addListener((message, sender, sendResponse) => {
    if (message.type === 'AUTH_SUCCESS') {
        chrome.storage.local.set({ user: message.data }, () => {
            console.log('User data saved from external message:', message.data);
            sendResponse({ status: 'success' });
        });
        return true; // 비동기 응답을 위해 true를 반환
    }
});