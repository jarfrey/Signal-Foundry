package predictbackend;

import java.util.ArrayList;
import java.util.List;

public class Signal {
    
    // FUNCTION_BOOTSTRAP = leaders with no ICs
    // CONVERGENCE = 3+ companies hiring for same function
    // BEACHHEAD = company has 1-3 roles in a company out of 25+ openings
    public String kind;

    public String company;
    public String claim;
    public double confidence;   // 0.0 - 1.0
    public List<Posting> evidence = new ArrayList<>();

    public Signal(String kind, String company, String claim, double confidence) {
        this.kind = kind;
        this.company = company;
        this.claim = claim;
        this.confidence = confidence;
    }

    public String toString() {
        return String.format("[%.2f] %-20s %s  (%d postings)", confidence, kind, claim, evidence.size());
    }

}
