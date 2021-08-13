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
package jdplus.cruncher.core;

import demetra.sa.SaItems;
import demetra.timeseries.calendars.CalendarDefinition;
import demetra.timeseries.calendars.CalendarManager;
import demetra.timeseries.regression.ModellingContext;
import demetra.timeseries.regression.TsDataSuppliers;
import demetra.timeseries.regression.TsVariables;
import demetra.util.NameManager;
import demetra.workspace.WorkspaceFamily;
import demetra.workspace.WorkspaceItem;
import demetra.workspace.file.FileWorkspace;
import demetra.workspace.util.Paths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Jean Palate
 */
@lombok.experimental.UtilityClass
public class FileRepository {

    public void storeSaProcessing(FileWorkspace ws, WorkspaceItem item, SaItems processing) throws IOException {
        makeSaProcessingBackup(ws, item);
        ws.store(item, processing);
    }

    public Map<WorkspaceItem, SaItems> loadAllSaProcessing(FileWorkspace ws, ModellingContext context) throws IOException {
        Map<WorkspaceItem, SaItems> result = new LinkedHashMap<>();
        for (WorkspaceItem item : ws.getItems()) {
            WorkspaceFamily family = item.getFamily();
            if (family.equals(WorkspaceFamily.SA_MULTI)) {
                result.put(item, (SaItems) ws.load(item));
            }
        }
        return result;
    }

    public Map<WorkspaceItem, CalendarManager> loadAllCalendars(FileWorkspace ws, ModellingContext context) throws IOException {
        Map<WorkspaceItem, CalendarManager> result = new LinkedHashMap<>();
        for (WorkspaceItem item : ws.getItems()) {
            WorkspaceFamily family = item.getFamily();
            if (family.equals(WorkspaceFamily.UTIL_CAL)) {
                CalendarManager calendar = (CalendarManager) ws.load(item);
                result.put(item, calendar);
                applyCalendars(context, calendar);
            }
        }
        return result;
    }

    public Map<WorkspaceItem, TsDataSuppliers> loadAllVariables(FileWorkspace ws, ModellingContext context) throws IOException {
        Map<WorkspaceItem, TsDataSuppliers> result = new LinkedHashMap<>();
        for (WorkspaceItem item : ws.getItems()) {
            WorkspaceFamily family = item.getFamily();
            if (family.equals(WorkspaceFamily.UTIL_VAR)) {
                TsDataSuppliers vars = (TsDataSuppliers) ws.load(item);
                result.put(item, vars);
                applyVariables(context, item.getLabel(), vars);
            }
        }
        return result;
    }

    private void makeSaProcessingBackup(FileWorkspace ws, WorkspaceItem item) throws IOException {
        Path source = ws.getFile(item);
        Path target = source.getParent().resolve(Paths.changeExtension(source.getFileName().toString(), "bak"));
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void applyVariables(ModellingContext context, String id, TsDataSuppliers value) {
        NameManager<TsDataSuppliers> manager = context.getTsVariableManagers();
        manager.set(id, value);
        manager.resetDirty();
    }

    private void applyCalendars(ModellingContext context, CalendarManager source) {
        CalendarManager target = context.getCalendars();
        for (String s : source.getNames()) {
            if (!target.contains(s)) {
                CalendarDefinition cal = source.get(s);
                target.set(s, cal);
            }
        }
        target.resetDirty();
    }
}
