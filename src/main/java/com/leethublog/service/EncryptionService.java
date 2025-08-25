package com.leethublog.service;

public interface EncryptionService {
    String encrypt(String data);
    String decrypt(String encryptedData);
}
