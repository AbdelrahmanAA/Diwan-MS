package com.diwan.common.autoconfigure;

import com.diwan.common.crypto.FieldEncryption;
import com.diwan.common.crypto.FieldEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Column encryption for services that define {@code diwan.crypto.keys} (startup fails on an invalid key).
 * Map it from an environment variable without a default outside DEV, like {@code JWT_SECRET}.
 */
@AutoConfiguration
@ConditionalOnProperty("diwan.crypto.keys")
public class FieldEncryptionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FieldEncryptor fieldEncryptor(@Value("${diwan.crypto.keys}") String keys) {
        FieldEncryptor encryptor = FieldEncryptor.fromSpec(keys);
        FieldEncryption.install(encryptor);
        return encryptor;
    }
}
