export type Trend = 'Growing' | 'Steady' | 'Cooling'

export type Job = {
  id: string
  title: string
  company: string
  location: string
  department: string
  posted: string
  /** ISO-8601 publish date from the board, when it gives one. */
  postedAt?: string
  source: 'Greenhouse' | 'Lever'
  trend: Trend
  description: string
  /** Link to the real posting on the company's board. */
  url?: string
  seniority?: string
}

/** A job with a resume-match score attached. */
export type ScoredJob = Job & { score: number }

/** Where a company's written read came from. */
export type InsightSource = 'nemotron' | 'fallback'

export type CompanyTrend = {
  id: string
  company: string
  sector: string
  /** Percent change in roles posted vs the prior 30 days. */
  velocity: number
  openRoles: number
  newThisMonth: number
  /** Roles taken down in the last 30 days. Needs 2+ scrapes to be non-zero. */
  closedThisMonth: number
  topLocation: string
  trend: Trend
  /** Share of open roles per function, 0-1, highest first. */
  mix: { label: string; share: number }[]
  /** One-paragraph read generated from the posting data. */
  read: string
  headline: string
  bullets: string[]
  insightSource: InsightSource
}
