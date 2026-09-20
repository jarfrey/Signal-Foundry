export type Trend = 'Growing' | 'Steady' | 'Cooling'

export type Job = {
  id: string
  title: string
  company: string
  location: string
  department: string
  posted: string
  source: 'Greenhouse' | 'Lever'
  trend: Trend
  description: string
}

/** A job with a resume-match score attached. */
export type ScoredJob = Job & { score: number }

export type CompanyTrend = {
  id: string
  company: string
  sector: string
  /** Percent change in open roles vs the prior 30 days. */
  velocity: number
  openRoles: number
  newThisMonth: number
  topLocation: string
  /** Share of open roles per function, 0-1, highest first. */
  mix: { label: string; share: number }[]
  /** One-paragraph read generated from the posting data. */
  read: string
}
