import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

/**
 * Shown after successful subscription checkout.
 * Confirms the subscription with the backend and shows details.
 */
export default function SubscriptionSuccessPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [confirmed, setConfirmed] = useState(false)
  const [details, setDetails] = useState<any>(null)

  useEffect(() => {
    const sessionId = searchParams.get('session_id')
    if (!sessionId) return

    // Confirm the subscription with our backend
    fetch('/api/subscriptions/confirm', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId }),
    })
      .then((res) => res.json())
      .then((data) => {
        setDetails(data)
        setConfirmed(true)
      })
  }, [searchParams])

  return (
    <div className="text-center py-16">
      <div className="text-6xl mb-4">✓</div>
      <h2 className="text-3xl font-bold text-green-600 mb-2">Subscription Active</h2>
      {confirmed && details ? (
        <div className="mt-4 space-y-1">
          <p className="text-gray-700">Plan: {details.planName}</p>
          <p className="text-gray-700">Amount: ${(details.amount / 100).toFixed(2)} CAD</p>
          <p className="text-gray-500 text-sm">Subscription ID: {details.subscriptionId}</p>
        </div>
      ) : (
        <p className="text-gray-500">Confirming your subscription...</p>
      )}
      <button
        onClick={() => navigate('/subscriptions')}
        className="mt-8 bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700"
      >
        View My Subscription
      </button>
    </div>
  )
}
