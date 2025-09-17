/**
 * This script acts as a bridge between the MAIN world (interceptor.js) 
 * and the extension's service worker (background.js).
 */
window.addEventListener('message', (event) => {
    // We only accept messages from ourselves
    if (event.source !== window) {
        return;
    }

    const { type, data } = event.data;

    if (type === 'LEETLOG_SUBMISSION_DATA') {
        console.log('[LeetLogHub Bridge] Received data from MAIN world, forwarding to background script...');
        chrome.runtime.sendMessage({ type: 'LEETCODE_SUBMISSION_SUCCESS', data });
    }
});
