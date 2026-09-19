import app.Posting;
import java.util.List;
import java.util.Set;

public interface Source {

    String name();
    Result fetch(String handle) throws Exception;

    class Result {
        public final List<Posting> postings;
        public final Set<String> offices;
        public Result(List<Posting> postings, Set<String> offices) {
            this.postings = postings;
            this.offices = offices;
        }
    }
    
}