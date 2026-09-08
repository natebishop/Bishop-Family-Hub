package com.familyhub.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenEncryptionServiceTest {

    // Base64-encoded 32-byte key
    private static final String VALID_KEY = "hZVYYDBsu6sIox9GnjtoSB6Wv3cgjVG5euDC/JVAHZ4=";

    private TokenEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new TokenEncryptionService(VALID_KEY);
    }

    @Test
    void encryptThenDecrypt_returnsOriginal() {
        String original = "ya29.a0AfH6SMBx_some_access_token_value";
        String encrypted = encryptionService.encrypt(original);
        String decrypted = encryptionService.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void encrypt_producesDifferentCiphertext() {
        String input1 = "token-one";
        String input2 = "token-two";

        String encrypted1 = encryptionService.encrypt(input1);
        String encrypted2 = encryptionService.encrypt(input2);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    void encrypt_sameInput_producesDifferentCiphertextEachTime() {
        String input = "same-token";
        String encrypted1 = encryptionService.encrypt(input);
        String encrypted2 = encryptionService.encrypt(input);

        // Due to random IV, same plaintext → different ciphertext
        assertThat(encrypted1).isNotEqualTo(encrypted2);

        // But both decrypt to the same value
        assertThat(encryptionService.decrypt(encrypted1)).isEqualTo(input);
        assertThat(encryptionService.decrypt(encrypted2)).isEqualTo(input);
    }

    @Test
    void encryptedValue_isNotPlaintext() {
        String original = "my-secret-token";
        String encrypted = encryptionService.encrypt(original);

        assertThat(encrypted).doesNotContain(original);
    }

    @Test
    void constructor_invalidBase64_throwsIllegalState() {
        assertThatThrownBy(() -> new TokenEncryptionService("not-valid-base64!!!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64-encoded");
    }

    @Test
    void constructor_wrongKeyLength_throwsIllegalState() {
        // Base64-encoded 16-byte key (too short for AES-256)
        String shortKey = java.util.Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new TokenEncryptionService(shortKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
