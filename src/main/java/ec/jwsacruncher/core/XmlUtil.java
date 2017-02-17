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

import ec.tss.xml.IXmlConverter;
import ec.tss.xml.information.XmlInformationSet;
import ec.tstoolkit.information.InformationSet;
import ec.tstoolkit.information.InformationSetSerializable;
import ec.tstoolkit.utilities.IModifiable;
import java.io.File;
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

    static <S, X extends IXmlConverter<S>> S loadLegacy(File file, Class<X> type) {
        if (!file.exists() || !file.canRead()) {
            return null;
        }

        try {
            X jaxbElement = unmarshalLegacy(file, type);
            return jaxbElement.create();
        } catch (JAXBException ex) {
            return null;
        }
    }

    static <X extends InformationSetSerializable> X loadInfo(File file, Class<X> type) {
        if (!file.exists() || !file.canRead()) {
            return null;
        }

        try {
            XmlInformationSet jaxbElement = unmarshalInfo(file);
            X result = type.newInstance();
            if (!result.read(jaxbElement.create())) {
                return null;
            }
            return result;
        } catch (JAXBException | InstantiationException | IllegalAccessException ex) {
            return null;
        }
    }

    static <T, X extends IXmlConverter<T>> boolean storeLegacy(File file, Class<X> type, T item) {
        try {
            X jaxbElement = type.newInstance();
            jaxbElement.copy(item);
            marshalLegacy(file, jaxbElement);
            if (item instanceof IModifiable) {
                ((IModifiable) item).resetDirty();
            }
            return true;
        } catch (InstantiationException | IllegalAccessException | JAXBException ex) {
            return false;
        }
    }

    static <T extends InformationSetSerializable> boolean storeInfo(File file, T item) {
        InformationSet info = item.write(false);
        if (info == null) {
            return false;
        }
        XmlInformationSet jaxbElement = new XmlInformationSet();
        jaxbElement.copy(info);
        try {
            marshalInfo(file, jaxbElement);
            return true;
        } catch (JAXBException ex) {
            return false;
        }
    }

    private static <X extends IXmlConverter<?>> X unmarshalLegacy(File file, Class<X> type) throws JAXBException {
        return (X) unmarshal(file, JAXBContext.newInstance(type));
    }

    private static XmlInformationSet unmarshalInfo(File file) throws JAXBException {
        return (XmlInformationSet) unmarshal(file, XML_INFORMATION_SET_CONTEXT);
    }

    private static Object unmarshal(File file, JAXBContext context) throws JAXBException {
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return unmarshaller.unmarshal(file);
    }

    private static void marshalLegacy(File file, IXmlConverter<?> jaxbElement) throws JAXBException {
        marshal(file, JAXBContext.newInstance(jaxbElement.getClass()), jaxbElement);
    }

    private static void marshalInfo(File file, XmlInformationSet jaxbElement) throws JAXBException {
        marshal(file, XML_INFORMATION_SET_CONTEXT, jaxbElement);
    }

    private static void marshal(File file, JAXBContext context, Object jaxbElement) throws JAXBException {
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(jaxbElement, file);
    }

    private static final JAXBContext XML_INFORMATION_SET_CONTEXT;

    static {
        try {
            XML_INFORMATION_SET_CONTEXT = JAXBContext.newInstance(XmlInformationSet.class);
        } catch (JAXBException ex) {
            throw new RuntimeException(ex);
        }
    }
}
