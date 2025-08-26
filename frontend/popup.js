const loggedOutView = document.getElementById('logged-out-view');
const loggedInView = document.getElementById('logged-in-view');
const loginButton = document.getElementById('login-button');
const logoutButton = document.getElementById('logout-button');
const connectNotionButton = document.getElementById('connect-notion-button');
const welcomeMessage = document.getElementById('welcome-message');

// Function to update the UI based on login state
function updateUI(userData) {
    if (userData && userData.isLoggedIn) {
        loggedOutView.style.display = 'none';
        loggedInView.style.display = 'block';
        welcomeMessage.textContent = `Welcome, ${userData.githubUsername}!`;
    } else {
        loggedOutView.style.display = 'block';
        loggedInView.style.display = 'none';
    }
}

// --- Event Listeners ---

// Add click listener for the main login button
loginButton.addEventListener('click', () => {
    chrome.tabs.create({ url: 'http://localhost:8080/oauth2/authorization/github' });
});

// Add click listener for the Notion connect button
connectNotionButton.addEventListener('click', () => {
    chrome.storage.local.get('user', (result) => {
        if (result.user && result.user.githubUsername) {
            const notionLoginUrl = `http://localhost:8080/auth/notion/login?user=${result.user.githubUsername}`;
            chrome.tabs.create({ url: notionLoginUrl });
        } else {
            console.error('Could not find GitHub username to connect to Notion.');
        }
    });
});

// Add click listener for the logout button
logoutButton.addEventListener('click', () => {
    chrome.storage.local.remove('user', () => {
        updateUI(null);
    });
});

// --- Initial Check ---

// When the popup opens, check chrome.storage to see if the user is already logged in
document.addEventListener('DOMContentLoaded', () => {
    chrome.storage.local.get('user', (result) => {
        updateUI(result.user);
    });
});
