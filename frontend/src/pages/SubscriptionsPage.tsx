import { useState } from 'react'

const PLANS = [
  { id: 'weekly', name: 'Weekly', price: 100, interval: 'week', description: 'Cleaning service every week' },
  { id: 'biweekly', name: 'Bi-Weekly', price: 180, interval: '2 weeks', description: 'Cleaning service every two weeks' },
  { id: 'monthly', name: 'Monthly', price: 350, interval: 'month', description: 'Cleaning service once a month' },
]

type SubStatus = {
  hasSubscription: boolean
  subscriptionId?: string
  planName?: string
  amount?: number
  status?: string
  createdAt?: string
}

/**
 * Subscription management page.
 * Shows plan options if no active subscription, or current subscription with cancel option.
 */
export default function SubscriptionsPage() {
  const [email, setEmail] = useState('')
  const [subStatus, setSubStatus] = useState<SubStatus | null>(null)
  const [loading, setLoading] = useState(false)
  const [checked, setChecked] = useState(false)

  const checkStatus = async () => {
    if (!email) return
    setLoading(true)
    try {
      const res = await fetch(`/api/subscriptions/status?email=${encodeURIComponent(email)}`)
      const data = await res.json()
      setSubStatus(data)
      setChecked(true)
    } finally {
      setLoading(false)
    }
  }

  const subscribe = async (planId: string) => {
    if (!email) return
    setLoading(true)
    try {
      const res = await fetch('/api/subscriptions/checkout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ plan: planId, email }),
      })
      const data = await res.json()
      window.location.href = data.url
    } catch {
      alert('Failed to create checkout session')
    } finally {
      setLoading(false)
    }
  }

  const cancelSubscription = async () => {
    if (!confirm('Are you sure you want to cancel your subscription?')) return
    setLoading(true)
    try {
      await fetch('/api/subscriptions/cancel', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      })
      setSubStatus({ hasSubscription: false })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-900">Cleaning Service Subscriptions</h2>

      {/* Email input */}
      <div className="bg-white rounded-lg shadow p-6">
        <label className="block text-sm text-gray-600 mb-1">Your email address</label>
        <div className="flex gap-3">
          <input
            type="email"
            value={email}
            onChange={(e) => { setEmail(e.target.value); setChecked(false) }}
            placeholder="you@example.com"
            className="flex-1 border border-gray-300 rounded px-3 py-2"
          />
          <button
            onClick={checkStatus}
            disabled={!email || loading}
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? 'Checking...' : 'Check Subscription'}
          </button>
        </div>
      </div>

      {/* Active subscription */}
      {checked && subStatus?.hasSubscription && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-6">
          <h3 className="text-lg font-semibold text-green-800 mb-2">Active Subscription</h3>
          <p className="text-green-700">Plan: {subStatus.planName}</p>
          <p className="text-green-700">Amount: ${((subStatus.amount || 0) / 100).toFixed(2)} CAD</p>
          <p className="text-green-700">Status: {subStatus.status}</p>
          <p className="text-sm text-green-600 mt-1">Since: {subStatus.createdAt?.slice(0, 10)}</p>
          <button
            onClick={cancelSubscription}
            disabled={loading}
            className="mt-4 bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 disabled:opacity-50"
          >
            Cancel Subscription
          </button>
        </div>
      )}

      {/* Plan selection — only show if no active subscription */}
      {checked && !subStatus?.hasSubscription && (
        <div>
          <h3 className="text-lg font-semibold text-gray-700 mb-4">Choose a Plan</h3>
          <div className="grid grid-cols-3 gap-4">
            {PLANS.map((plan) => (
              <div key={plan.id} className="bg-white rounded-lg shadow p-6 text-center border-2 border-transparent hover:border-blue-500 transition">
                <h4 className="text-xl font-bold text-gray-900">{plan.name}</h4>
                <p className="text-3xl font-bold text-blue-600 my-3">${plan.price}</p>
                <p className="text-sm text-gray-500 mb-1">per {plan.interval}</p>
                <p className="text-sm text-gray-400 mb-4">{plan.description}</p>
                <button
                  onClick={() => subscribe(plan.id)}
                  disabled={loading}
                  className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 disabled:opacity-50"
                >
                  Subscribe
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
