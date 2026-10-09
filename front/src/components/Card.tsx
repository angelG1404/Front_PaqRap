import type { ReactNode } from 'react'
import { Icon } from './Icon'
export function Card({ title, subtitle, icon, tone = 'blue', badge, children, className = '' }: { title: string; subtitle: string; icon: string; tone?: string; badge?: ReactNode; children: ReactNode; className?: string }) {
  return <section className={`card ${className}`}><div className="card-heading"><span className={`card-icon ${tone}`}><Icon name={icon} /></span><div><h2>{title}</h2><p>{subtitle}</p></div>{badge && <span className={`badge ${tone}`}>{badge}</span>}</div>{children}</section>
}
