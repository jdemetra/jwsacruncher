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
import ec.jwsacruncher.xml.XmlWorkspace;
import ec.tss.xml.IXmlConverter;
import ec.tss.xml.information.XmlInformationSet;
import ec.tstoolkit.information.InformationSet;
import ec.tstoolkit.information.InformationSetSerializable;
import ec.tstoolkit.utilities.IModifiable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author Jean Palate
 */
final class XmlUtil {

    private XmlUtil() {
        // static class
    }

    static XmlGenericWorkspace loadWorkspace(Path file) {
        try {
            return (XmlGenericWorkspace) unmarshal(file, XML_GENERIC_WS_CONTEXT);
        } catch (JAXBException | IOException ex) {
            return null;
        }
    }

    static XmlWorkspace loadLegacyWorkspace(Path file) {
        try {
            return (XmlWorkspace) unmarshal(file, XML_WS_CONTEXT);
        } catch (JAXBException | IOException ex) {
            return null;
        }
    }

    static <S, X extends IXmlConverter<S>> S loadLegacy(Path file, Class<X> type) {
        if (!Files.exists(file) || !Files.isReadable(file)) {
            return null;
        }

        try {
            X jaxbElement = unmarshalLegacy(file, type);
            return jaxbElement.create();
        } catch (JAXBException | IOException ex) {
            return null;
        }
    }

    static <X extends InformationSetSerializable> X loadInfo(Path file, Class<X> type) {
        if (!Files.exists(file) || !Files.isReadable(file)) {
            return null;
        }

        try {
            XmlInformationSet jaxbElement = unmarshalInfo(file);
            X result = type.newInstance();
            if (!result.read(jaxbElement.create())) {
                return null;
            }
            return result;
        } catch (JAXBException | InstantiationException | IllegalAccessException | IOException ex) {
            return null;
        }
    }

    static <T, X extends IXmlConverter<T>> boolean storeLegacy(Path file, Class<X> type, T item) {
        try {
            X jaxbElement = type.newInstance();
            jaxbElement.copy(item);
            marshalLegacy(file, jaxbElement);
            if (item instanceof IModifiable) {
                ((IModifiable) item).resetDirty();
            }
            return true;
        } catch (InstantiationException | IllegalAccessException | JAXBException | IOException ex) {
            return false;
        }
    }

    static <T extends InformationSetSerializable> boolean storeInfo(Path file, T item) {
        InformationSet info = item.write(false);
        if (info == null) {
            return false;
        }
        XmlInformationSet jaxbElement = new XmlInformationSet();
        jaxbElement.copy(info);
        try {
            marshalInfo(file, jaxbElement);
            return true;
        } catch (JAXBException | IOException ex) {
            return false;
        }
    }

    private static <X extends IXmlConverter<?>> X unmarshalLegacy(Path file, Class<X> type) throws JAXBException, IOException {
        return (X) unmarshal(file, JAXBContext.newInstance(type));
    }

    private static XmlInformationSet unmarshalInfo(Path file) throws JAXBException, IOException {
        return (XmlInformationSet) unmarshal(file, XML_INFORMATION_SET_CONTEXT);
    }

    private static Object unmarshal(Path file, JAXBContext context) throws JAXBException, IOException {
        Unmarshaller unmarshaller = context.createUnmarshaller();
        try {
            return unmarshaller.unmarshal(file.toFile());
        } catch (UnsupportedOperationException ex) {
            try (Reader reader = Files.newBufferedReader(file)) {
                return unmarshaller.unmarshal(reader);
            }
        }
    }

    private static void marshalLegacy(Path file, IXmlConverter<?> jaxbElement) throws JAXBException, IOException {
        marshal(file, JAXBContext.newInstance(jaxbElement.getClass()), jaxbElement);
    }

    private static void marshalInfo(Path file, XmlInformationSet jaxbElement) throws JAXBException, IOException {
        marshal(file, XML_INFORMATION_SET_CONTEXT, jaxbElement);
    }

    private static void marshal(Path file, JAXBContext context, Object jaxbElement) throws JAXBException, IOException {
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        try {
            marshaller.marshal(jaxbElement, file.toFile());
        } catch (UnsupportedOperationException ex) {
            try (Writer writer = Files.newBufferedWriter(file)) {
                marshaller.marshal(jaxbElement, writer);
            }
        }
    }

    private static final JAXBContext XML_GENERIC_WS_CONTEXT;
    private static final JAXBContext XML_WS_CONTEXT;
    private static final JAXBContext XML_INFORMATION_SET_CONTEXT;

    static {
        try {
            XML_GENERIC_WS_CONTEXT = JAXBContext.newInstance(XmlGenericWorkspace.class);
            XML_WS_CONTEXT = JAXBContext.newInstance(XmlWorkspace.class);
            XML_INFORMATION_SET_CONTEXT = JAXBContext.newInstance(XmlInformationSet.class);
        } catch (JAXBException ex) {
            throw new RuntimeException(ex);
        }
    }
}
