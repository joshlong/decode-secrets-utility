package com.example.decodesecrets;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.SystemPropertyUtils;

import java.io.File;
import java.util.Base64;
import java.util.Map;


@ImportRuntimeHints(DecodeSecretsApplication.Hints.class)
@SpringBootApplication
public class DecodeSecretsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DecodeSecretsApplication.class, args);
    }

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.reflection().registerType(Secrets.class, MemberCategory.values());
        }
    }

    @Bean
    ApplicationRunner decoder(ObjectMapper objectMapper) {
        return args -> {
            var resource = source(args);
            var secrets = objectMapper.readValue(resource.getInputStream(), Secrets.class);
            secrets.data().forEach((k, v) -> {
                var decoded = new String(Base64.getDecoder().decode(v));
                System.out.println(k + '=' + decoded);
            });
        };
    }

    @SneakyThrows
    private Resource source(ApplicationArguments arguments) {
        if (System.in.available() > 0) {
            return new InputStreamResource(System.in);
        }
        if (arguments.containsOption("file")) {
            var fileArgValues = arguments.getOptionValues("file");
            Assert.state(fileArgValues.size() == 1, "there should be only one file specified");
            var fileArgValue = fileArgValues.get(0);
            Assert.hasText(fileArgValue, "the file path must be non-empty");
            var pathName = SystemPropertyUtils.resolvePlaceholders(fileArgValue);
            return new FileSystemResource(new File(pathName));
        }
        throw new IllegalStateException("you must specify --file <path to file> or pass data in via STDIN");
    }
}

record Secrets(String apiVersion, Map<String, String> data, String kind, Map<String, String> metadata) {
}
