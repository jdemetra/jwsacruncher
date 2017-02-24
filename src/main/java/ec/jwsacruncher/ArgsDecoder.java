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

import com.google.common.collect.Maps;
import ec.tss.sa.EstimationPolicyType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map.Entry;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author Philippe Charles
 */
final class ArgsDecoder {

    public static Entry<File, WsaConfig> decodeArgs(String[] args) {
        WsaConfig config = new WsaConfig();
        if (args == null || args.length == 0) {
            try {
                writeConfig(new File("wsacruncher.params"), config);
            } catch (JAXBException e) {
                System.err.println("Failed to create params file: " + e.getMessage());
            }
            return Maps.immutableEntry(null, config);
        }

        File file = null;
        //
        int cur = 0;
        while (cur < args.length) {
            String cmd = args[cur++];
            if (cmd.length() == 0) {
                return null;
            }
            if (cmd.charAt(0) != '-') {
                file = new File(cmd);
            } else {
                switch (cmd) {
                    case "-x":
                    case "-X": {
                        String str = getParamOrNull(args, cur++);
                        if (str == null) {
                            return null;
                        }
                        try {
                            config = readConfig(new File(str));
                        } catch (JAXBException e) {
                            System.out.print("Invalid configuration file");
                            return null;
                        }
                        break;
                    }
                    case "-d": {
                        String str = getParamOrNull(args, cur++);
                        if (str == null) {
                            return null;
                        }
                        config.Output = str;
                        break;
                    }
                    case "-m": {
                        String str = getParamOrNull(args, cur++);
                        if (str == null) {
                            return null;
                        }
                        try {
                            config.Matrix = readMatrixConfig(new File(str));
                        } catch (Exception e) {
                            return null;
                        }
                        break;
                    }
                    case "-p": {
                        String str = getParamOrNull(args, cur++);
                        if (str == null) {
                            return null;
                        }
                        config.policy = str;
                        if (config.getPolicy() == EstimationPolicyType.None) {
                            return null;
                        }
                        break;
                    }
                    case "-f": {
                        String str = getParamOrNull(args, cur++);
                        if (str == null) {
                            return null;
                        }
                        config.layout = str;
                        break;
                    }
                    case "-t":
                        // No longer supported
                        // Config.Diagnostics = true;
                        break;
                    default:
                        return null;
                }
            }
        }
        return Maps.immutableEntry(file, config);
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

    private static WsaConfig readConfig(File file) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(WsaConfig.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (WsaConfig) unmarshaller.unmarshal(file);
    }

    private static void writeConfig(File file, WsaConfig config) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(WsaConfig.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(config, file);
    }

    private static String[] readMatrixConfig(File file) throws IOException {
        return Files.readAllLines(file.toPath()).toArray(new String[0]);
    }
}
