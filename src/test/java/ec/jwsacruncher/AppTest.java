/*
 * Copyright 2013 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved
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

import nbbrd.io.xml.bind.Jaxb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for simple App.
 */
public class AppTest {
    
    @Test
    public void testGenerateDefaultConfigFile(@TempDir Path temp) throws IOException {
        App.generateDefaultConfigFile(temp.toFile());

        assertThat(temp.resolve(WsaConfig.DEFAULT_FILE_NAME))
                .hasContent(writeConfigToString(WsaConfig.generateDefault()));
    }

    private static String writeConfigToString(WsaConfig config) throws IOException {
        return Jaxb.Formatter.of(WsaConfig.class).withFormatted(true).formatToString(config);
    }
}
