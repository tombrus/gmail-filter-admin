package com.tombrus.gmailFilterAdmin.impl;

import org.xml.sax.Attributes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tombrus.gmailFilterAdmin.impl.Info.*;

public class Filter {
    private static final char   FLAG_shouldArchive               = 'A';
    private static final char   FLAG_shouldMarkAsRead            = 'R';
    private static final char   FLAG_shouldAlwaysMarkAsImportant = 'I';
    private static final char   FLAG_shouldNeverMarkAsImportant  = 'i';
    private static final char   FLAG_shouldNeverSpam             = 's';
    private static final char   FLAG_ABSENT                      = '_';
    private static final String LABEL_LABEL                      = "label";
    private static final String FLAGS_LABEL                      = "flags";
    private static final String FROM_LABEL                       = "from";
    private static final String TO_LABEL                         = "to";
    private static final String SUBJECT_LABEL                    = "subject";
    private static final String WITH_LABEL                       = "with";
    private static final String WITHOUT_LABEL                    = "without";
    private static final String SMART_LABEL_LABEL                = "smartLabel";
    private static final String SIZE_OPER_LABEL                  = "sizeOper";
    private static final String SIZE_UNIT_LABEL                  = "sizeUnit";

    public String  label;
    public String  from;
    public String  to;
    public String  subject;
    public String  hasTheWord;
    public String  doesNotHaveTheWord;
    public String  smartLabelToApply;
    public String  sizeOperator; // "s_sl" "s_ss"
    public String  sizeUnit;     // "s_sb", "s_skb", "s_smb"
    //
    public boolean shouldAlwaysMarkAsImportant;
    public boolean shouldArchive;
    public boolean shouldMarkAsRead;
    public boolean shouldNeverMarkAsImportant;
    public boolean shouldNeverSpam;

    // Also possible but not used at the moment:
    //      public boolean excludeChats;
    //      public String  forwardTo;
    //      public boolean hasAttachment;
    //      public boolean shouldStar;
    //      public boolean shouldTrash;
    //      public String  size;

    public Filter() {
    }

    public Filter(String csvLine) {
        String[] fields = csvLine.split(",", 10);
        this.label = csvField(fields, 0);
        String flags = csvField(fields, 1);
        this.shouldAlwaysMarkAsImportant = flags != null && flags.indexOf(FLAG_shouldAlwaysMarkAsImportant) /**/ != -1;
        this.shouldArchive               = flags != null && flags.indexOf(FLAG_shouldArchive) /*              */ != -1;
        this.shouldMarkAsRead            = flags != null && flags.indexOf(FLAG_shouldMarkAsRead) /*           */ != -1;
        this.shouldNeverMarkAsImportant  = flags != null && flags.indexOf(FLAG_shouldNeverMarkAsImportant) /* */ != -1;
        this.shouldNeverSpam             = flags != null && flags.indexOf(FLAG_shouldNeverSpam) /*            */ != -1;
        this.from                        = csvField(fields, 2);
        this.to                          = csvField(fields, 3);
        this.subject                     = csvField(fields, 4);
        this.hasTheWord                  = csvField(fields, 5);
        this.doesNotHaveTheWord          = csvField(fields, 6);
        this.smartLabelToApply           = csvField(fields, 7);
        this.sizeOperator                = csvField(fields, 8);
        this.sizeUnit                    = csvField(fields, 9);
    }

