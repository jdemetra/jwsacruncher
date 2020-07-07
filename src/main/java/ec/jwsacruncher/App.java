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
import ec.demetra.workspace.WorkspaceItem;
import ec.demetra.workspace.file.FileWorkspace;
import ec.jwsacruncher.core.FileRepository;
import ec.jwsacruncher.batch.SaBatchProcessor;
import ec.jwsacruncher.batch.SaBatchInformation;
import ec.tss.ITsProvider;
import ec.tss.TsFactory;
import ec.tss.sa.EstimationPolicyType;
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
import ec.tstoolkit.algorithm.ProcessingContext;
import ec.tstoolkit.design.VisibleForTesting;
import ec.tstoolkit.information.InformationMapping;
import ec.tstoolkit.information.InformationSet;
import ec.tstoolkit.timeseries.calendars.GregorianCalendarManager;
import ec.tstoolkit.timeseries.regression.ITsVariable;
import ec.tstoolkit.timeseries.regression.TsVariables;
import ec.tstoolkit.utilities.IDynamicObject;
import ec.tstoolkit.utilities.Paths;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;
import picocli.CommandLine;

/**
 *
 * @author Kristof Bayens
 */
@lombok.extern.java.Log
public final class App {

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                File userDir = new File(System.getProperty("user.dir"));
                generateDefaultConfigFile(userDir);
            } else {
                Args config = ArgsDecoder2.decode(args);
                if (config != null) {
                    process(config.getWorkspace(), config.getConfig());
                }
            }
        } catch (IOException | IllegalArgumentException ex) {
            reportException(ex);
            System.exit(-1);
        } catch (CommandLine.ExecutionException ex) {
            reportException(ex.getCause());
            System.exit(-1);
        }
    }

    private static void reportException(Throwable ex) {
        log.log(Level.SEVERE, null, ex);
        System.err.println(ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }

    @VisibleForTesting
    static void generateDefaultConfigFile(@NonNull File userDir) throws IOException {
        WsaConfig config = WsaConfig.generateDefault();
        File configFile = new File(userDir, WsaConfig.DEFAULT_FILE_NAME);
        WsaConfig.write(configFile, config);
    }

    @VisibleForTesting
    static void process(@NonNull File workspace, @NonNull WsaConfig config) throws IllegalArgumentException, IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();

        loadResources();
        enableDiagnostics(config.Matrix);

        try (FileWorkspace ws = FileWorkspace.open(workspace.toPath())) {
            process(ws, ProcessingContext.getActiveContext(), config);
        }

        System.out.println("Total processing time: " + stopwatch.elapsed(TimeUnit.SECONDS) + "s");
    }

    private static void process(FileWorkspace ws, ProcessingContext context, WsaConfig config) throws IOException {
        Map<WorkspaceItem, GregorianCalendarManager> cal = FileRepository.loadAllCalendars(ws, context);
        Map<WorkspaceItem, TsVariables> vars = FileRepository.loadAllVariables(ws, context);
        Map<WorkspaceItem, SaProcessing> sa = FileRepository.loadAllSaProcessing(ws, context);

        applyFilePaths(getFilePaths(config));
        if (config.refresh) {
            refreshVariables(ws, vars);
        }
        if (sa.isEmpty()) {
            return;
        }
        applyOutputConfig(config, ws.getRootFolder());
        for (Entry<WorkspaceItem, SaProcessing> o : sa.entrySet()) {
            process(ws, o.getKey(), o.getValue(), config.getPolicy(), config.BundleSize);
        }
    }

    private static void process(FileWorkspace ws, WorkspaceItem item, SaProcessing processing, EstimationPolicyType policy, int bundleSize) throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();

        System.out.println("Refreshing data");
        processing.refresh(policy, false);
        SaBatchInformation info = new SaBatchInformation(processing.size() > bundleSize ? bundleSize : 0);
        info.setName(item.getId());
        info.setItems(processing);
        SaBatchProcessor processor = new SaBatchProcessor(info, new ConsoleFeedback());
        processor.process();

        System.out.println("Saving new processing...");
        FileRepository.storeSaProcessing(ws, item, processing);

        System.out.println("Processing time: " + stopwatch.elapsed(TimeUnit.SECONDS) + "s");
    }

    private static void loadResources() {
        loadFileProperties();
        ServiceLoader.load(ITsProvider.class).forEach(TsFactory.instance::add);
        ServiceLoader.load(ISaProcessingFactory.class).forEach(SaManager.instance::add);
        ServiceLoader.load(ISaDiagnosticsFactory.class).forEach(SaManager.instance::add);
        InformationMapping.updateAll(null);
    }

    private static void loadFileProperties() {
        String basedir = System.getProperty("basedir");
        if (basedir != null) {
            Path file = java.nio.file.Paths.get(basedir, "etc", "system.properties");
            try (InputStream stream = Files.newInputStream(file)) {
                Properties properties = new Properties();
                properties.load(stream);
                System.getProperties().putAll(properties);
            } catch (IOException ex) {
                log.log(Level.WARNING, "While loading system properties", ex);
            }
        }
    }

    private static void applyFilePaths(File[] paths) {
        TsProviders.all().filter(IFileLoader.class).forEach(o -> o.setPaths(paths));
    }

    private static void applyOutputConfig(WsaConfig config, Path rootFolder) {
        if (config.ndecs != null) {
            BasicConfiguration.setDecimalNumber(config.ndecs);
        }
        if (config.csvsep != null && config.csvsep.length() == 1) {
            BasicConfiguration.setCsvSeparator(config.csvsep.charAt(0));
        }

        if (config.Output == null) {
            config.Output = Paths.concatenate(rootFolder.toAbsolutePath().toString(), "Output");
        }
        File output = new File(config.Output);
        if (!output.exists()) {
            output.mkdirs();
        }

        // TODO: apply instead of add
        SaManager.instance.add(new CsvOutputFactory(getCsvOutputConfiguration(config)));
        SaManager.instance.add(new CsvMatrixOutputFactory(getCsvMatrixOutputConfiguration(config)));
    }

    private static File[] getFilePaths(WsaConfig config) {
        return config.Paths != null
                ? Stream.of(config.Paths).map(File::new).toArray(File[]::new)
                : new File[0];
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

    private static final String DIAGNOSTICS = "diagnostics";

    private static void enableDiagnostics(String[] items) {
        // step 1. We retrieve the used diagnostics
        Set<String> diags = new HashSet<>();
        if (items != null) {
            for (int i = 0; i < items.length; ++i) {
                if (InformationSet.isPrefix(items[i], DIAGNOSTICS)) {
                    int start = DIAGNOSTICS.length() + 1;
                    int end = items[i].indexOf(InformationSet.SEP, start);
                    if (end > 0) {
                        String diag = items[i].substring(start, end);
                        diags.add(diag);
                    }
                }
            }
        }
        // step 2. Enable/disables diag
        SaManager.instance.getDiagnostics().forEach(d -> d.setEnabled(diags.contains(d.getName().toLowerCase())));
    }

    private static void refreshVariables(FileWorkspace ws, Map<WorkspaceItem, TsVariables> vars) {
        vars.forEach((item, v) -> {
            boolean dirty = false;
            Collection<ITsVariable> variables = v.variables();
            for (ITsVariable var : variables) {
                if (var instanceof IDynamicObject) {
                    IDynamicObject dvar = (IDynamicObject) var;
                    dvar.refresh();
                    dirty = true;
                }
            }
            if (dirty) {
                try {
                    ws.store(item, v);
                } catch (IOException ex) {
                    log.log(Level.SEVERE, null, ex);
                }
            }
        }
        );
    }

}
