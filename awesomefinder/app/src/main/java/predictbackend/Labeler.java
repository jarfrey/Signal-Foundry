package predictbackend;

public class Labeler {
    
    public static void label(Posting p) {
        p.seniority = seniority(p.title);
        p.function = function(p.title);
        p.country = country(p.locationRaw);
        p.city = city(p.locationRaw);
    }

    public static String seniority(String title) {

        if (title == null) {
            return "IC";
        }

        String t = title.toLowerCase();

        if (t.contains("chief") || t.contains("cto") || t.contains("cfo") || t.contains("ceo") || t.contains("coo")) return "EXEC";
        if (t.contains("vp") || t.contains("vice president")) return "VP";
        if (t.contains("head of")) return "HEAD";
        if (t.contains("director")) return "DIRECTOR";
        if (t.contains("manager") || t.contains("lead") || t.contains("principal")) return "MANAGER";
        if (t.contains("senior") || t.contains("sr.") || t.contains("staff")) return "SENIOR";
        if (t.contains("intern") || t.contains("junior") || t.contains("new grad")) return "JUNIOR";

        return "IC";

    }

    public static String function(String title) {

        if (title == null) {
            return "OTHER";
        }

        String t = title.toLowerCase();

        if (t.contains("compliance") || t.contains("regulatory") || t.contains("aml") || t.contains("kyc")) return "COMPLIANCE";
        if (t.contains("legal") || t.contains("counsel")) return "LEGAL";
        if (t.contains("security") || t.contains("infosec")) return "SECURITY";
        if (t.contains("data scientist") || t.contains("data engineer") || t.contains("machine learning") || t.contains("analytics") || t.contains("data analyst")) return "DATA";
        if (t.contains("engineer") || t.contains("developer") || t.contains("software") || t.contains("sre") || t.contains("devops") || t.contains("architect")) return "ENGINEERING";
        if (t.contains("product manager") || t.contains("product owner")) return "PRODUCT";
        if (t.contains("designer") || t.contains("design")) return "DESIGN";
        if (t.contains("sales") || t.contains("account executive") || t.contains("account manager") || t.contains("business development")) return "SALES";
        if (t.contains("marketing") || t.contains("brand") || t.contains("growth")) return "MARKETING";
        if (t.contains("recruit") || t.contains("talent") || t.contains("people")) return "PEOPLE";
        if (t.contains("finance") || t.contains("accountant") || t.contains("accounting") || t.contains("controller") || t.contains("audit") || t.contains("treasury")) return "FINANCE";
        if (t.contains("support") || t.contains("customer success")) return "SUPPORT";
        if (t.contains("operations") || t.contains("program manager") || t.contains("project manager") || t.contains("strategy")) return "OPERATIONS";

        return "OTHER";

    }

    public static String country(String location) {

        if (location == null || location.isEmpty()) {
            return "UNKNOWN";
        }

        String l = location.toLowerCase();

        if (l.contains("united states") || l.contains("usa")) return "US";
        if (l.contains("united kingdom") || l.contains("england") || l.contains("scotland")) return "GB";
        if (l.contains("germany") || l.contains("deutschland")) return "DE";
        if (l.contains("ireland")) return "IE";
        if (l.contains("france")) return "FR";
        if (l.contains("netherlands")) return "NL";
        if (l.contains("spain")) return "ES";
        if (l.contains("poland")) return "PL";
        if (l.contains("canada")) return "CA";
        if (l.contains("india")) return "IN";
        if (l.contains("singapore")) return "SG";
        if (l.contains("japan")) return "JP";
        if (l.contains("australia")) return "AU";
        if (l.contains("israel")) return "IL";
        if (l.contains("uae") || l.contains("united arab emirates")) return "AE";

        if (l.contains("london")) return "GB";
        if (l.contains("berlin") || l.contains("munich") || l.contains("hamburg") || l.contains("frankfurt")) return "DE";
        if (l.contains("dublin")) return "IE";
        if (l.contains("paris")) return "FR";
        if (l.contains("amsterdam")) return "NL";
        if (l.contains("madrid") || l.contains("barcelona")) return "ES";
        if (l.contains("warsaw") || l.contains("krakow")) return "PL";
        if (l.contains("toronto") || l.contains("vancouver")) return "CA";
        if (l.contains("bangalore") || l.contains("bengaluru") || l.contains("mumbai") || l.contains("hyderabad")) return "IN";
        if (l.contains("tokyo")) return "JP";
        if (l.contains("sydney") || l.contains("melbourne")) return "AU";
        if (l.contains("tel aviv")) return "IL";
        if (l.contains("dubai")) return "AE";

        if (l.contains("hawthorne") || l.contains("san francisco")
                || l.contains("new york")  || l.contains("nyc")
                || l.contains("seattle")   || l.contains("chicago")
                || l.contains("austin")    || l.contains("boston")
                || l.contains("denver")    || l.contains("atlanta")
                || l.contains("los angeles") || l.contains("palo alto")
                || l.contains("redmond")   || l.contains("washington")
                || l.contains("brownsville") || l.contains("mcgregor")
                || l.contains("starbase")
                || l.contains(", ca") || l.contains(", ny") || l.contains(", wa")
                || l.contains(", tx") || l.contains(", ma") || l.contains(", il")
                || l.contains(", co") || l.contains(", fl") || l.contains(", va"))
            return "US";
        
        if (l.contains("remote")) return "REMOTE_UNKNOWN";

        return "UNKNOWN";

    }

    public static String city(String location) {

        if (location == null || location.isEmpty()) {
            return "";
        }

        String l = location;

        int dash = l.indexOf(" - ");
        if (l.toLowerCase().startsWith("remote") && dash >= 0) {
            l = l.substring(dash + 3);
        }
        int comma = l.indexOf(',');
        if (comma > 0) l = l.substring(0, comma);

        return l.trim();

    }

}
