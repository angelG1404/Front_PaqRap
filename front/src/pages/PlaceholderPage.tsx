import { Link } from 'react-router-dom'
import { Icon } from '../components/Icon'
export function PlaceholderPage({ title }: { title: string }) {
  return <section className="placeholder card"><Icon name={title === 'Mapa en vivo' ? 'map' : 'box'} /><h2>{title}</h2><p>Esta vista estará disponible en una próxima etapa.</p><Link className="secondary-button" to="/simulacion/parametros">Volver a parámetros</Link></section>
}
