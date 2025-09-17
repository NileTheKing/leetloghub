let latestAuthToken = null; // Global variable to hold the most recent token

// --- Initialization: Load token from storage on startup ---
chrome.storage.local.get(['authToken'], (result) => {
    if (result.authToken) {
        latestAuthToken = result.authToken;
        console.log('Auth token loaded from storage into memory.');
    }
});

// --- Listeners ---

chrome.runtime.onMessageExternal.addListener((message, sender, sendResponse) => {
    if (message.type === 'GITHUB_AUTH_SUCCESS') {
        handleGitHubAuthSuccess(message.data, sender.tab);
        sendResponse({ status: 'success' });
    } else if (message.type === 'NOTION_AUTH_SUCCESS') {
        handleNotionAuthSuccess(message.data, sender.tab);
        sendResponse({ status: 'success' });
    }
    return true;
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.type === 'LEETCODE_SUBMISSION_SUCCESS') {
        console.log('Received successful submission data from interceptor:', message.data);

        // Use the in-memory token directly to avoid race conditions
        if (!latestAuthToken) {
            console.error('Auth token not found in memory. User might not be logged in.');
            sendResponse({ status: 'error', message: 'Authentication token not found.' });
            return;
        }

        fetch('http://localhost:8080/api/solves', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${latestAuthToken}`
            },
            body: JSON.stringify(message.data)
        })
        .then(response => {
            if (response.ok) {
                sendResponse({ status: 'success' });
            } else {
                response.text().then(text => {
                    console.error('Error sending solve to backend:', response.status, text);
                    sendResponse({ status: 'error', message: `Server responded with ${response.status}` });
                });
            }
        })
        .catch(error => {
            console.error('Error sending solve to backend:', error);
            sendResponse({ status: 'error', message: error.message });
        });

        return true; // Indicates that the response is sent asynchronously
    }
});


// --- Handler Functions ---

function handleGitHubAuthSuccess(data, senderTab) {
    console.log('GitHub auth success:', data);
    const userData = {
        isLoggedIn: true,
        githubUsername: data.user
    };
    
    // Update both the in-memory token and the persistent storage
    latestAuthToken = data.token;
    chrome.storage.local.set({ user: userData, authToken: data.token }, () => {
        console.log('User data and auth token saved. Now opening setup page.');
        
        chrome.tabs.create({
            url: chrome.runtime.getURL('github-integration.html')
        });

        if (senderTab && senderTab.id) {
            chrome.tabs.get(senderTab.id, (tab) => {
                if (tab) { 
                    chrome.tabs.remove(senderTab.id);
                }
            });
        }
    });
}

function handleNotionAuthSuccess(data, senderTab) {
    console.log('Notion auth success:', data);
    chrome.storage.local.set({ connectedNotion: data }, () => {
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

        if (senderTab && senderTab.id) {
            chrome.tabs.get(senderTab.id, (tab) => {
                if (tab) {
                    chrome.tabs.remove(senderTab.id);
                }
            });
        }
    });
}