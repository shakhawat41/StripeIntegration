import { useState } from 'react'
import { Routes, Route, NavLink } from 'react-router-dom'
import ItemsPage from './pages/ItemsPage'
import CheckoutPage from './pages/CheckoutPage'
import SuccessPage from './pages/SuccessPage'
import SubscriptionsPage from './pages/SubscriptionsPage'
import SubscriptionSuccessPage from './pages/SubscriptionSuccessPage'
import InstallmentsPage from './pages/InstallmentsPage'
import InstallmentSuccessPage from './pages/InstallmentSuccessPage'
import InstallmentPayPage from './pages/InstallmentPayPage'
import './index.css'

export type LineItem = { description: string; amount: number }

/**
 * Shakhawat's Business — Payment POC for AuctionSystem.
 * Two flows: One-time invoice payment and recurring subscription management.
 */
export default function App() {
  const [items, setItems] = useState<LineItem[]>([])

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header with navigation */}
      <header className="bg-white border-b border-gray-200 px-6 py-4">
        <div className="max-w-4xl mx-auto flex justify-between items-center">
          <div>
            <h1 className="text-xl font-bold text-gray-900">Shakhawat's Business</h1>
            <p className="text-sm text-gray-500">Payment for services rendered</p>
          </div>
          <nav className="flex gap-4">
            <NavLink to="/" className={({ isActive }) =>
              `text-sm px-3 py-1 rounded ${isActive ? 'bg-blue-100 text-blue-700' : 'text-gray-600 hover:text-gray-900'}`
            }>Invoice</NavLink>
            <NavLink to="/subscriptions" className={({ isActive }) =>
              `text-sm px-3 py-1 rounded ${isActive ? 'bg-blue-100 text-blue-700' : 'text-gray-600 hover:text-gray-900'}`
            }>Subscriptions</NavLink>
            <NavLink to="/installments" className={({ isActive }) =>
              `text-sm px-3 py-1 rounded ${isActive ? 'bg-blue-100 text-blue-700' : 'text-gray-600 hover:text-gray-900'}`
            }>Installments</NavLink>
          </nav>
        </div>
      </header>

      <main className="max-w-4xl mx-auto py-8 px-4">
        <Routes>
          <Route path="/" element={<ItemsPage items={items} setItems={setItems} />} />
          <Route path="/checkout" element={<CheckoutPage items={items} />} />
          <Route path="/success" element={<SuccessPage />} />
          <Route path="/subscriptions" element={<SubscriptionsPage />} />
          <Route path="/subscription-success" element={<SubscriptionSuccessPage />} />
          <Route path="/installments" element={<InstallmentsPage />} />
          <Route path="/installment-success" element={<InstallmentSuccessPage />} />
          <Route path="/pay/installment/:planId" element={<InstallmentPayPage />} />
        </Routes>
      </main>
    </div>
  )
}
