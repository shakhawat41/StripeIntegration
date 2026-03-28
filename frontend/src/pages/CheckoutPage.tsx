import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { LineItem } from '../App'

const GST_RATE = 0.13

/**
 * Page 2: Review items, see subtotal + 13% GST + total.
 * "Pay Now" calls the backend to create a Stripe Checkout Session
 * and redirects to Stripe's hosted payment page.
 */
export default function CheckoutPage({ items }: { items: LineItem[] }) {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const subtotal = items.reduce((sum, item) => sum + item.amount, 0)
  const gst = subtotal * GST_RATE
  const total = subtotal + gst

  if (items.length === 0) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500 mb-4">No items to checkout.</p>
        <button onClick={() => navigate('/')} className="text-blue-600 hover:underline">
          ← Go back and add items
        </button>
      </div>
    )
  }

  const handlePayNow = async () => {
    setLoading(true)
    try {
      const res = await fetch('/api/checkout/create-session', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          items: items.map((item) => ({
            description: item.description,
            amount: Math.round(item.amount * 100), // convert to cents
          })),
          subtotal: Math.round(subtotal * 100),
          gst: Math.round(gst * 100),
          total: Math.round(total * 100),
        }),
      })
      const data = await res.json()
      // Redirect to Stripe's hosted checkout page
      window.location.href = data.url
    } catch {
      alert('Failed to create checkout session. Is the backend running?')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-6">
      <button onClick={() => navigate('/')} className="text-blue-600 hover:underline text-sm">
        ← Back to items
      </button>

      <div className="bg-white rounded-lg shadow p-8">
        {/* Business header */}
        <div className="text-center mb-8 pb-6 border-b border-gray-200">
          <h2 className="text-2xl font-bold text-gray-900">Shakhawat's Business</h2>
          <p className="text-gray-500 mt-1">Invoice for services rendered</p>
          <p className="text-sm text-gray-400 mt-1">Currency: CAD</p>
        </div>

        {/* Line items */}
        <table className="w-full mb-6">
          <thead>
            <tr className="border-b border-gray-200">
              <th className="text-left py-2 text-sm font-medium text-gray-500">Service</th>
              <th className="text-right py-2 text-sm font-medium text-gray-500">Amount</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item, i) => (
              <tr key={i} className="border-b border-gray-100">
                <td className="py-3">{item.description}</td>
                <td className="py-3 text-right">${item.amount.toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Totals */}
        <div className="space-y-2 border-t border-gray-200 pt-4">
          <div className="flex justify-between text-gray-600">
            <span>Subtotal</span>
            <span>${subtotal.toFixed(2)}</span>
          </div>
          <div className="flex justify-between text-gray-600">
            <span>GST (13%)</span>
            <span>${gst.toFixed(2)}</span>
          </div>
          <div className="flex justify-between text-xl font-bold text-gray-900 pt-2 border-t border-gray-300">
            <span>Total</span>
            <span>${total.toFixed(2)}</span>
          </div>
        </div>

        {/* Payment methods info */}
        <div className="mt-6 text-center text-sm text-gray-400">
          Accepts: Credit Card, Debit Card, Bank Transfer
        </div>

        {/* Pay button */}
        <button
          onClick={handlePayNow}
          disabled={loading}
          className="w-full mt-6 bg-green-600 text-white py-4 rounded-lg text-lg font-semibold hover:bg-green-700 disabled:opacity-50"
        >
          {loading ? 'Redirecting to payment...' : `Pay Now — $${total.toFixed(2)} CAD`}
        </button>
      </div>
    </div>
  )
}
