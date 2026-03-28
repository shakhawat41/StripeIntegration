import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'

type Plan = {
  id: number; customerEmail: string; description: string;
  totalAmountCents: number; firstPaymentCents: number; remainingInstallmentCents: number;
  totalInstallments: number; frequency: string; status: string; installmentsPaid: number;
}

/**
 * Customer-facing page — simulates what the customer sees when they
 * open the payment link sent by the merchant.
 */
export default function InstallmentPayPage() {
  const { planId } = useParams<{ planId: string }>()
  const [plan, setPlan] = useState<Plan | null>(null)
  const [loading, setLoading] = useState(true)
  const [paying, setPaying] = useState(false)

  useEffect(() => {
    fetch(`/api/installments/${planId}`)
      .then(r => r.json())
      .then(setPlan)
      .finally(() => setLoading(false))
  }, [planId])

  const handlePay = async () => {
    setPaying(true)
    try {
      const res = await fetch(`/api/installments/${planId}/pay-first`, { method: 'POST' })
      const data = await res.json()
      window.location.href = data.url
    } catch {
      alert('Failed to start payment')
      setPaying(false)
    }
  }

  if (loading) return <p className="text-center py-16 text-gray-500">Loading payment details...</p>
  if (!plan) return <p className="text-center py-16 text-red-500">Plan not found</p>

  if (plan.status !== 'pending') {
    return (
      <div className="text-center py-16">
        <p className="text-gray-500">This payment link is no longer active.</p>
        <p className="text-sm text-gray-400 mt-2">Status: {plan.status}</p>
      </div>
    )
  }

  const remainingCount = plan.totalInstallments - 1
  const perInstallment = plan.remainingInstallmentCents / 100

  return (
    <div className="max-w-lg mx-auto py-12">
      {/* Simulated email/link context */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6 text-center">
        <p className="text-sm text-blue-700">You received a payment link from</p>
        <p className="text-lg font-bold text-blue-900">Shakhawat's Business</p>
      </div>

      {/* Payment details card */}
      <div className="bg-white rounded-lg shadow-lg p-8">
        <div className="text-center mb-6">
          <h2 className="text-2xl font-bold text-gray-900">Installment Payment Plan</h2>
          <p className="text-gray-500 mt-1">{plan.description}</p>
        </div>

        {/* Breakdown */}
        <div className="space-y-3 border-t border-gray-200 pt-4">
          <div className="flex justify-between">
            <span className="text-gray-600">Total amount</span>
            <span className="font-medium">${(plan.totalAmountCents / 100).toFixed(2)} CAD</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600">Number of installments</span>
            <span className="font-medium">{plan.totalInstallments}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-600">Frequency</span>
            <span className="font-medium capitalize">{plan.frequency}</span>
          </div>

          <div className="border-t border-gray-200 pt-3 mt-3">
            <p className="text-sm font-semibold text-gray-700 mb-2">Payment Schedule:</p>
            <div className="space-y-1 text-sm">
              <div className="flex justify-between bg-green-50 rounded px-3 py-2">
                <span>Installment 1 (today)</span>
                <span className="font-semibold text-green-700">${(plan.firstPaymentCents / 100).toFixed(2)}</span>
              </div>
              {Array.from({ length: remainingCount }, (_, i) => (
                <div key={i} className="flex justify-between px-3 py-2">
                  <span>Installment {i + 2}</span>
                  <span className="text-gray-600">${perInstallment.toFixed(2)}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="flex justify-between text-lg font-bold border-t border-gray-300 pt-3 mt-3">
            <span>Due now</span>
            <span className="text-green-700">${(plan.firstPaymentCents / 100).toFixed(2)} CAD</span>
          </div>
        </div>

        <p className="text-xs text-gray-400 text-center mt-4">
          Remaining installments will be charged automatically {plan.frequency}.
        </p>

        <button
          onClick={handlePay}
          disabled={paying}
          className="w-full mt-6 bg-green-600 text-white py-4 rounded-lg text-lg font-semibold hover:bg-green-700 disabled:opacity-50"
        >
          {paying ? 'Redirecting to payment...' : `Pay First Installment — $${(plan.firstPaymentCents / 100).toFixed(2)} CAD`}
        </button>

        <p className="text-xs text-gray-400 text-center mt-3">
          Secure payment powered by Stripe
        </p>
      </div>
    </div>
  )
}
