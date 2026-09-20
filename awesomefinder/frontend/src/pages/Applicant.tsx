import { useEffect, useMemo, useRef, useState } from 'react'
import { ArrowLeft, ArrowUpRight, Search, Upload } from 'lucide-react'
import { getJobs, rankJobs } from '../api'
import type { Job, ScoredJob } from '../types'

const DEPARTMENTS = ['All roles', 'Engineering', 'Product', 'Strategy', 'Sales']

export default function Applicant() {
  const [jobs, setJobs] = useState<Job[]>([])
  const [live, setLive] = useState(true)
  const [department, setDepartment] = useState('All roles')
  const [query, setQuery] = useState('')
  const [resume, setResume] = useState('')
  const [ranked, setRanked] = useState<ScoredJob[] | null>(null)
  const fileInput = useRef<HTMLInputElement>(null)

  useEffect(() => {
    getJobs().then((result) => {
      setJobs(result.jobs)
      setLive(result.live)
    })
  }, [])

  // A new fetch or a cleared resume invalidates any existing ranking.
  useEffect(() => {
    if (!resume.trim()) setRanked(null)
  }, [resume])

  const visible = useMemo(() => {
    const list: (Job | ScoredJob)[] = ranked ?? jobs
    const needle = query.trim().toLowerCase()
    return list.filter((job) => {
      const inDept = department === 'All roles' || job.department === department
      const haystack = `${job.title} ${job.company} ${job.location}`.toLowerCase()
      return inDept && haystack.includes(needle)
    })
  }, [department, jobs, query, ranked])

  async function onFile(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) return
    setResume(await file.text())
    event.target.value = ''
  }

  return (
    <main className="shell">
      <nav className="topbar">
        <a className="brand" href="#/">
          <span className="brand-mark" />
          <span>signal<span className="brand-accent">foundry</span></span>
        </a>
        <a className="back" href="#/">
          <ArrowLeft size={14} /> Back
        </a>
      </nav>

      <section className="band">
        <p className="kicker">FOR APPLICANTS</p>
        <h1>Open roles</h1>
        <p>
          Pulled from company job boards. Add your resume and the list reorders by how
          closely each posting matches what you have actually done.
        </p>

        <div className="resume">
          <div>
            <textarea
              value={resume}
              onChange={(event) => setResume(event.target.value)}
              placeholder="Paste your resume here, or upload a .txt file."
            />
          </div>
          <div className="resume-side">
            <button
              className="btn btn-solid"
              disabled={resume.trim().length < 40}
              onClick={() => setRanked(rankJobs(resume, jobs))}
            >
              Match me
            </button>
            <button className="btn btn-ghost" onClick={() => fileInput.current?.click()}>
              <Upload size={14} /> Upload file
            </button>
            <input
              ref={fileInput}
              type="file"
              accept=".txt,.md,text/plain"
              hidden
              onChange={onFile}
            />
            <p className="resume-note">
              {ranked
                ? 'Sorted by match. Clear the box to go back to newest first.'
                : 'Nothing is uploaded anywhere — matching runs in your browser.'}
            </p>
          </div>
        </div>

        <div className="search-row">
          <div className="search-box">
            <Search size={18} />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search roles, companies, or locations"
            />
          </div>
        </div>

        <div className="tabs">
          {DEPARTMENTS.map((name) => (
            <button
              key={name}
              className={department === name ? 'on' : ''}
              onClick={() => setDepartment(name)}
            >
              {name}
            </button>
          ))}
        </div>
      </section>

      <section className="band" style={{ paddingTop: 28, paddingBottom: 60 }}>
        <div className="section-head">
          <h2>
            {ranked ? 'Best matches' : 'Newest'}
            <span className="count">{visible.length}</span>
          </h2>
        </div>

        <div className="rows">
          {visible.map((job) => (
            <JobRow key={job.id} job={job} />
          ))}
          {visible.length === 0 && <p className="empty">No roles match those filters.</p>}
        </div>

        {!live && (
          <p className="notice">
            SAMPLE DATA — the backend at /api/jobs is not responding.
          </p>
        )}
      </section>
    </main>
  )
}

function JobRow({ job }: { job: Job | ScoredJob }) {
  const score = 'score' in job ? job.score : null
  const trend = job.trend ?? 'Steady'

  return (
    <article className="row">
      <div className="avatar">{(job.company || '?').slice(0, 1)}</div>
      <div>
        <div className="row-title">
          <h3>{job.title}</h3>
          <span className={`tag ${trend.toLowerCase()}`}>{trend}</span>
        </div>
        <p>
          {job.company} <span className="dot">·</span> {job.location}
        </p>
      </div>
      <div className="row-meta">
        {score !== null && (
          <span className={`score ${score < 40 ? 'weak' : ''}`}>{score}% match</span>
        )}
        <span>{job.department}</span>
        <span>{job.posted}</span>
      </div>
      <a className="arrow" href={`#job-${job.id}`} aria-label={`View ${job.title}`}>
        <ArrowUpRight size={18} />
      </a>
    </article>
  )
}
