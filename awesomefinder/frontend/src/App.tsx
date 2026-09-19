import { useEffect, useMemo, useState } from 'react'
import { ArrowUpRight, BriefcaseBusiness, ChevronDown, MapPin, Plus, Search, Sparkles, TrendingUp } from 'lucide-react'
import { getJobs } from './api'
import type { Job } from './types'

const filters = ['All jobs', 'Engineering', 'Product', 'Strategy', 'Sales']

function App() {
  const [jobs, setJobs] = useState<Job[]>([])
  const [activeFilter, setActiveFilter] = useState('All jobs')
  const [query, setQuery] = useState('')

  useEffect(() => {
    getJobs().then(setJobs)
  }, [])

  const visibleJobs = useMemo(() => jobs.filter((job) => {
    const matchesFilter = activeFilter === 'All jobs' || job.department === activeFilter
    const haystack = `${job.title} ${job.company} ${job.location}`.toLowerCase()
    return matchesFilter && haystack.includes(query.toLowerCase())
  }), [activeFilter, jobs, query])

  return (
    <main className="app-shell">
      <nav className="topbar">
        <a className="brand" href="/">
          <span className="brand-mark"><Sparkles size={16} /></span>
          <span>signal<span className="brand-accent">foundry</span></span>
        </a>
        <div className="nav-links">
          <a className="active" href="#jobs">Jobs</a>
          <a href="#companies">Companies</a>
          <a href="#signals">Signals</a>
        </div>
        <button className="profile-button" aria-label="Open profile menu">JD <ChevronDown size={15} /></button>
      </nav>

      <section className="hero" id="jobs">
        <div className="eyebrow"><span className="live-dot" /> LIVE MARKET INDEX <span className="eyebrow-divider" /> SEP 19, 2026</div>
        <div className="hero-grid">
          <div>
            <h1>Find the work.<br /><em>Read the signal.</em></h1>
            <p className="hero-copy">The job market, organized for people who want to move through it with intent.</p>
          </div>
          <div className="hero-note">
            <span className="note-number">01</span>
            <p>Every listing is a data point. Every pattern tells a story.</p>
          </div>
        </div>
        <div className="search-row">
          <div className="search-box"><Search size={19} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search roles, companies, or locations" /><kbd>/</kbd></div>
          <button className="filter-button"><MapPin size={17} /> Any location <ChevronDown size={15} /></button>
          <button className="add-button" aria-label="Add a saved search"><Plus size={19} /></button>
        </div>
      </section>

      <section className="workspace">
        <div className="section-head">
          <div><p className="section-kicker">DISCOVER</p><h2>Open roles <span>{visibleJobs.length}</span></h2></div>
          <button className="sort-button">Most relevant <ChevronDown size={15} /></button>
        </div>
        <div className="filter-tabs">{filters.map((filter) => <button key={filter} className={activeFilter === filter ? 'selected' : ''} onClick={() => setActiveFilter(filter)}>{filter}</button>)}</div>
        <div className="job-list">{visibleJobs.map((job) => <JobRow key={job.id} job={job} />)}</div>
      </section>

      <aside className="signal-panel" id="signals">
        <div className="signal-icon"><TrendingUp size={20} /></div>
        <div><p className="section-kicker">COMING INTO FOCUS</p><h2>Hiring velocity is becoming a signal.</h2><p className="signal-copy">Track where companies are building, which functions are accelerating, and what the market is quietly saying next.</p></div>
        <button className="signal-link">Explore signals <ArrowUpRight size={16} /></button>
      </aside>
    </main>
  )
}

function JobRow({ job }: { job: Job }) {
  return <article className="job-row"><div className="company-avatar">{job.company.slice(0, 1)}</div><div className="job-main"><div className="job-title-line"><h3>{job.title}</h3><span className={`trend ${job.trend.toLowerCase()}`}>{job.trend}</span></div><p>{job.company} <span className="muted-dot">·</span> {job.location}</p></div><div className="job-meta"><span>{job.department}</span><span>{job.posted}</span></div><a className="row-arrow" href={`#job-${job.id}`} aria-label={`View ${job.title}`}><ArrowUpRight size={18} /></a></article>
}

export default App
