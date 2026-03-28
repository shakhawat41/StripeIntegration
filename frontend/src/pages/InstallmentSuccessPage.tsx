import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

/**
 * Shown after the first installment payment succeeds.
 * Confirms with the backend, which creates the subscription for remaining installments.
 */
export default function InstallmentSuccessPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [details, setDetails] = useState<any>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const sessionId = searchParams.get('session_id')
    const planId = searchParams.get('plan_id')
    if (!sessionId || !planId) return

    fetch('/api/installments/confirm-first', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId, planId }),
    })
      .then(r => r.json())
      .then(setDetails)
      .catch(() => setError('Failed to confirm payment'))
  }, [searchParams])

  return (
    <div className="text-center py-16">
      <div className="text-6xl mb-4">✓</div>
      <h2 className="text-3xl font-bold text-green-600 mb-2">First Installment Paid</h2>
      {error && <p className="text-red-600">{error}</p>}
      {details ? (
        <div className="mt-4 space-y-1">
          <p className="text-gray-700">Installments paid: {details.installmentsPaid} of {details.installmentsPaid + details.remainingInstallments}</p>
          <p className="text-gray-700">Remaining installments will be charged automatically ({details.remainingInstallments} left)</p>
          <p className="text-sm text-gray-500 mt-2">Subscription ID: {details.subscriptionId}</p>
        </div>
      ) : (
        <p className="text-gray-500">Setting up remaining installments...</p>
      )}
      <button onClick={() => navigate('/installments')}
        className="mt-8 bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700">
        View All Plans
      </button>
    </div>
  )
}
