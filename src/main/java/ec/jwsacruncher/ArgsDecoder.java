/*
 * Copyright 2017 National Bank of Belgium
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

import ec.tss.sa.EstimationPolicyType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.annotation.Nonnull;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
final class ArgsDecoder {

    @Nonnull
    static Args decode(@Nonnull String... args) throws IllegalArgumentException {
        WsaConfig config = new WsaConfig();
        File workspace = null;

        int index = 0;
        while (index < args.length) {
            String cmd = args[index++];
            if (cmd.length() == 0) {
                throw new IllegalArgumentException("Empty arg");
            }
            if (cmd.charAt(0) != '-') {
                workspace = new File(cmd);
            } else {
                switch (cmd) {
                    case "-x":
                    case "-X": {
                        String str = getParamOrNull(args, index++);
                        if (str == null) {
                            throw new IllegalArgumentException("Missing config path");
                        }
                        try {
                            config = WsaConfig.read(new File(str));
                        } catch (IOException e) {
                            throw new IllegalArgumentException("Invalid config file", e);
                        }
                        break;
                    }
                    case "-d": {
                        String str = getParamOrNull(args, index++);
                        if (str == null) {
                            throw new IllegalArgumentException("Missing output arg");
                        }
                        config.Output = str;
                        break;
                    }
                    case "-m": {
                        String str = getParamOrNull(args, index++);
                        if (str == null) {
                            throw new IllegalArgumentException("Missing matrix path");
                        }
                        try {
                            config.Matrix = readMatrixConfig(new File(str));
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Invalid matrix file", e);
                        }
                        break;
                    }
                    case "-p": {
                        String str = getParamOrNull(args, index++);
                        if (str == null) {
                            throw new IllegalArgumentException("Missing policy arg");
                        }
                        config.policy = str;
                        if (config.getPolicy() == EstimationPolicyType.None) {
                            throw new IllegalArgumentException("Invalid policy arg");
                        }
                        break;
                    }
                    case "-f": {
                        String str = getParamOrNull(args, index++);
                        if (str == null) {
                            throw new IllegalArgumentException("Missing layout arg");
                        }
                        config.layout = str;
                        break;
                    }
                    case "-t":
                        // No longer supported
                        // Config.Diagnostics = true;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown arg");
                }
            }
        }

        if (workspace == null) {
            throw new IllegalArgumentException("Missing workspace path");
        }

        return Args.of(workspace, config);
    }

    private static String getParamOrNull(String[] args, int cur) {
        if (cur >= args.length) {
            return null;
        }
        String str = args[cur];
        if (str.length() == 0 || str.charAt(0) == '-') {
            return null;
        }
        return str;
    }

    private static String[] readMatrixConfig(File file) throws IOException {
        return Files.readAllLines(file.toPath()).toArray(new String[0]);
    }
}
