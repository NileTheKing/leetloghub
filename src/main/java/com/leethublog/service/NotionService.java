package com.leethublog.service;

public interface NotionService {
    void createPageInDatabase(String token, String databaseId, String title);
}
