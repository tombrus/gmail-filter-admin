package com.tombrus.gmailFilterAdmin;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import com.tombrus.gmailFilterAdmin.impl.FileFinder;
import com.tombrus.gmailFilterAdmin.impl.FileFinder.FilePair;
import com.tombrus.gmailFilterAdmin.impl.Filter;
import com.tombrus.gmailFilterAdmin.impl.Info;

public class Xml2Csv {
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        for (FilePair p : FileFinder.find(Paths.get("."))) {
            System.out.println("transforming " + p.xml() + " -> " + p.csv() + "...");
            if (Files.isRegularFile(p.xml())) {
                List<Filter> filterList = FilterHandler.parse(p.xml());
                List<String> lines      = Stream.concat(Stream.of(Filter.getCsvHeader()), filterList.stream().map(Filter::toCsv)).toList();
                Files.write(p.csv(), lines);
                System.out.println("   transformed " + filterList.size() + " filters");
            } else {
                System.out.println("   xml file not found");
            }
        }
    }

    public static class FilterHandler extends DefaultHandler {
        private final List<Filter> filterList = new ArrayList<>();
        private       Filter       currentFilter;

        public static List<Filter> parse(Path file) throws ParserConfigurationException, SAXException, IOException {
            FilterHandler filterHandler = new FilterHandler();
            SAXParserFactory.newInstance().newSAXParser().parse(file.toFile(), filterHandler);
            return filterHandler.filterList;
        }

        @Override
        public void startElement(String uri, String lName, String qName, Attributes attr) {
            switch (qName) {
            case Info.ENTRY -> filterList.add(currentFilter = new Filter());
            case Info.PROPERTY -> currentFilter.fromXml(attr);
            }
        }
    }
}
