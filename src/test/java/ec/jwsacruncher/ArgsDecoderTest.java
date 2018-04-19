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

import com.google.common.io.Files;
import static ec.jwsacruncher.ArgsDecoder.decode;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Philippe Charles
 */
public class ArgsDecoderTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void testNoArgs() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> decode())
                .withMessage("Missing workspace path")
                .withNoCause();
    }

    @Test
    @SuppressWarnings("null")
    public void testX() throws IOException {
        File userDir = temp.newFolder("X");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> decode("-x"))
                .withMessage("Missing config path")
                .withNoCause();

        File configFile = new File(userDir, "config.xml");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> decode("-x", configFile.getAbsolutePath()))
                .withMessage("Invalid config file")
                .withCauseInstanceOf(IOException.class);

        Files.write("some invalid content", configFile, StandardCharsets.UTF_8);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> decode("-x", configFile.getAbsolutePath()))
                .withMessage("Invalid config file")
                .withCauseInstanceOf(IOException.class);

        WsaConfig config = new WsaConfig();
        WsaConfig.write(configFile, config);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> decode("-x", configFile.getAbsolutePath()))
                .withMessage("Missing workspace path")
                .withNoCause();

        File workspace = new File(userDir, "workspace.xml");

        assertThat(decode("-x", configFile.getAbsolutePath(), workspace.getAbsolutePath()))
                .as("Valid args")
                .satisfies(o -> {
                    assertThat(o.getWorkspace()).isEqualTo(workspace);
                    assertThat(o.getConfig()).isEqualToComparingFieldByField(config);
                });
    }
}
