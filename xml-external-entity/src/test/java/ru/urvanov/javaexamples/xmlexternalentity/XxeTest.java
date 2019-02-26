package ru.urvanov.javaexamples.xmlexternalentity;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Fedor Urvanov
 * (<a href="https://urvanov.ru">https://urvanov.ru</a>)
 */
public class XxeTest 
{

    private static final String SECRET_WORD = "some secret word";

    private static final Logger logger = LoggerFactory.getLogger(XxeTest.class);

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "myobject")
    static class MyObject {

        @XmlElement(name = "field1")
        private String field1;

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("MyObject [field1=");
            builder.append(field1);
            builder.append("]");
            return builder.toString();
        }
        
    }

    @Test
    public void xmlExternalEntity() throws IOException, JAXBException, XMLStreamException {
        // Создаём временный файл.
        File file = tmpFolder.newFile();
        logger.info("tempFile = {}.", file.getCanonicalPath());
        
        // Записываем секретное слово в файл.
        Files.write(file.toPath(),
                SECRET_WORD.getBytes(Charset.forName("UTF-8")),
                StandardOpenOption.WRITE);
        
        // Генерируем XML с Xml eXternal Entity Injection
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        stringBuilder.append("<!DOCTYPE myobject[<!ENTITY xxe SYSTEM \"file:///");
        stringBuilder.append(file.getCanonicalPath()); // XXE! Считываем содержимое
                                                       // файла в сущность.
        stringBuilder.append("\">]>");
        stringBuilder.append("<myobject>");
        stringBuilder.append("<field1>");
        stringBuilder.append("&xxe;");
        stringBuilder.append("</field1>");
        stringBuilder.append("</myobject>");
        String xmlString = stringBuilder.toString();
        logger.info("xmlString={}.", xmlString);
        
        JAXBContext jaxbContext = JAXBContext.newInstance(MyObject.class);
        XMLInputFactory xif = XMLInputFactory.newFactory();
        
        // Включаем поддержку внешних сущностей в XML
        xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, true);
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, true);

        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        MyObject myObject = null;
        try (StringReader stringReader = new StringReader(xmlString)) {
            XMLStreamReader xsr = xif.createXMLStreamReader(stringReader);
            myObject = (MyObject) jaxbUnmarshaller.unmarshal(xsr);
        }
        
        // В нашей считанной сущности содержится секретное слово из файла.
        // В данном случае ничего страшного, а ведь таким образом
        // можно и /etc/passwd считать.
        assertEquals(SECRET_WORD, myObject.getField1());
    }
}
