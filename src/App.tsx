import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { SimulationProvider } from './context/SimulationProvider'
import { Layout } from './components/Layout'
import { ParametersPage } from './pages/ParametersPage'
import { PlaceholderPage } from './pages/PlaceholderPage'
import { SimulationMapPage } from './pages/SimulationMapPage'
import { DailyOperationPage } from './pages/DailyOperationPage'
export default function App() {
  return <BrowserRouter><SimulationProvider><Routes><Route element={<Layout />}>
    <Route index element={<Navigate to="/simulacion/parametros" replace />} />
    <Route path="simulacion/parametros" element={<ParametersPage />} />
    <Route path="simulacion/mapa" element={<SimulationMapPage />} />
    <Route path="pedidos" element={<PlaceholderPage title="Pedidos" />} />
    <Route path="dashboard" element={<PlaceholderPage title="Dashboard" />} />
    <Route path="operacion-diaria" element={<DailyOperationPage />} />
    <Route path="*" element={<Navigate to="/simulacion/parametros" replace />} />
  </Route></Routes></SimulationProvider></BrowserRouter>
}
