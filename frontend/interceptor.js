console.log("[LeetLogHub] Interceptor injected.");

/**
 * Helper function to read a cookie value by name.
 */
function getCookie(name) {
    let cookieValue = null;
    if (document.cookie && document.cookie !== '') {
        const cookies = document.cookie.split(';');
        for (let i = 0; i < cookies.length; i++) {
            const cookie = cookies[i].trim();
            if (cookie.substring(0, name.length + 1) === (name + '=')) {
                cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                break;
            }
        }
    }
    return cookieValue;
}

/**
 * Scrapes static data from the DOM, like title and description.
 */
function scrapeStaticData() {
    const titleTag = document.querySelector('title');
    const rawTitle = titleTag ? titleTag.textContent.trim() : 'Unknown Title';
    const problemTitle = rawTitle.replace(' - LeetCode', '');

    const descriptionTag = document.querySelector('meta[name="description"]');
    const problemDescription = descriptionTag ? descriptionTag.getAttribute('content') : 'Could not find description.';

    const difficultyNode = document.querySelector('.text-difficulty-easy, .text-difficulty-medium, .text-difficulty-hard');
    const problemDifficulty = difficultyNode ? difficultyNode.textContent.toUpperCase() : "UNKNOWN";

    return {
        problemTitle,
        problemDifficulty,
        problemDescription
    };
}

/**
 * Step 2: Fetches the full submission details using a GraphQL query.
 */
async function getFullSubmissionDetails(submissionId) {
    console.log(`[LeetLogHub] Step 2: Fetching full details for submission ID: ${submissionId}`);

    const csrftoken = getCookie('csrftoken');
    if (!csrftoken) {
        console.error("[LeetLogHub] CSRF token not found in cookies.");
        return;
    }

    const graphqlQuery = {
        operationName: "submissionDetails",
        variables: { submissionId: parseInt(submissionId, 10) },
        query: `query submissionDetails($submissionId: Int!) {
            submissionDetails(submissionId: $submissionId) {
                runtime
                runtimeDisplay
                runtimePercentile
                memory
                memoryDisplay
                memoryPercentile
                code
                timestamp
                lang { name verboseName }
                question { questionId titleSlug }
                statusCode
            }
        }`
    };

    try {
        const response = await originalFetch('/graphql/', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'x-csrftoken': csrftoken,
                'Referer': window.location.href, // Add Referer header
            },
            body: JSON.stringify(graphqlQuery),
        });

        const fullData = await response.json();
        const submissionDetails = fullData.data?.submissionDetails;

        if (!submissionDetails) {
            console.error("[LeetLogHub] Could not fetch submission details from GraphQL.", fullData);
            return;
        }

        console.log("[LeetLogHub] Successfully fetched full details:", submissionDetails);

        const staticData = scrapeStaticData();
        const problemUrl = `https://leetcode.com/problems/${submissionDetails.question.titleSlug}/`;

        const finalData = {
            code: submissionDetails.code,
            language: submissionDetails.lang.verboseName,
            runtimeMs: parseInt(submissionDetails.runtime, 10),
            memoryMb: parseFloat(submissionDetails.memoryDisplay.replace(' MB', '')),
            runtimePercentile: submissionDetails.runtimePercentile,
            memoryPercentile: submissionDetails.memoryPercentile,
            problemUrl: problemUrl,
            problemTitle: staticData.problemTitle,
            problemDifficulty: staticData.problemDifficulty,
            problemDescription: staticData.problemDescription,
            solveStatus: 'GOOD',
        };

        console.log('[LeetLogHub] Combined data to be sent:', finalData);

        // Send data to the page's window, to be picked up by the bridge script
        window.postMessage({ 
            type: 'LEETLOG_SUBMISSION_DATA', 
            data: finalData 
        }, '*');

    } catch (error) {
        console.error("[LeetLogHub] Error in getFullSubmissionDetails:", error);
    }
}

// --- Main Interception Logic ---
const originalFetch = window.fetch;
let processedSubmissions = new Set();

window.fetch = async (url, options) => {
    const response = await originalFetch(url, options);

    if (typeof url === 'string' && url.includes('/submissions/detail/') && url.includes('/check/')) {
        const responseClone = response.clone();

        responseClone.json().then(data => {
            if (data.state === 'SUCCESS' && data.status_msg === 'Accepted' && !processedSubmissions.has(data.submission_id)) {
                processedSubmissions.add(data.submission_id);
                getFullSubmissionDetails(data.submission_id);
            }
        }).catch(err => {});
    }

    return response;
};