import type { Job } from './types'

const mockJobs: Job[] = [
  { id: '1', title: 'Senior ML Engineer', company: 'Cursor', location: 'San Francisco, CA', department: 'Engineering', posted: '2d ago', source: 'Greenhouse', trend: 'Growing', description: 'Build the intelligence layer for a new generation of developer tools.' },
  { id: '2', title: 'Product Researcher', company: 'Perplexity', location: 'New York, NY', department: 'Product', posted: '3d ago', source: 'Lever', trend: 'Growing', description: 'Turn search behavior and user needs into product direction.' },
  { id: '3', title: 'Growth Strategy Lead', company: 'Ramp', location: 'New York, NY', department: 'Strategy', posted: '4d ago', source: 'Greenhouse', trend: 'Steady', description: 'Shape the next phase of growth for a category-defining fintech.' },
  { id: '4', title: 'Founding Account Executive', company: 'Harvey', location: 'Remote - US', department: 'Sales', posted: '5d ago', source: 'Lever', trend: 'Growing', description: 'Help bring AI-native legal workflows to the world’s best teams.' },
  { id: '5', title: 'Staff Infrastructure Engineer', company: 'Vercel', location: 'Remote - US', department: 'Engineering', posted: '6d ago', source: 'Greenhouse', trend: 'Steady', description: 'Keep the global frontend cloud fast, reliable, and observable.' },
]

export async function getJobs(): Promise<Job[]> {
  try {
    const response = await fetch('/api/jobs')
    if (!response.ok) throw new Error('API unavailable')
    return response.json() as Promise<Job[]>
  } catch {
    return mockJobs
  }
}
