package predictbackend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * Covers the parts that were wrong before and are easy to break again:
 * the two boards' differing JSON shapes, date normalisation, the trend
 * arithmetic, and unwrapping the model response.
 */
public class PipelineTest {

    // Trimmed from a real boards-api.greenhouse.io response.
    private static final String GREENHOUSE_JSON = """
            {"jobs":[
              {"id":8172487,
               "title":"Abuse Investigator",
               "absolute_url":"https://stripe.com/jobs/search?gh_jid=8172487",
               "location":{"name":"Dublin"},
               "departments":[{"name":"Risk"}],
               "first_published":"2026-09-03T13:30:34-04:00",
               "updated_at":"2026-09-12T13:25:25-04:00"},
              {"id":8172488,
               "title":"Staff Security Engineer",
               "absolute_url":"https://example.com/2",
               "location":{"name":"Remote - US"},
               "departments":[],
               "first_published":null,
               "updated_at":"2026-09-10T00:00:00-04:00"}
            ],"meta":{"total":2}}
            """;

    // Trimmed from a real api.lever.co response -- note the bare array.
    private static final String LEVER_JSON = """
            [
              {"id":"ac978161-6f46-4f6b-ad9e-a258e642751c",
               "text":"Administrative Business Partner",
               "hostedUrl":"https://jobs.lever.co/palantir/ac978161",
               "createdAt":1711403416463,
               "country":"GB",
               "categories":{"location":"London, United Kingdom","team":"Administrative"}}
            ]
            """;

    @Test
    public void greenhouseReadsItsOwnFieldNames() {
        List<Posting> postings = new Greenhouse().parse("stripe", GREENHOUSE_JSON);

        assertEquals(2, postings.size());
        Posting p = postings.get(0);
        assertEquals("Abuse Investigator", p.title);
        assertEquals("Dublin", p.locationRaw);
        assertEquals("Risk", p.departmentRaw);
        assertEquals("greenhouse:stripe:8172487", p.postingId);
        assertEquals("https://stripe.com/jobs/search?gh_jid=8172487", p.url);
        assertEquals("2026-09-03T17:30:34Z", p.postedAt);
    }

    @Test
    public void greenhouseFallsBackToUpdatedAtWhenNeverPublished() {
        Posting p = new Greenhouse().parse("stripe", GREENHOUSE_JSON).get(1);
        assertEquals("2026-09-10T04:00:00Z", p.postedAt);
    }

    @Test
    public void leverReadsABareArrayAndItsOwnFieldNames() {
        List<Posting> postings = new Lever().parse("palantir", LEVER_JSON);

        assertEquals(1, postings.size());
        Posting p = postings.get(0);
        // Lever calls the title "text"; Greenhouse calls it "title".
        assertEquals("Administrative Business Partner", p.title);
        assertEquals("London, United Kingdom", p.locationRaw);
        assertEquals("Administrative", p.departmentRaw);
        assertEquals("GB", p.countryHint);
        assertEquals("2024-03-25T21:50:16.463Z", p.postedAt);
    }

    @Test
    public void labelerPrefersTheBoardsOwnCountryCode() {
        Posting p = new Lever().parse("palantir", LEVER_JSON).get(0);
        Labeler.label(p);
        assertEquals("GB", p.country);
        assertEquals("SENIOR", Labeler.seniority("Staff Security Engineer"));
        assertEquals("SECURITY", Labeler.function("Staff Security Engineer"));
    }

