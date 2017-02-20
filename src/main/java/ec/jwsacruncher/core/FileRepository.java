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
package ec.jwsacruncher.core;

import ec.jwsacruncher.xml.XmlGenericWorkspace;
import ec.jwsacruncher.xml.XmlWksElement;
import ec.jwsacruncher.xml.XmlWorkspace;
import ec.jwsacruncher.xml.XmlWorkspaceItem;
import ec.satoolkit.GenericSaProcessingFactory;
import ec.tss.sa.SaProcessing;
import ec.tss.xml.sa.XmlSaProcessing;
import ec.tstoolkit.algorithm.ProcessingContext;
import ec.tstoolkit.timeseries.calendars.GregorianCalendarManager;
import ec.tstoolkit.timeseries.calendars.IGregorianCalendarProvider;
import ec.tstoolkit.timeseries.regression.TsVariables;
import ec.tstoolkit.utilities.LinearId;
import ec.tstoolkit.utilities.NameManager;
import ec.tstoolkit.utilities.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Jean Palate
 */
public final class FileRepository {

    public enum Type {
        VAR("Variables"), CAL("Calendars"), SA("SAProcessing"), MULTIDOCUMENTS("multi-documents");

        private final String path;

        private Type(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    private static final LinearId SA_ID = new LinearId(GenericSaProcessingFactory.FAMILY, Type.MULTIDOCUMENTS.path);
    private static final LinearId VAR_ID = new LinearId("Utilities", "Variables");
    private static final String SEP = "@";

    private final Path workspace;
    private final boolean legacy;

    public FileRepository(Path workspace) {
        this.workspace = workspace;
        this.legacy = isLegacyWorkspace(workspace);
    }

    public Path getRepositoryRootFolder() {
        return java.nio.file.Paths.get(Paths.changeExtension(workspace.toAbsolutePath().toString(), null));
    }

    public void storeSaProcessing(String name, SaProcessing processing) {
        if (legacy) {
            storeLegacy(name, processing);
        } else {
            store(name, processing);
        }
    }

    public Map<String, SaProcessing> loadAllSaProcessing(ProcessingContext context) {
        return legacy ? loadAllLegacy(context) : loadAll(context);
    }

    private Path getFolder(Type type) {
        Path result = getRepositoryRootFolder().resolve(type.getPath());
        if (Files.exists(result) && !Files.isDirectory(result)) {
            return null;
        }
        return result;
    }

    private Path getFile(Type type, String fileName) {
        return getFolder(type).resolve(Paths.changeExtension(fileName, "xml"));
    }

    private Path getVariablesFile() {
        return getFile(Type.VAR, Type.VAR.getPath());
    }

    private Path getCalendarFile() {
        return getFile(Type.CAL, Type.CAL.getPath());
    }

    private Map<String, SaProcessing> loadAllLegacy(ProcessingContext context) {
        XmlWorkspace xml = XmlUtil.loadLegacyWorkspace(workspace);
        if (xml == null) {
            return null;
        }

        applyCalendars(loadLegacyCalendars(getCalendarFile()), context);
        applyVariables(ProcessingContext.LEGACY, loadLegacyVariables(getVariablesFile()), context);

        Map<String, SaProcessing> result = new LinkedHashMap<>();
        if (xml.saProcessing != null) {
            for (XmlWksElement item : xml.saProcessing) {
                String fileName = getFileName(item);
                SaProcessing p = loadLegacySaProcessing(getFile(Type.SA, fileName));
                if (p != null) {
                    result.put(fileName, p);
                }
            }
        }
        return result;
    }

    private Map<String, SaProcessing> loadAll(ProcessingContext context) {
        XmlGenericWorkspace xml = XmlUtil.loadWorkspace(workspace);
        if (xml == null) {
            return null;
        }

        // load calendars (same as the legacy code)
        applyCalendars(loadCalendars(getCalendarFile()), context);

        Map<String, SaProcessing> result = new LinkedHashMap<>();
        if (xml.items != null) {
            for (XmlWorkspaceItem item : xml.items) {
                String fileName = getFileName(item);
                LinearId id = new LinearId(item.family.split(SEP));
                if (id.equals(SA_ID)) {
                    SaProcessing p = loadSaProcessing(getFile(Type.SA, fileName));
                    if (p != null) {
                        result.put(fileName, p);
                    }
                } else if (id.equals(VAR_ID)) {
                    applyVariables(item.name, loadVariables(getFile(Type.VAR, fileName)), context);
                }
            }
        }
        return result;
    }

    private void storeLegacy(String name, SaProcessing processing) {
        try {
            Path pfolder = getFolder(Type.SA);
            String nfile = Paths.changeExtension(name, "xml");
            String ofile = Paths.changeExtension(name, "bak");

            Path tfile = pfolder.resolve(nfile);
            Path bfile = pfolder.resolve(ofile);
            Files.copy(tfile, bfile);

            XmlUtil.storeLegacy(tfile, XmlSaProcessing.class, processing);
        } catch (Exception ex) {
        }
    }

    private void store(String name, SaProcessing processing) {
        try {
            Path pfolder = getFolder(Type.SA);
            String nfile = Paths.changeExtension(name, "xml");
            String ofile = Paths.changeExtension(name, "bak");

            Path tfile = pfolder.resolve(nfile);
            Path bfile = pfolder.resolve(ofile);
            Files.copy(tfile, bfile, StandardCopyOption.REPLACE_EXISTING);

            XmlUtil.storeInfo(tfile, processing);
        } catch (Exception ex) {
        }
    }

    private static boolean applyVariables(String id, TsVariables value, ProcessingContext context) {
        NameManager<TsVariables> manager = context.getTsVariableManagers();
        if (value != null) {
            manager.set(id, value);
            manager.resetDirty();
            return true;
        } else {
            return false;
        }
    }

    private static boolean applyCalendars(GregorianCalendarManager value, ProcessingContext context) {
        GregorianCalendarManager manager = context.getGregorianCalendars();
        if (value != null) {
            for (String s : value.getNames()) {
                if (!manager.contains(s)) {
                    IGregorianCalendarProvider cal = value.get(s);
                    manager.set(s, cal);
                }
            }
            manager.resetDirty();
            return true;
        } else {
            return false;
        }
    }

    private static String getFileName(XmlWksElement item) {
        return item.file != null ? item.file : item.name;
    }

    private static String getFileName(XmlWorkspaceItem item) {
        return item.file != null ? item.file : item.name;
    }

    private static boolean isLegacyWorkspace(Path file) {
        return XmlUtil.loadLegacyWorkspace(file) != null;
    }

    private static TsVariables loadVariables(Path file) {
        return XmlUtil.loadInfo(file, TsVariables.class);
    }

    private static TsVariables loadLegacyVariables(Path file) {
        return XmlUtil.loadLegacy(file, ec.tss.xml.legacy.XmlTsVariables.class);
    }

    private static GregorianCalendarManager loadCalendars(Path file) {
        return XmlUtil.loadLegacy(file, ec.tss.xml.calendar.XmlCalendars.class);
    }

    private static GregorianCalendarManager loadLegacyCalendars(Path file) {
        return XmlUtil.loadLegacy(file, ec.tss.xml.legacy.XmlCalendars.class);
    }

    private static SaProcessing loadSaProcessing(Path file) {
        return XmlUtil.loadInfo(file, SaProcessing.class);
    }

    private static SaProcessing loadLegacySaProcessing(Path file) {
        return XmlUtil.loadLegacy(file, XmlSaProcessing.class);
    }
}
