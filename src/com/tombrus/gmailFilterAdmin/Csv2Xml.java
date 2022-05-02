package com.tombrus.gmailFilterAdmin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.tombrus.gmailFilterAdmin.impl.FileFinder;
import com.tombrus.gmailFilterAdmin.impl.FileFinder.FilePair;
import com.tombrus.gmailFilterAdmin.impl.Filter;

@SuppressWarnings("SameParameterValue")
public class Csv2Xml {
    public static void main(String[] args) throws IOException {
        for (FilePair p : FileFinder.find(Paths.get("."))) {
            System.out.println("transforming " + p.csv() + " -> " + p.xml() + "...");
            if (Files.isRegularFile(p.csv())) {
                List<Filter> filterList = readFilters(p.csv());
                List<String> xmlLines   = generateXml(filterList);
                Files.write(p.xml(), xmlLines);
                System.out.println("   transformed " + filterList.size() + " filters");
            } else {
                System.out.println("   csv file not found");
            }
        }
    }

    private static List<Filter> readFilters(Path file) throws IOException {
        try (Stream<String> lineStream = Files.lines(file)) {
            return lineStream.skip(1).map(Filter::new).toList();
        }
    }

    private static List<String> generateXml(List<Filter> filters) {
        List<String> lines = new ArrayList<>();

        lines.add("<?xml version='1.0' encoding='UTF-8'?>");
        lines.add("<feed xmlns='http://www.w3.org/2005/Atom' xmlns:apps='http://schemas.google.com/apps/2006'>");
        lines.addAll(filters.stream().sorted(new MyComparator()).flatMap(Filter::toXml).toList());
        lines.add("</feed>");

        return lines;
    }

    private static class MyComparator implements java.util.Comparator<Filter> {
        @Override
        public int compare(Filter f1, Filter f2) {
            if (f1.label == null || f1.label.isBlank()) {
                return -1;
            }
            if (f2.label == null || f2.label.isBlank()) {
                return 1;
            }
            if (isParent(f2.label, f1.label)) {
                return -1;
            }
            if (isParent(f1.label, f2.label)) {
                return 1;
            }
            return f1.label.compareTo(f2.label);
        }

        private boolean isParent(String a, String b) {
            return b.startsWith(a + "/");
        }
    }
}
