package predictbackend;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Grouper {
    
    // counts how many postings fall in each bucket of one label.
    public static Map<String, Integer> countBy(List<Posting> postings, String field) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Posting p : postings) {
            String k = valueOf(p, field);
            counts.put(k, counts.getOrDefault(k, 0) + 1);
        }
        return counts;
    }

    // counts how many postings fall in each bucket of combinations of two labels
    public static Map<String, Integer> countBy(List<Posting> postings,
                                               String f1, String f2) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Posting p : postings) {
            String k = valueOf(p, f1) + " | " + valueOf(p, f2);
            counts.put(k, counts.getOrDefault(k, 0) + 1);
        }
        return counts;
    }

    // filters data
    public static List<Posting> where(List<Posting> postings,
                                      String field, String value) {
        List<Posting> out = new ArrayList<>();
        for (Posting p : postings) {
            if (valueOf(p, field).equals(value)) out.add(p);
        }
        return out;
    }

    // sorts the count map, biggest to smallest
    public static void print(String heading, Map<String, Integer> counts) {
        System.out.println("--- " + heading + " ---");
        counts.entrySet().stream()
              .sorted((a, b) -> b.getValue() - a.getValue())
              .forEach(e -> System.out.printf("%5d  %s%n", e.getValue(), e.getKey()));
        System.out.println();
    }

    private static String valueOf(Posting p, String field) {
        switch (field) {
            case "seniority": return p.seniority == null ? "?" : p.seniority;
            case "function":  return p.function  == null ? "?" : p.function;
            case "country":   return p.country   == null ? "?" : p.country;
            case "city":      return p.city      == null ? "?" : p.city;
            case "company":   return p.company;
            case "source":    return p.source;
            default: throw new IllegalArgumentException("unknown field: " + field);
        }
    }

}
