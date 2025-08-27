chrome.runtime.onMessageExternal.addListener((message, sender, sendResponse) => {
    if (message.type === 'GITHUB_AUTH_SUCCESS') {
        handleGitHubAuthSuccess(message.data, sender.tab);
        sendResponse({ status: 'success' });
    } else if (message.type === 'NOTION_AUTH_SUCCESS') {
        handleNotionAuthSuccess(message.data, sender.tab);
        sendResponse({ status: 'success' });
    }
    return true; // Keep message channel open for async operations
});

function handleGitHubAuthSuccess(data, senderTab) {
    console.log('GitHub auth success:', data);
    const userData = {
        isLoggedIn: true,
        githubUsername: data.user
    };
    chrome.storage.local.set({ user: userData }, () => {
        console.log('User data saved to chrome.storage.local.');
        // Open the setup page in a new tab
        chrome.tabs.create({
            url: chrome.runtime.getURL('github-integration.html')
        });
        // Close the original auth tab
        if (senderTab && senderTab.id) {
            chrome.tabs.remove(senderTab.id);
        }
    });
}

function handleNotionAuthSuccess(data, senderTab) {
    console.log('Notion auth success:', data);
    // Save notion connection status
    chrome.storage.local.set({ connectedNotion: data }, () => {
        // Notify the github-integration tab to update its UI
        chrome.tabs.query({ url: chrome.runtime.getURL('github-integration.html') }, (tabs) => {
            if (tabs.length > 0) {
                chrome.tabs.sendMessage(tabs[0].id, {
                    type: 'NOTION_CONNECTION_SUCCESS',
                    data: data
                });
            } else {
                console.error('Could not find github-integration.html tab to notify.');
            }
        });
        // Close the original auth tab
        if (senderTab && senderTab.id) {
            chrome.tabs.remove(senderTab.id);
        }
    });
}