import predictbackend.*;
import java.util.*;
public class Probe {
  public static void main(String[] a) throws Exception {
    System.setProperty("db.path", a[0]);
    try (Db db = Db.open()) {
      List<Signal> all = Detector.detectAll(db);
      long co = all.stream().filter(s -> !s.company.isEmpty()).count();
      System.out.println("TOTAL signals: " + all.size() + "  (company " + co + ", sector " + (all.size()-co) + ")");
      System.out.println("was: 143 total, 54 distinct\n");
      Map<String,List<Signal>> by = new TreeMap<>();
      for (Signal s : all) by.computeIfAbsent(s.company.isEmpty() ? "~SECTOR" : s.company, k->new ArrayList<>()).add(s);
      for (var e : by.entrySet()) {
        System.out.println(e.getKey() + "  (" + e.getValue().size() + ")");
        for (Signal s : e.getValue())
          System.out.printf("   %-9s %-20s %-28s %s%n", s.stanceName().toUpperCase(), s.kind, s.metric,
            s.claim.length() > 96 ? s.claim.substring(0,96)+"..." : s.claim);
      }
      System.out.println("\ndistinct claims: " + all.stream().map(s->s.claim).distinct().count() + " of " + all.size());
      System.out.println("max per company: " + by.entrySet().stream().filter(x->!x.getKey().equals("~SECTOR")).mapToInt(x->x.getValue().size()).max().orElse(0));
      System.out.println("every signal has evidence: " + all.stream().allMatch(s->!s.evidence.isEmpty()));
    }
  }
}
