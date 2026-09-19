export type Job = {
  id: string
  title: string
  company: string
  location: string
  department: string
  posted: string
  source: 'Greenhouse' | 'Lever'
  trend: 'Growing' | 'Steady' | 'Cooling'
  description: string
}
