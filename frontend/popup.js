document.addEventListener('DOMContentLoaded', () => {
    // Views
    const loggedOutView = document.getElementById('logged-out-view');
    const loggedInView = document.getElementById('logged-in-view');

    // Buttons
    const loginButton = document.getElementById('login-button');
    const logoutButton = document.getElementById('logout-button');
    const connectNotionButton = document.getElementById('connect-notion-button');
    const settingsLink = document.getElementById('settings-link');

    // --- Main UI Update Function ---
    function updatePopupUI() {
        chrome.storage.local.get(['user', 'connectedNotion'], (result) => {
            if (result.user && result.user.isLoggedIn) {
                // --- User is logged in, show dashboard ---
                loggedOutView.style.display = 'none';
                loggedInView.style.display = 'block';

                // Populate user info
                const githubUsernameEl = document.getElementById('github-username');
                githubUsernameEl.textContent = result.user.githubUsername;

                // Populate stats (mocked for now)
                const solvedCountEl = document.getElementById('solved-count');
                solvedCountEl.textContent = '128'; // Mock data

                // Handle Notion connection status
                const notionConnectCard = document.getElementById('notion-connect-card');
                const notionConnectedCard = document.getElementById('notion-connected-card');
                if (result.connectedNotion && result.connectedNotion.status === 'success') {
                    notionConnectCard.style.display = 'none';
                    notionConnectedCard.style.display = 'block';
                    document.getElementById('notion-workspace-name').textContent = result.connectedNotion.workspaceName;
                } else {
                    notionConnectCard.style.display = 'block';
                    notionConnectedCard.style.display = 'none';
                }

            } else {
                // --- User is logged out ---
                loggedOutView.style.display = 'block';
                loggedInView.style.display = 'none';
            }
        });
    }

    // --- Event Listeners ---
    loginButton.addEventListener('click', () => {
        chrome.tabs.create({ url: 'http://localhost:8080/oauth2/authorization/github' });
    });

    logoutButton.addEventListener('click', (e) => {
        e.preventDefault();
        // Clear all local storage for the extension
        chrome.storage.local.clear(() => {
            console.log('All data cleared, logging out.');
            updatePopupUI(); // Re-render the UI to show logged-out view
        });
    });

    connectNotionButton.addEventListener('click', () => {
        chrome.tabs.create({ url: 'http://localhost:8080/auth/notion/login' });
    });

    settingsLink.addEventListener('click', (e) => {
        e.preventDefault();
        chrome.tabs.create({ url: chrome.runtime.getURL('github-integration.html') });
    });

    // --- Real-time listener for changes from other pages (like Notion auth) ---
    chrome.storage.onChanged.addListener((changes, namespace) => {
        // If 'connectedNotion' changes, re-render the whole UI
        if (changes.connectedNotion) {
            console.log('Notion connection status changed, updating UI.');
            updatePopupUI();
        }
    });

    // --- Initial Load ---
    updatePopupUI();
});
