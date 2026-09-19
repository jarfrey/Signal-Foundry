package predictbackend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Detector {
    
    static final Set<String> LEADER = Set.of("HEAD", "DIRECTOR", "VP", "EXEC");
    static final Set<String> INDIVIDUAL = Set.of("IC", "SENIOR", "JUNIOR", "MANAGER");

    // run every rule and return all signals found
    public static List<Signal> detectAll(List<Posting> postings) {
        List<Signal> signals = new ArrayList<>();
        signals.addAll(functionBootstrap(postings));
        signals.addAll(convergence(postings));
        signals.addAll(beachhead(postings));
        signals.sort((a, b) -> Double.compare(b.confidence, a.confidence));
        return signals;
    }

    // RULE 1: leader with no team
    // a head of X with zero ICs means they're creating that team
    public static List<Signal> functionBootstrap(List<Posting> postings) {

        List<Signal> out = new ArrayList<>();

        Set<String> pairs = new HashSet<>();
        for (Posting p : postings) {
            pairs.add(p.company + "|" + p.function);
        }

        for (String pair : pairs) {

            String company  = pair.split("\\|")[0];
            String function = pair.split("\\|")[1];
            if (function.equals("OTHER")) continue;

            List<Posting> leaders = new ArrayList<>();
            int icCount = 0;

            for (Posting p : postings) {
                if (!p.company.equals(company) || !p.function.equals(function)) continue;
                if (LEADER.contains(p.seniority)) leaders.add(p);
                else if (INDIVIDUAL.contains(p.seniority)) icCount++;
            }

            if (!leaders.isEmpty() && icCount == 0) {
                Signal s = new Signal("FUNCTION_BOOTSTRAP", company, company + " is hiring a " + function + " leader with no individual contributors open in that function — likely standing up a capability that doesn't exist yet", 0.75);
                s.evidence.addAll(leaders);
                out.add(s);
            }

        }

        return out;

    }

    // RULE 2: peers converging
    // three competitors staffing the same function in the same country is an insight that exists only BETWEEN companies
    public static List<Signal> convergence(List<Posting> postings) {

        List<Signal> out = new ArrayList<>();

        Set<String> combos = new HashSet<>();
        for (Posting p : postings) {
            combos.add(p.function + "|" + p.company);
        }

        for (String combo : combos) {

            String function = combo.split("\\|")[0];
            String country  = combo.split("\\|")[1];
            if (function.equals("OTHER") || country.equals("UNKNOWN") || country.equals("REMOTE_UNKNOWN")) continue;

            Set<String> companies = new HashSet<>();
            List<Posting> hits = new ArrayList<>();

            for (Posting p : postings) {
                if (p.function.equals(function) && p.country.equals(country)) {
                    companies.add(p.company);
                    hits.add(p);
                }
            }

            if (companies.size() >= 3) {
                Signal s = new Signal("CONVERGENCE", "", companies.size() + " companies on the watchlist are all hiring " + function + " roles in " + country, 0.65);
                s.evidence.addAll(hits.subList(0, Math.min(6, hits.size())));
                out.add(s);
            }

        }

        return out;

    }

    // RULE 3: geographic beechhead
    // a handful of roles in a country where they're otherwise absent reads as market entry
    public static List<Signal> beachhead(List<Posting> postings) {

        List<Signal> out = new ArrayList<>();

        Set<String> pairs = new HashSet<>();
        for (Posting p : postings) { 
            pairs.add(p.company + "|" + p.country);
        }

        for (String pair : pairs) {

            String company = pair.split("\\|")[0];
            String country = pair.split("\\|")[1];
            if (country.equals("UNKNOWN") || country.equals("REMOTE_UNKNOWN")) continue;

            List<Posting> here = new ArrayList<>();
            int total = 0;

            for (Posting p : postings) {
                if (!p.company.equals(company)) continue;
                total++;
                if (p.country.equals(country)) here.add(p);
            }

            if (total >= 25 && here.size() >= 1 && here.size() <= 3) {
                Signal s = new Signal("BEACHHEAD", company, company + "has only " + here.size() + " of " + total + " open roles in " + country + " - possible early market entry", 0.55);
                s.evidence.addAll(here);
                out.add(s);
            }

        }

        return out;

    }

}
