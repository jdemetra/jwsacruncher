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

import ec.tss.sa.EstimationPolicyType;
import ec.tss.sa.ISaDiagnosticsFactory;
import ec.tss.sa.ISaProcessingFactory;
import ec.tss.sa.SaManager;
import ec.tss.sa.output.BasicConfiguration;
import ec.tss.sa.output.CsvLayout;
import ec.tstoolkit.information.InformationMapping;
import ioutil.Jaxb;
import ioutil.Xml;
import java.io.File;
import java.io.IOException;
import java.util.ServiceLoader;
import org.checkerframework.checker.nullness.qual.NonNull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Kristof Bayens
 */
@XmlRootElement(name = "wsaConfig")
public class WsaConfig {

    @XmlElement(name = "policy")
    public String policy = "parameters";
    @XmlElement(name = "refreshall")
    public Boolean refresh = true;
    @XmlElement(name = "output")
    public String Output;
    @XmlElementWrapper(name = "matrix")
    @XmlElement(name = "item")
    public String[] Matrix = new String[0];
    @XmlElementWrapper(name = "tsmatrix")
    @XmlElement(name = "series")
    public String[] TSMatrix = new String[0];
    @XmlElementWrapper(name = "paths")
    @XmlElement(name = "path")
    public String[] Paths;
    @XmlAttribute(name = "bundle")
    public Integer BundleSize = 10000;
    @XmlAttribute(name = "csvlayout")
    public String layout = "list";
    @XmlAttribute(name = "csvseparator")
    public String csvsep = String.valueOf(BasicConfiguration.getCsvSeparator());
    @XmlAttribute(name = "ndecs")
    public Integer ndecs = 6;

    public WsaConfig() {
    }

    public EstimationPolicyType getPolicy() {
        if (policy == null) {
            return EstimationPolicyType.None;
        } else if (policy.equalsIgnoreCase("n")
                || policy.equalsIgnoreCase("current")) {
            return EstimationPolicyType.Fixed;
        } else if (policy.equalsIgnoreCase("f")
                || policy.equalsIgnoreCase("fixed") || policy.equalsIgnoreCase("fixedparameters")) {
            return EstimationPolicyType.FixedParameters;
        } else if (policy.equalsIgnoreCase("p")
                || policy.equalsIgnoreCase("parameters")) {
            return EstimationPolicyType.FreeParameters;
        } else if (policy.equalsIgnoreCase("c")
                || policy.equalsIgnoreCase("complete") || policy.equalsIgnoreCase("concurrent")) {
            return EstimationPolicyType.Complete;
        } else if (policy.equalsIgnoreCase("o")
                || policy.equalsIgnoreCase("outliers")) {
            return EstimationPolicyType.Outliers;
        } else if (policy.equalsIgnoreCase("l")
                || policy.equalsIgnoreCase("lastoutliers")) {
            return EstimationPolicyType.LastOutliers;
        } else if (policy.equalsIgnoreCase("stochastic")
                || policy.equalsIgnoreCase("s")) {
            return EstimationPolicyType.Outliers_StochasticComponent;
        } else {
            return EstimationPolicyType.None;
        }
    }

    public CsvLayout getLayout() {
        if (layout == null) {
            return CsvLayout.List;
        } else if (layout.equalsIgnoreCase("h")
                || layout.equalsIgnoreCase("htable")) {
            return CsvLayout.HTable;
        } else if (layout.equalsIgnoreCase("v")
                || layout.equalsIgnoreCase("vtable")) {
            return CsvLayout.VTable;
        } else {
            return CsvLayout.List;
        }
    }

    static WsaConfig read(File file) throws IOException {
        try {
            return Jaxb.Parser.of(WsaConfig.class).parseFile(file);
        } catch (Xml.WrappedException ex) {
            throw new IOException("Failed to parse config file '" + file + "'", ex.getCause());
        }
    }

    static void write(File file, WsaConfig config) throws IOException {
        try {
            JAXBContext context = JAXBContext.newInstance(WsaConfig.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(config, file);
        } catch (JAXBException ex) {
            throw new IOException("Failed to write config file '" + file + "'", unwrap(ex));
        }
    }

    private static Throwable unwrap(JAXBException ex) {
        return ex.getMessage() == null || ex.getMessage().isEmpty() ? ex.getCause() : ex;
    }

    static final String DEFAULT_FILE_NAME = "wsacruncher.params";

    @NonNull
    static WsaConfig generateDefault() {
        WsaConfig result = new WsaConfig();
        loadAll();
        // series
        result.TSMatrix = BasicConfiguration.allSaSeries(true).toArray(result.TSMatrix);
        result.Matrix = BasicConfiguration.allSaDetails(true).toArray(result.Matrix);
        return result;
    }

    private static void loadAll() {
        // update the possible items
        ServiceLoader.load(ISaProcessingFactory.class).forEach(SaManager.instance::add);
        // load and enables all diagnostics
        ServiceLoader.load(ISaDiagnosticsFactory.class).forEach(
                diag -> {
                    diag.setEnabled(true);
                    SaManager.instance.add(diag);
                });
        InformationMapping.updateAll(null);
    }
}
