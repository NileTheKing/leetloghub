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
        connectRepoButton.disabled = true;

        fetch('http://localhost:8080/api/config/github/repos')
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .then(repos => {
                repoList.innerHTML = ''; // Clear existing options
                if (!repos || repos.length === 0) {
                    repoLoading.classList.remove('hidden');
                    repoLoading.textContent = 'No repositories with push access found.';
                    return; // Keep button disabled
                }

                repos.forEach(repo => {
                    const option = document.createElement('option');
                    option.value = repo.full_name;
                    option.textContent = `${repo.name} ${repo.private ? '🔒' : ''}`;
                    repoList.appendChild(option);
                });

                repoLoading.classList.add('hidden');
                repoSelection.classList.remove('hidden');
                connectRepoButton.disabled = false; // Re-enable button after loading
            })
            .catch(error => {
                console.error('Error fetching GitHub repositories:', error);
                repoLoading.textContent = 'Error loading repositories. Please ensure the backend is running and you are logged in.';
            });
    }

    function fetchNotionPages() {
        pageLoading.classList.remove('hidden');
        pageSelectionControls.classList.add('hidden');
        fetch('http://localhost:8080/api/config/notion/pages')
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
        const repoDropdown = document.getElementById('repo-list');
        const selectedRepoFullName = repoDropdown.value;

        if (selectedRepoFullName && selectedRepoFullName !== 'undefined' && selectedRepoFullName !== '') {
            fetch('http://localhost:8080/api/config/github/target-repository', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ repoFullName: selectedRepoFullName })
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                chrome.storage.local.set({ connectedRepo: selectedRepoFullName }, () => {
                    showConnectedRepo(selectedRepoFullName);
                });
            })
            .catch(error => {
                console.error('Error linking repository:', error);
                alert('Failed to link repository. Check console for details.');
            });
        } else {
            alert("Please select a valid repository from the list.");
        }
    });

    createRepoButton.addEventListener('click', () => {
        const newRepoName = newRepoNameInput.value.trim();
        if (newRepoName) {
            fetch('http://localhost:8080/api/config/github/repos', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name: newRepoName, isPrivate: false })
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .then(newRepo => {
                chrome.storage.local.set({ connectedRepo: newRepo.full_name }, () => {
                    showConnectedRepo(newRepo.full_name);
                });
            })
            .catch(error => {
                console.error('Error creating repository:', error);
                alert('Failed to create repository. Check console for details.');
            });

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

        fetch('http://localhost:8080/api/config/notion/databases', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ pageId: selectedPageId })
        })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => { 
                    throw new Error(`HTTP error! status: ${response.status}, body: ${text}`);
                });
            }
            return; 
        })
        .then(() => {
            chrome.storage.local.set({ dbCreated: true });
        })
        .catch(error => {
            console.error('Error creating database:', error);
            alert('Failed to create database. Check the console for details.');
        });
    });

    // --- Storage Change Listener (More reliable) ---
    chrome.storage.onChanged.addListener((changes, namespace) => {
        if (namespace === 'local' && changes.connectedNotion) {
            const notionData = changes.connectedNotion.newValue;
            if (notionData && notionData.status === 'success') {
                handleNotionConnected(notionData);
            }
        }
        if (namespace === 'local' && changes.dbCreated) {
            if (changes.dbCreated.newValue === true) {
                chrome.storage.local.get('connectedNotion', (result) => {
                    handleNotionDbCreated(result.connectedNotion);
                });
            }
        }
    });
});