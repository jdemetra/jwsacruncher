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
package jdplus.cruncher.batch;

import demetra.processing.Output;
import demetra.sa.SaEstimation;
import demetra.sa.SaItem;
import demetra.sa.SaOutputFactory;
import demetra.util.LinearId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Kristof Bayens
 */
public class SaBatchProcessor {

    private final SaBatchInformation info;
    private final ISaBatchFeedback feedback;
    private final List<SaOutputFactory> output;
//    SaProcessing processing_;
    private final String QUERY = "Loading information...", PROCESS = "Processing...", FLUSH = "Flushing bundle...", OPEN = "Opening...", CLOSE = "Closing...", GENERATEOUTPUT = "Generate Output";

    public SaBatchProcessor(SaBatchInformation info, List<SaOutputFactory> output, ISaBatchFeedback fb) {
        this.info = info;
        this.feedback = fb;
        this.output = output;
    }

    public boolean open() {
        if (feedback != null) {
            feedback.showAction(OPEN);
        }
        return info.open();
    }

    public boolean process() {
        if (!open()) {
            return false;
        }

        Iterator<SaBundle> iter = info.start();
        while (iter.hasNext()) {
            SaBundle current = iter.next();
            Collection<SaItem> items = current.getItems();
            compute(items);
            if (feedback != null) {
                feedback.showAction(FLUSH);
            }
            generateOutput();
            current.flush(output, feedback);
        }
        info.close();
        return true;
    }

    public void generateOutput() {
        if (feedback != null) {
            feedback.showAction(GENERATEOUTPUT);
        }
    }

    private List<Callable<String>> createTasks(Collection<SaItem> items) {
        List<Callable<String>> result = new ArrayList(items.size());
        if (!items.isEmpty()) {
            for (final SaItem o : items) {
                result.add((Callable<String>) () -> {
                    SaEstimation result1 = o.getEstimation();
                    String rslt = result1 == null ? " failed" : " processed";
                    if (feedback != null) {
                        feedback.showItem(o.getDefinition().getTs().getName(), rslt);
                    }
                    return rslt;
                });
            }
        }
        return result;
    }

    private static final int NBR_EXECUTORS = Runtime.getRuntime().availableProcessors();

    private void compute(Collection<SaItem> items) {

        List<Callable<String>> tasks = createTasks(items);
        if (tasks.isEmpty()) {
            return;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(NBR_EXECUTORS);
        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException ex) {
        } finally {
            executorService.shutdown();
        }
    }
}
