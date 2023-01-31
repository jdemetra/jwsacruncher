/*
 * Copyright 2018 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package ec.jwsacruncher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;

import static ec.jwsacruncher.ArgsDecoder2.decode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Philippe Charles
 */
public class ArgsDecoder2Test {

    @Test
    public void testDecodeNoArgs() throws IOException {
        assertThat(decode()).isNull();
    }

    @Test
    public void testDecodeDefault(@TempDir Path temp) throws IOException {
        Path ws = temp.resolve("workspace.xml");

        assertThat(decode(ws.toString()))
                .as("Valid args")
                .usingRecursiveComparison()
                .isEqualTo(Args.of(ws.toFile(), new WsaConfig()));
    }

    @Test
    public void testDecodeConfig(@TempDir Path temp) throws IOException {
        Path ws = temp.resolve("workspace.xml");
        Path cfg = temp.resolve("config.xml");

        assertThatThrownBy(() -> decode(ws.toString(), "-x", temp.toString()))
                .as("Config file is not a file")
                .isInstanceOf(CommandLine.ExecutionException.class)
                .hasCauseInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> decode(ws.toString(), "-x", cfg.toString()))
                .as("Config file is missing")
                .isInstanceOf(CommandLine.ExecutionException.class)
                .hasCauseInstanceOf(NoSuchFileException.class);

        writeString(cfg, "");
        assertThatThrownBy(() -> decode(ws.toString(), "-x", cfg.toString()))
                .as("Config file is empty")
                .isInstanceOf(CommandLine.ExecutionException.class)
                .hasCauseInstanceOf(EOFException.class);

        writeString(cfg, "<?xml version=\"1.0\" encoding=\"UTF-");
        assertThatThrownBy(() -> decode(ws.toString(), "-x", cfg.toString()))
                .as("Config file has partial content")
                .isInstanceOf(CommandLine.ExecutionException.class)
                .hasCauseInstanceOf(IOException.class);

        writeString(cfg, "some invalid content");
        assertThatThrownBy(() -> decode(ws.toString(), "-x", cfg.toString()))
                .as("Config file is invalid")
                .isInstanceOf(CommandLine.ExecutionException.class)
                .hasCauseInstanceOf(IOException.class);

        WsaConfig config = new WsaConfig();
        config.layout = "htable";
        WsaConfig.write(cfg.toFile(), config);
        assertThat(decode(ws.toString(), "-x", cfg.toString()))
                .as("Valid args")
                .usingRecursiveComparison()
                .isEqualTo(Args.of(ws.toFile(), config));
    }

    private static void writeString(Path file, String content) throws IOException {
        Files.write(file, Collections.singleton(content), StandardCharsets.UTF_8);
    }
}
