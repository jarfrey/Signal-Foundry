import type { CompanyTrend, Job, ScoredJob } from './types'

/**
 * Shown only when the Java backend is unreachable, so the UI still renders
 * during a demo. `live: false` makes the pages say so rather than passing
 * invented numbers off as real ones.
 */
const mockJobs: Job[] = [
  { id: '1', title: 'Senior ML Engineer', company: 'Cursor', location: 'San Francisco, CA', department: 'Engineering', posted: '2d ago', source: 'Greenhouse', trend: 'Growing', description: 'Build the intelligence layer for a new generation of developer tools.' },
  { id: '2', title: 'Product Researcher', company: 'Perplexity', location: 'New York, NY', department: 'Product', posted: '3d ago', source: 'Lever', trend: 'Growing', description: 'Turn search behavior and user needs into product direction.' },
  { id: '3', title: 'Growth Strategy Lead', company: 'Ramp', location: 'New York, NY', department: 'Operations', posted: '4d ago', source: 'Greenhouse', trend: 'Steady', description: 'Shape the next phase of growth for a category-defining fintech.' },
  { id: '4', title: 'Founding Account Executive', company: 'Harvey', location: 'Remote - US', department: 'Sales', posted: '5d ago', source: 'Lever', trend: 'Growing', description: 'Help bring AI-native legal workflows to the world’s best teams.' },
  { id: '5', title: 'Staff Infrastructure Engineer', company: 'Vercel', location: 'Remote - US', department: 'Engineering', posted: '6d ago', source: 'Greenhouse', trend: 'Steady', description: 'Keep the global frontend cloud fast, reliable, and observable.' },
]

const mockTrends: CompanyTrend[] = [
  {
    id: 'cursor', company: 'Cursor', sector: 'Engineering-led', velocity: 64, openRoles: 48,
    newThisMonth: 18, closedThisMonth: 3, topLocation: 'San Francisco', trend: 'Growing',
    mix: [{ label: 'Engineering', share: 0.62 }, { label: 'Product', share: 0.18 }, { label: 'Sales', share: 0.12 }],
    read: 'Sample data. Start the backend to see real reads.',
    headline: 'Sample company', bullets: [], insightSource: 'fallback',
  },
]

/**
 * Fetch with a deadline. Insights are pre-generated during ingestion, so a
 * slow response means something is wrong rather than merely busy -- better to
 * show sample data with a notice than to leave the page spinning.
 */
async function fetchJson<T>(url: string, timeoutMs = 8000): Promise<T> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeoutMs)
  try {
    const response = await fetch(url, { signal: controller.signal })
    if (!response.ok) throw new Error(`API returned ${response.status}`)
    return (await response.json()) as T
  } finally {
    clearTimeout(timer)
  }
}

export type JobsResult = { jobs: Job[]; live: boolean }

export async function getJobs(): Promise<JobsResult> {
  try {
    const jobs = await fetchJson<Job[]>('/api/jobs')
    if (!Array.isArray(jobs) || jobs.length === 0) throw new Error('no jobs returned')
    return { jobs, live: true }
  } catch {
    return { jobs: mockJobs, live: false }
  }
}

export type TrendsResult = { trends: CompanyTrend[]; live: boolean }

export async function getCompanyTrends(): Promise<TrendsResult> {
  try {
    // Allow longer here: if the cache is cold this waits on the model.
    const trends = await fetchJson<CompanyTrend[]>('/api/trends', 45000)
    if (!Array.isArray(trends) || trends.length === 0) throw new Error('no trends returned')
    return { trends, live: true }
  } catch {
    return { trends: mockTrends, live: false }
  }
}

/** Words too common in job ads to say anything about a particular candidate. */
const STOPWORDS = new Set([
  'a', 'an', 'and', 'the', 'or', 'for', 'to', 'of', 'in', 'on', 'at', 'as', 'by', 'with', 'from',
  'we', 'you', 'our', 'your', 'is', 'are', 'be', 'will', 'that', 'this', 'it', 'its', 'have', 'has',
  'work', 'working', 'team', 'teams', 'role', 'roles', 'job', 'jobs', 'company', 'experience',
  'years', 'year', 'new', 'across', 'about', 'into', 'their', 'them', 'who', 'what', 'how',
])

function tokenize(text: string): string[] {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9+#.\s-]/g, ' ')
    .split(/[\s-]+/)
    .map((word) => word.replace(/^\.+|\.+$/g, ''))
    .filter((word) => word.length > 2 && !STOPWORDS.has(word))
}

/**
 * Score each posting against the resume and sort best-first.
 *
 * Deliberately simple and local: it is term overlap weighted so that a hit
 * in the title counts for more than one in the blurb, normalised against the
 * posting's own vocabulary so short ads are not unfairly favoured. Runs in
 * the browser, so nothing about the resume leaves the page.
 */
export function rankJobs(resume: string, jobs: Job[]): ScoredJob[] {
  const resumeTerms = new Set(tokenize(resume))
  if (resumeTerms.size === 0) {
    return jobs.map((job) => ({ ...job, score: 0 }))
  }

  const scored = jobs.map((job) => {
    const titleTerms = new Set(tokenize(`${job.title} ${job.department}`))
    const bodyTerms = new Set(tokenize(`${job.description} ${job.company} ${job.location}`))

    let hits = 0
    titleTerms.forEach((term) => {
      if (resumeTerms.has(term)) hits += 3
    })
    bodyTerms.forEach((term) => {
      if (resumeTerms.has(term) && !titleTerms.has(term)) hits += 1
    })

    const possible = titleTerms.size * 3 + bodyTerms.size
    const raw = possible === 0 ? 0 : hits / possible

    // Spread the useful range out; raw overlap rarely exceeds ~0.35.
    const score = Math.max(0, Math.min(99, Math.round(raw * 260)))
    return { ...job, score }
  })

  return scored.sort((a, b) => b.score - a.score)
}
