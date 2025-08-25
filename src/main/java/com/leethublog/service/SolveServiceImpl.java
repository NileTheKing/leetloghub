package com.leethublog.service;

import com.leethublog.domain.Member;
import org.springframework.stereotype.Service;

@Service
public class SolveServiceImpl implements SolveService {

    private final MemberService memberService;
    private final GithubService githubService;
    private final NotionService notionService;

    public SolveServiceImpl(MemberService memberService, GithubService githubService, NotionService notionService) {
        this.memberService = memberService;
        this.githubService = githubService;
        this.notionService = notionService;
    }

    @Override
    public void logSolution(String username, String problemTitle, String code, String directoryPath) {
        Member member = memberService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Member not found: " + username));

        // 1. Commit to GitHub
        String commitMessage = "Solve: " + problemTitle;
        String filePath = directoryPath + "/" + problemTitle.replaceAll("[^a-zA-Z0-9.-]", "_") + ".java";
        githubService.commitCode(
                member.getEncryptedGithubToken(), // This will need decryption
                member.getTargetRepo(),
                filePath,
                code,
                commitMessage
        );

        // 2. Create Notion Page
        notionService.createPageInDatabase(
                member.getEncryptedNotionToken(), // This will need decryption
                member.getTargetDbId(),
                problemTitle
        );
    }
}
