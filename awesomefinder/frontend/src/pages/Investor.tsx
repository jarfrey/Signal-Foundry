import { useEffect, useMemo, useState } from "react";
import { getCompanyTrends } from "../api";
import type { CompanyTrend } from "../types";
import { ArrowLeft, ChevronDown } from "lucide-react";
type Sort = "velocity" | "volume";

export default function Investor() {
  const [trends, setTrends] = useState<CompanyTrend[]>([]);
  const [live, setLive] = useState(true);
  const [loading, setLoading] = useState(true);
  const [sort, setSort] = useState<Sort>("velocity");

  useEffect(() => {
    getCompanyTrends().then((result) => {
      setTrends(result.trends);
      setLive(result.live);
      setLoading(false);
    });
  }, []);

  const ordered = useMemo(
    () =>
      [...trends].sort((a, b) =>
        sort === "velocity"
          ? b.velocity - a.velocity
          : b.openRoles - a.openRoles,
      ),
    [sort, trends],
  );

  const totals = useMemo(
    () => ({
      roles: trends.reduce((sum, t) => sum + t.openRoles, 0),
      fresh: trends.reduce((sum, t) => sum + t.newThisMonth, 0),
    }),
    [trends],
  );

  // True only if every card came back with a model-written read.
  const allModelWritten =
    trends.length > 0 && trends.every((t) => t.insightSource === "nemotron");

  return (
    <main className="shell dark">
      <nav className="topbar">
        <a className="brand" href="#/">
          <span className="brand-mark" />
          <span>
            signal<span className="brand-accent">foundry</span>
          </span>
        </a>
        <a className="back" href="#/">
          <ArrowLeft size={14} /> Back
        </a>
      </nav>

      <section className="band">
        <p className="kicker">FOR INVESTORS</p>
        <h1>Hiring signals</h1>
        <p>
          {trends.length} companies · {totals.roles} open roles · {totals.fresh}{" "}
          posted in the last 30 days. Velocity compares roles posted in the last
          30 days against the 30 days before that.
        </p>

        <div className="tabs">
          <button
            className={sort === "velocity" ? "on" : ""}
            onClick={() => setSort("velocity")}
          >
            Fastest moving
          </button>
          <button
            className={sort === "volume" ? "on" : ""}
            onClick={() => setSort("volume")}
          >
            Most open roles
          </button>
        </div>
      </section>

      <section className="band" style={{ paddingTop: 22, paddingBottom: 60 }}>
        {loading && <p className="empty">Reading the boards…</p>}

        <div className="grid">
          {ordered.map((company) => (
            <CompanyCard key={company.id} company={company} />
          ))}
        </div>

        {!loading && !live && (
          <p className="notice">
            SAMPLE DATA — /api/trends is not responding. Run the backend to see
            real companies.
          </p>
        )}
        {!loading && live && !allModelWritten && (
          <p className="notice">
            Some reads are computed summaries rather than Nemotron output — set
            NEMOTRON_API_KEY and restart the backend for model-written analysis.
          </p>
        )}
      </section>
    </main>
  );
}

function CompanyCard({ company }: { company: CompanyTrend }) {
  // State to track if the summary accordion is open or closed
  const [isOpen, setIsOpen] = useState(false);

  const direction =
    company.trend === "Growing"
      ? "up"
      : company.trend === "Cooling"
        ? "down"
        : "flat";

  return (
    <article className="co">
      <div className="co-head">
        <div>
          <h3>{company.company}</h3>
          <p className="co-sector">{company.sector}</p>
        </div>
        <div className={`velocity ${direction}`}>
          {company.velocity > 0 ? "+" : ""}
          {Math.round(company.velocity)}%<span>30D</span>
        </div>
      </div>

      {/* Accordion Toggle Header */}
      <button
        type="button"
        className={`co-toggle ${isOpen ? "active" : ""}`}
        onClick={() => setIsOpen(!isOpen)}
        aria-expanded={isOpen}
      >
        <span>{isOpen ? "Hide AI Summary" : "View AI Summary"}</span>
        <ChevronDown size={16} className={`chevron ${isOpen ? "open" : ""}`} />
      </button>

      {/* Collapsible Content Section */}
      {isOpen && (
        <div className="co-body">
          {company.headline && (
            <p className="co-headline">{company.headline}</p>
          )}

          <p className="co-read">{company.read}</p>

          {company.bullets?.length > 0 && (
            <ul className="co-bullets">
              {company.bullets.map((bullet) => (
                <li key={bullet}>{bullet}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      {/* Department Mix Bars */}
      <div className="bars">
        {company.mix.map((slice) => (
          <div className="bar" key={slice.label}>
            <span>{slice.label}</span>
            <i>
              <em style={{ width: `${Math.round(slice.share * 100)}%` }} />
            </i>
            <span>{Math.round(slice.share * 100)}%</span>
          </div>
        ))}
      </div>

      <div className="co-foot">
        <span>
          <b>{company.openRoles}</b> open
        </span>
        <span>
          <b>{company.newThisMonth}</b> new
        </span>
        <span>{company.topLocation}</span>
        <span className={`prov ${company.insightSource}`}>
          {company.insightSource === "nemotron" ? "Nemotron" : "Computed"}
        </span>
      </div>
    </article>
  );
}
