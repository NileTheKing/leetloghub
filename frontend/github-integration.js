document.addEventListener('DOMContentLoaded', () => {
    let userData;

    // --- Element Selectors ---
    const welcomeMessage = document.getElementById('welcome-message');
    // GitHub
    const repoLoading = document.getElementById('repo-loading');
    const repoSelection = document.getElementById('repo-selection');
    const repoConnected = document.getElementById('repo-connected');
    const connectedRepoName = document.getElementById('connected-repo-name');
    const repoList = document.getElementById('repo-list');
    const connectRepoButton = document.getElementById('connect-repo-button');
    const createRepoButton = document.getElementById('create-repo-button');
    const newRepoNameInput = document.getElementById('new-repo-name');
    // Notion
    const notionDisconnected = document.getElementById('notion-disconnected');
    const notionConnected = document.getElementById('notion-connected');
    const connectNotionButton = document.getElementById('connect-notion-button');
    const notionWorkspaceName = document.getElementById('notion-workspace-name');
    const pageLoading = document.getElementById('page-loading');
    const pageSelectionControls = document.getElementById('page-selection-controls');
    const pageList = document.getElementById('page-list');
    const createDbButton = document.getElementById('create-db-button');
    const notionDbCreated = document.getElementById('notion-db-created');


    // --- Initialization ---
    chrome.storage.local.get(['user', 'connectedRepo', 'connectedNotion', 'dbCreated'], (result) => {
        if (!result.user || !result.user.isLoggedIn) {
            document.body.innerHTML = '<h1>Error: You are not logged in. Please log in through the extension popup.</h1>';
            return;
        }
        userData = result.user;
        welcomeMessage.textContent = `Welcome, ${userData.githubUsername}! Let's get you set up.`;

        // Check repo connection status
        if (result.connectedRepo) {
            showConnectedRepo(result.connectedRepo);
        } else {
            fetchRepositories();
        }

        // Check Notion connection status
        if (result.dbCreated) {
            handleNotionDbCreated(result.connectedNotion);
        } else if (result.connectedNotion && result.connectedNotion.status === 'success') {
            handleNotionConnected(result.connectedNotion);
        }
    });

    // --- UI Update Functions ---
    function showConnectedRepo(repoName) {
        repoLoading.classList.add('hidden');
        repoSelection.classList.add('hidden');
        repoConnected.classList.remove('hidden');
        connectedRepoName.textContent = repoName;
    }

    function handleNotionConnected(notionData) {
        notionDisconnected.classList.add('hidden');
        notionConnected.classList.remove('hidden');
        notionDbCreated.classList.add('hidden');
        notionWorkspaceName.textContent = notionData.workspaceName;
        fetchNotionPages();
    }

    function handleNotionDbCreated(notionData) {
        notionDisconnected.classList.add('hidden');
        notionConnected.classList.remove('hidden');
        document.getElementById('notion-page-selection').classList.add('hidden');
        notionDbCreated.classList.remove('hidden');
        notionWorkspaceName.textContent = notionData.workspaceName;
    }

    // --- Data Fetching ---
    function fetchRepositories() {
        repoLoading.classList.remove('hidden');
        repoSelection.classList.add('hidden');

        fetch('http://localhost:8080/api/github/repos')
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .then(repos => {
                repoList.innerHTML = ''; // Clear existing options
                repos.forEach(repo => {
                    const option = document.createElement('option');
                    // Use the unique fullName as the value
                    option.value = repo.fullName;
                    // Display the simple name in the list
                    option.textContent = repo.name;
                    repoList.appendChild(option);
                });
                repoLoading.classList.add('hidden');
                repoSelection.classList.remove('hidden');
            })
            .catch(error => {
                console.error('Error fetching GitHub repositories:', error);
                repoLoading.textContent = 'Error loading repositories. Please ensure the backend is running and you are logged in.';
            });
    }

    function fetchNotionPages() {
        pageLoading.classList.remove('hidden');
        pageSelectionControls.classList.add('hidden');
        // This would be a fetch call to your backend: GET /api/notion/pages
        fetch('http://localhost:8080/api/notion/pages')
            .then(response => response.json())
            .then(pages => {
                pageList.innerHTML = '';
                pages.forEach(page => {
                    const option = document.createElement('option');
                    option.value = page.id;
                    option.textContent = page.title;
                    pageList.appendChild(option);
                });
                pageLoading.classList.add('hidden');
                pageSelectionControls.classList.remove('hidden');
            })
            .catch(error => {
                console.error('Error fetching notion pages:', error);
                pageLoading.textContent = 'Error loading pages.';
            });
    }

    // --- Event Listeners ---
    connectRepoButton.addEventListener('click', () => {
        const selectedRepo = repoList.value;
        if (selectedRepo) {
            chrome.storage.local.set({ connectedRepo: selectedRepo }, () => showConnectedRepo(selectedRepo));
        }
    });

    createRepoButton.addEventListener('click', () => {
        const newRepoName = newRepoNameInput.value.trim();
        if (newRepoName) {
            chrome.storage.local.set({ connectedRepo: newRepoName }, () => showConnectedRepo(newRepoName));
        } else {
            alert('Please enter a name for the new repository.');
        }
    });

    connectNotionButton.addEventListener('click', () => {
        chrome.tabs.create({ url: 'http://localhost:8080/auth/notion/login' });
    });

    createDbButton.addEventListener('click', () => {
        const selectedPageId = pageList.value;
        if (!selectedPageId) {
            alert('Please select a page first.');
            return;
        }
        // This would be a fetch call to your backend: POST /api/notion/database
        fetch('http://localhost:8080/api/notion/database', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ pageId: selectedPageId })
        })
        .then(response => response.json())
        .then(data => {
            if (data.databaseId) {
                // Save final state
                chrome.storage.local.set({ dbCreated: true });
                handleNotionDbCreated(JSON.parse(localStorage.getItem('connectedNotion'))); // a bit of a hack to get workspace name
            }
        })
        .catch(error => console.error('Error creating database:', error));
    });

    // --- Message Listener from Background Script ---
    chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
        if (message.type === 'NOTION_CONNECTION_SUCCESS') {
            handleNotionConnected(message.data);
        }
    });
});