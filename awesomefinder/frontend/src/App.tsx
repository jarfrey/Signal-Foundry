import { useEffect, useState } from 'react'
import Home from './pages/Home'
import Applicant from './pages/Applicant'
import Investor from './pages/Investor'

type Route = 'home' | 'applicant' | 'investor'

function readHash(): Route {
  const hash = window.location.hash.replace('#/', '')
  return hash === 'applicant' || hash === 'investor' ? hash : 'home'
}

export default function App() {
  const [route, setRoute] = useState<Route>(readHash)

  useEffect(() => {
    const onChange = () => setRoute(readHash())
    window.addEventListener('hashchange', onChange)
    return () => window.removeEventListener('hashchange', onChange)
  }, [])

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [route])

  if (route === 'applicant') return <Applicant />
  if (route === 'investor') return <Investor />
  return <Home />
}
