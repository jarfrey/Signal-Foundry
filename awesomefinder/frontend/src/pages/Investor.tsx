import { useEffect, useMemo, useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { getCompanyTrends } from '../api'
import type { CompanyTrend } from '../types'

type Sort = 'velocity' | 'volume'

export default function Investor() {
  const [trends, setTrends] = useState<CompanyTrend[]>([])
  const [sort, setSort] = useState<Sort>('velocity')

  useEffect(() => {
    getCompanyTrends().then(setTrends)
  }, [])

  const ordered = useMemo(
    () =>
      [...trends].sort((a, b) =>
        sort === 'velocity' ? b.velocity - a.velocity : b.openRoles - a.openRoles
      ),
    [sort, trends]
  )

  const totals = useMemo(
    () => ({
      roles: trends.reduce((sum, t) => sum + t.openRoles, 0),
      fresh: trends.reduce((sum, t) => sum + t.newThisMonth, 0),
    }),
    [trends]
  )

  return (
    <main className="shell dark">
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
        <p className="kicker">FOR INVESTORS</p>
        <h1>Hiring signals</h1>
        <p>
          {trends.length} companies · {totals.roles} open roles · {totals.fresh} posted in
          the last 30 days. Velocity compares this month's openings against the prior one.
        </p>

        <div className="tabs">
          <button className={sort === 'velocity' ? 'on' : ''} onClick={() => setSort('velocity')}>
            Fastest moving
          </button>
          <button className={sort === 'volume' ? 'on' : ''} onClick={() => setSort('volume')}>
            Most open roles
          </button>
        </div>
      </section>

      <section className="band" style={{ paddingTop: 22, paddingBottom: 60 }}>
        <div className="grid">
          {ordered.map((company) => (
            <CompanyCard key={company.id} company={company} />
          ))}
        </div>

        <p className="notice">
          SAMPLE DATA — company reads are placeholders until /api/trends is wired up.
        </p>
      </section>
    </main>
  )
}

function CompanyCard({ company }: { company: CompanyTrend }) {
  const direction = company.velocity > 10 ? 'up' : company.velocity < 0 ? 'down' : 'flat'

  return (
    <article className="co">
      <div className="co-head">
        <div>
          <h3>{company.company}</h3>
          <p className="co-sector">{company.sector}</p>
        </div>
        <div className={`velocity ${direction}`}>
          {company.velocity > 0 ? '+' : ''}
          {company.velocity}%<span>30D</span>
        </div>
      </div>

      <p className="co-read">{company.read}</p>

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
      </div>
    </article>
  )
}