    @Test
    public void parserRejectsAPayloadWithNoJobs() {
        assertEquals(0, new Parser("[]").count());
        try {
            new Parser("{\"nothing\":1}");
            throw new AssertionError("expected an explanatory failure");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("no job array"));
        }
    }

    @Test
    public void missingFieldsYieldEmptyStringsNotTheWordNull() {
        Posting p = new Greenhouse().parse("stripe", GREENHOUSE_JSON).get(1);
        assertEquals("", p.departmentRaw);
        assertNotNull(p.title);
    }

    @Test
    public void velocityComparesTheTwoThirtyDayWindows() {
        assertEquals(0.0, CompanyStats.velocity(0, 0), 0.001);
        assertEquals(100.0, CompanyStats.velocity(10, 5), 0.001);
        assertEquals(-75.0, CompanyStats.velocity(2, 8), 0.001);
        // Growth from a zero baseline has no percentage; make sure it is
        // bounded rather than infinite or a divide-by-zero.
        assertTrue(CompanyStats.velocity(5, 0) <= 999);
    }

    @Test
    public void tinySamplesDoNotGetCalledATrend() {
        // 1 role after 0 is +100% but means nothing.
        assertEquals("Steady", CompanyStats.trend(100, 1, 0));
        assertEquals("Growing", CompanyStats.trend(72, 254, 148));
        assertEquals("Cooling", CompanyStats.trend(-74, 5, 19));
    }

    @Test
    public void modelResponseIsUnwrappedFromTheApiEnvelope() {
        String envelope = """
                {"id":"chatcmpl-1","choices":[{"index":0,"message":{"role":"assistant",
                "content":"{\\"headline\\":\\"Up sharply\\",\\"read\\":\\"More roles.\\",\\"bullets\\":[\\"a\\"]}"},
                "finish_reason":"stop"}],"model":"nvidia/nemotron-3-ultra-550b-a55b"}
                """;
        String content = InsightGeneratorService.extractContent(envelope);
        assertTrue(content.contains("Up sharply"));
        assertEquals("Up sharply",
                new org.json.JSONObject(InsightGeneratorService.stripFence(content)).getString("headline"));
    }

    @Test
    public void markdownFencesAndPreambleAreStripped() {
        String fenced = "Here you go:\n```json\n{\"headline\":\"h\"}\n```";
        assertEquals("h",
                new org.json.JSONObject(InsightGeneratorService.stripFence(fenced)).getString("headline"));
    }

    @Test
    public void apiErrorsAreRaisedRatherThanReturnedAsProse() {
        try {
            InsightGeneratorService.extractContent("{\"error\":{\"message\":\"bad key\"}}");
            throw new AssertionError("expected the error to propagate");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("bad key"));
        }
    }

    /**
     * Regression: Nemotron 3 is a reasoning model, so a tight max_tokens was
     * spent reasoning and the answer came back truncated. That surfaced as a
     * confusing JSON syntax error; it must name the real cause instead.
     */
    @Test
    public void truncatedAnswersAreReportedAsTruncation() {
        String truncated = """
                {"choices":[{"index":0,"message":{"role":"assistant",
                "content":"{\\"headline\\":\\"Partial"},"finish_reason":"length"}]}
                """;
        try {
            InsightGeneratorService.extractContent(truncated);
            throw new AssertionError("expected truncation to be reported");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("truncated"));
        }
    }

    @Test
    public void emptyAnswerAtTheTokenLimitNamesTheTokenLimit() {
        String empty = """
                {"choices":[{"index":0,"message":{"role":"assistant","content":""},
                "finish_reason":"length"}]}
                """;
        try {
            InsightGeneratorService.extractContent(empty);
            throw new AssertionError("expected an explanatory failure");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("ran out of tokens"));
        }
    }

    @Test
    public void fallbackReadQuotesTheRealNumbers() {
        CompanyStats s = new CompanyStats();
        s.slug = "stripe";
        s.openRoles = 665;
        s.newLast30 = 254;
        s.prev30 = 148;
        s.velocity = 72;
        s.trend = "Growing";
        s.topFunction = "ENGINEERING";
        s.topLocation = "San Francisco";

        InsightGeneratorService.Insight insight = InsightGeneratorService.fallback(s, "test");
        assertEquals("fallback", insight.source());
        assertTrue(insight.read().contains("665"));
        assertTrue(insight.read().contains("254"));
        assertTrue(insight.read().contains("148"));
    }

    @Test
    public void convergenceGroupsByCountryNotByCompany() {
        // Three companies hiring the same function in the same country is the
        // cluster the rule is meant to find.
        List<Posting> postings = List.of(
                posting("acme", "ENGINEERING", "US"),
                posting("beta", "ENGINEERING", "US"),
                posting("gamma", "ENGINEERING", "US"));

        List<Signal> signals = Detector.convergence(postings);
        assertTrue("expected a convergence signal", signals.size() >= 3);
        assertTrue(signals.get(0).claim.contains("ENGINEERING"));
        assertTrue(signals.get(0).claim.contains("US"));
        // Every signal must name a company so it can attach to a card.
        signals.forEach(s -> assertTrue(!s.company.isEmpty()));
    }

    @Test
    public void humanAgeIsReadable() {
        assertEquals("recently", Dates.humanAge(null));
        assertEquals("today", Dates.humanAge(Dates.nowIso()));
        assertEquals("2d ago", Dates.humanAge(Dates.daysAgo(2)));
    }

    private static Posting posting(String company, String function, String country) {
        Posting p = new Posting();
        p.company = company;
        p.function = function;
        p.country = country;
        p.seniority = "IC";
        p.title = "Engineer";
        p.locationRaw = country;
        p.postingId = company + ":1";
        return p;
    }
}
