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

import com.google.common.base.Stopwatch;
import ec.jwsacruncher.core.FileRepository;
import ec.jwsacruncher.batch.SaBatchProcessor;
import ec.jwsacruncher.batch.SaBatchInformation;
import ec.tss.ITsProvider;
import ec.tss.TsFactory;
import ec.tss.sa.ISaDiagnosticsFactory;
import ec.tss.sa.ISaProcessingFactory;
import ec.tss.sa.SaManager;
import ec.tss.sa.SaProcessing;
import ec.tss.sa.output.BasicConfiguration;
import ec.tss.sa.output.CsvMatrixOutputConfiguration;
import ec.tss.sa.output.CsvMatrixOutputFactory;
import ec.tss.sa.output.CsvOutputConfiguration;
import ec.tss.sa.output.CsvOutputFactory;
import ec.tss.tsproviders.IFileLoader;
import ec.tss.tsproviders.TsProviders;
import ec.tstoolkit.utilities.Paths;
import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 *
 * @author Kristof Bayens
 */
public final class App {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        Entry<File, WsaConfig> config = ArgsDecoder.decodeArgs(args);
        if (config == null) {
            System.out.println("Wrong arguments");
            return;
        }

        if (args == null || args.length == 0) {
            return;
        }

        if (config.getValue() == null) {
            return;
        }

        loadResources();
        run(config.getKey(), config.getValue());

        System.out.println("Total processing time: " + stopwatch.elapsed(TimeUnit.SECONDS) + "s");
    }

    private static void run(File file, WsaConfig config) {
        Map<String, SaProcessing> sa = FileRepository.loadProcessing(file);
        if (sa == null || sa.isEmpty()) {
            return;
        }

        applyFilePaths(getFilePaths(config));
        applyOutputConfig(config, file);

        sa.entrySet().forEach(o -> process(config, file, o.getKey(), o.getValue()));
    }

    private static void loadResources() {
        ServiceLoader.load(ITsProvider.class).forEach(TsFactory.instance::add);
        ServiceLoader.load(ISaProcessingFactory.class).forEach(SaManager.instance::add);
        ServiceLoader.load(ISaDiagnosticsFactory.class).forEach(SaManager.instance::add);
    }

    private static void applyFilePaths(File[] paths) {
        TsProviders.all().filter(IFileLoader.class).forEach(o -> o.setPaths(paths));
    }

    private static File[] getFilePaths(WsaConfig config) {
        return config.Paths != null
                ? Stream.of(config.Paths).map(File::new).toArray(File[]::new)
                : new File[0];
    }

    private static void applyOutputConfig(WsaConfig config, File file) {
        if (config.ndecs != null) {
            BasicConfiguration.setDecimalNumber(config.ndecs);
        }
        if (config.csvsep != null && config.csvsep.length() == 1) {
            BasicConfiguration.setCsvSeparator(config.csvsep.charAt(0));
        }

        if (config.Output == null) {
            config.Output = Paths.concatenate(FileRepository.getRepositoryRootFolder(file), "Output");
        }
        File output = new File(config.Output);
        if (!output.exists()) {
            output.mkdirs();
        }

        SaManager.instance.add(new CsvOutputFactory(getCsvOutputConfiguration(config)));
        SaManager.instance.add(new CsvMatrixOutputFactory(getCsvMatrixOutputConfiguration(config)));
    }

    private static CsvOutputConfiguration getCsvOutputConfiguration(WsaConfig config) {
        CsvOutputConfiguration result = new CsvOutputConfiguration();
        result.setFolder(new File(config.Output));
        result.setPresentation(config.getLayout());
        result.setSeries(Arrays.asList(config.TSMatrix));
        return result;
    }

    private static CsvMatrixOutputConfiguration getCsvMatrixOutputConfiguration(WsaConfig config) {
        CsvMatrixOutputConfiguration result = new CsvMatrixOutputConfiguration();
        result.setFolder(new File(config.Output));
        if (config.Matrix != null) {
            result.setItems(Arrays.asList(config.Matrix));
        }
        return result;
    }

    private static void process(WsaConfig config, File file, String name, SaProcessing processing) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        System.out.println("Refreshing data");
        processing.refresh(config.getPolicy(), false);
        SaBatchInformation info = new SaBatchInformation(processing.size() > config.BundleSize ? config.BundleSize : 0);
        info.setName(name);
        info.setItems(processing);
        SaBatchProcessor processor = new SaBatchProcessor(info, new ConsoleFeedback());
        processor.process();

        System.out.println("Saving new processing...");
        FileRepository.write(file, name, processing);

        System.out.println("Processing time: " + stopwatch.elapsed(TimeUnit.SECONDS) + "s");
    }
}
