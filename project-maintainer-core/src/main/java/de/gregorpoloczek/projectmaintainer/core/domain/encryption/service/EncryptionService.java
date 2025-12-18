package de.gregorpoloczek.projectmaintainer.core.domain.encryption.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

@Component
public class EncryptionService {
    private SecretKeySpec secretKey;

    public class SecretStringSerializer extends JsonSerializer<SecretString> {

        @Override
        public void serialize(SecretString value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(EncryptionService.this.encrypt(value.getValue()));
        }
    }

    public class SecretStringDeserializer extends JsonDeserializer<SecretString> {
        @Override
        public SecretString deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return new SecretString(EncryptionService.this.decrypt(p.getValueAsString()));
        }
    }

    @SneakyThrows({NoSuchAlgorithmException.class})
    @PostConstruct
    void init() {
        String deviceData = generateDeviceData();

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] deviceHash = md.digest(deviceData.getBytes(StandardCharsets.UTF_8));

        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(deviceHash);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    @SneakyThrows({IOException.class})
    private static @NonNull String generateDeviceData() {
        // determine inode from home directory (if possible)
        String iNodeData = Optional.ofNullable(Files.readAttributes(Paths.get(System.getProperty("user.home")), BasicFileAttributes.class).fileKey())
                .map(Object::toString)
                .orElse("N/A");

        return System.getProperty("os.name") + ":" +
                System.getProperty("os.arch") + ":" +
                System.getProperty("user.name") + ":" + iNodeData;
    }

    @SneakyThrows
    public String encrypt(String value) {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, this.secretKey);
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(encrypted);
    }

    @SneakyThrows
    public String decrypt(String value) {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, this.secretKey);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(value));

        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
