import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import { GRID_ALTO, GRID_ANCHO } from '../config/defaults'
import { Icon } from './Icon'
const navigation = [
  { to: '/pedidos', name: 'Pedidos', icon: 'truck' },
  { to: '/dashboard', name: 'Dashboard', icon: 'grid' },
  { to: '/operacion-diaria', name: 'Operación Diaria', icon: 'cycle' },
  { to: '/simulacion', name: 'Simulación', icon: 'route' },
]
export function Layout() {
  const { pathname } = useLocation()
  const simulation = pathname.startsWith('/simulacion')
  const mapa = pathname.endsWith('/mapa')
  const mapPage = mapa || pathname === '/operacion-diaria'
  const title = simulation ? mapa ? 'Mapa en vivo' : 'Parámetros de la Corrida' : navigation.find((item) => item.to === pathname)?.name
  return <div className="app-shell">
    <aside className="sidebar">
      <Link className="brand" to="/simulacion/parametros"><span><Icon name="box" /> PAQRAP</span><small>LOGISTICS ENGINE</small></Link>
      <nav aria-label="Navegación principal">{navigation.map((item) => <NavLink key={item.to} to={item.to === '/simulacion' ? '/simulacion/parametros' : item.to} className={({ isActive }) => `nav-item ${isActive || (item.to === '/simulacion' && simulation) ? 'active' : ''}`}><Icon name={item.icon} />{item.name}</NavLink>)}</nav>
      <div className="operator"><span className="avatar"><Icon name="user" /></span><div><strong>Operador Logístico</strong><small>ADMIN</small></div></div>
    </aside>
    <div className={`workspace ${mapPage ? 'map-workspace' : ''}`}>
      {!mapPage && <header className="topbar"><div><p className="breadcrumb">{simulation ? 'MÓDULO DE SIMULACIÓN' : 'PAQRAP'} <span>/</span> {mapa ? 'MAPA' : 'CONFIGURACIÓN PREVIA'}</p><h1>{title}</h1></div>
        {simulation && <ol className="steps" aria-label="Pasos de la corrida"><li className={!mapa ? 'current' : ''} aria-current={!mapa ? 'step' : undefined}><span>1</span>1. Parámetros</li><li className="step-line" aria-hidden="true" /><li className={mapa ? 'current' : ''} aria-current={mapa ? 'step' : undefined}><span>2</span>2. Mapa <Icon name="map" /></li></ol>}
      </header>}
      <main><Outlet /></main>
      {!mapPage && <footer>© {new Date().getFullYear()} PaqRap Logistics Solutions. Todos los derechos reservados.<span>Red {GRID_ANCHO} × {GRID_ALTO} km</span></footer>}
    </div>
  </div>
}
