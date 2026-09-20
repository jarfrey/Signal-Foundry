import { ArrowRight } from 'lucide-react'

export default function Home() {
  return (
    <main className="shell">
      <nav className="topbar">
        <span className="brand">
          <span className="brand-mark" />
          <span>signal<span className="brand-accent">foundry</span></span>
        </span>
      </nav>

      <section className="home">
        <p className="kicker">LIVE MARKET INDEX</p>
        <h1>
          Find the work.
          <br />
          <em>Read the signal.</em>
        </h1>
        <p className="home-copy">
          Every job posting a company publishes is a public statement about where it
          is spending next. We collect them, and read them two ways.
        </p>

        <div className="paths">
          <a className="path" href="#/applicant">
            <h2>For applicants</h2>
            <p>
              Browse open roles across company boards, then paste your resume to see
              which ones actually line up with your experience.
            </p>
            <span className="path-go">
              BROWSE ROLES <ArrowRight size={14} />
            </span>
          </a>

          <a className="path invert" href="#/investor">
            <h2>For investors</h2>
            <p>
              Hiring velocity, team composition and pace of change across the companies
              you track, read out of their job boards.
            </p>
            <span className="path-go">
              VIEW SIGNALS <ArrowRight size={14} />
            </span>
          </a>
        </div>
      </section>

      <footer className="foot">Data from public Greenhouse and Lever job boards.</footer>
    </main>
  )
}