    private static String csvField(String[] fields, int i) {
        if (i >= fields.length || fields[i] == null) {
            return null;
        }
        String s = fields[i];
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).replaceAll("\"\"", "\"");
        }
        return s;
    }

    public void fromXml(Attributes attr) {
        String name  = attr.getValue(NAME);
        String value = attr.getValue(VALUE);
        try {
            Field    field     = Filter.class.getDeclaredField(name);
            Class<?> fieldType = field.getType();
            if (fieldType == String.class) {
                field.set(this, value);
            } else if (fieldType == boolean.class && value.equals("true")) {
                field.set(this, true);
            } else if (fieldType == boolean.class && value.equals("false")) {
                field.set(this, false);
            } else {
                System.err.println("NO SUCH TYPE: " + fieldType);
            }
        } catch (NoSuchFieldException e) {
            System.err.println("NO SUCH FIELD: " + name + " = " + value);
        } catch (IllegalAccessException e) {
            System.err.println("CAN NOT ACCESS: " + name + " = " + value);
        } catch (IllegalArgumentException e) {
            System.err.println("CAN NOT SET: " + name + " = " + value);
        }

        // force some defaults:
        if (sizeOperator != null && sizeOperator.equals("s_sl")) {
            sizeOperator = null;
        }
        if (sizeUnit != null && sizeUnit.equals("s_smb")) {
            sizeUnit = null;
        }
    }

    public Stream<String> toXml() {
        return Stream.of(//
                Stream.of("    <" + ENTRY + ">"),//
                xmlEntries().entrySet().stream()//
                        .sorted(Entry.comparingByKey())//
                        .map(e -> "        <" + PROPERTY + " " + NAME + "='" + e.getKey() + "' " + VALUE + "='" + escapeXml(e.getValue()) + "'/>"),//
                Stream.of("    </" + ENTRY + ">")//
        ).flatMap(s -> s);
    }

    private String escapeXml(Object value) {
        String v = value.toString();
        v = v.replaceAll("\"", "&quot;");
        v = v.replaceAll("<", "&lt;");
        v = v.replaceAll(">", "&gt;");
        return v;
    }

    public Map<String, Object> xmlEntries() {
        return Arrays.stream(Filter.class.getDeclaredFields())//
                .filter(f -> !Modifier.isStatic(f.getModifiers()))//
                .filter(f -> get(f) != null && !get(f).equals(""))//
                .filter(f -> !(f.getType() == boolean.class && !((Boolean) get(f))))//
                .collect(Collectors.toMap(Field::getName, this::get));
    }

    private Object get(Field f) {
        try {
            return f.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getCsvHeader() {
        return LABEL_LABEL /*      */ + CSV_SEP + //
               FLAGS_LABEL /*      */ + CSV_SEP + //
               FROM_LABEL /*       */ + CSV_SEP + //
               TO_LABEL /*         */ + CSV_SEP + //
               SUBJECT_LABEL /*    */ + CSV_SEP + //
               WITH_LABEL /*       */ + CSV_SEP + //
               WITHOUT_LABEL /*    */ + CSV_SEP + //
               SMART_LABEL_LABEL /**/ + CSV_SEP + //
               SIZE_OPER_LABEL /*  */ + CSV_SEP + //
               SIZE_UNIT_LABEL;
    }

    public String toCsv() {
        return (wrap(label) /*             */ + CSV_SEP + //
                wrap(flags()) /*           */ + CSV_SEP + //
                wrap(from) /*              */ + CSV_SEP + //
                wrap(to) /*                */ + CSV_SEP + //
                wrap(subject) /*           */ + CSV_SEP + //
                wrap(hasTheWord) /*        */ + CSV_SEP + //
                wrap(doesNotHaveTheWord) /**/ + CSV_SEP + //
                wrap(smartLabelToApply) /* */ + CSV_SEP + //
                wrap(sizeOperator) /*      */ + CSV_SEP + //
                wrap(sizeUnit)).replaceFirst(",*$", "");
    }

    private String flags() {
        return ("" +//
                (shouldArchive /*              */ ? FLAG_shouldArchive /*              */ : FLAG_ABSENT) +//
                (shouldMarkAsRead /*           */ ? FLAG_shouldMarkAsRead /*           */ : FLAG_ABSENT) +//
                (shouldAlwaysMarkAsImportant /**/ ? FLAG_shouldAlwaysMarkAsImportant /**/ : FLAG_ABSENT) +//
                (shouldNeverMarkAsImportant /* */ ? FLAG_shouldNeverMarkAsImportant /* */ : FLAG_ABSENT) +//
                (shouldNeverSpam /*            */ ? FLAG_shouldNeverSpam /*            */ : FLAG_ABSENT)); //
    }

    private static String wrap(String v) {
        if (v == null) {
            return "";
        } else if (v.contains(CSV_SEP) || v.contains(QUOTE)) {
            return QUOTE + v.replaceAll(QUOTE, QUOTE + QUOTE) + QUOTE;
        } else {
            return v;
        }
    }
}
