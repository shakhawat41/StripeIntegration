import { useState, useEffect } from 'react'

type Plan = {
  id: number; customerEmail: string; description: string;
  totalAmountCents: number; firstPaymentCents: number; remainingInstallmentCents: number;
  totalInstallments: number; frequency: string; status: string; installmentsPaid: number;
}

/**
 * Merchant view: create installment plans and see all existing plans.
 * Customer view: see your plans and pay first installment.
 */
export default function InstallmentsPage() {
  const [plans, setPlans] = useState<Plan[]>([])
  const [email, setEmail] = useState('')
  const [desc, setDesc] = useState('')
  const [total, setTotal] = useState('')
  const [first, setFirst] = useState('')
  const [installments, setInstallments] = useState('4')
  const [frequency, setFrequency] = useState('monthly')
  const [loading, setLoading] = useState(false)

  const loadPlans = () => {
    fetch('/api/installments').then(r => r.json()).then(setPlans)
  }

  useEffect(loadPlans, [])

  const remaining = total && first && installments
    ? ((parseFloat(total) * 100 - parseFloat(first) * 100) / (parseInt(installments) - 1)) / 100
    : 0

  const createPlan = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      await fetch('/api/installments/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          customerEmail: email,
          description: desc,
          totalAmount: Math.round(parseFloat(total) * 100),
          firstPayment: Math.round(parseFloat(first) * 100),
          totalInstallments: parseInt(installments),
          frequency,
        }),
      })
      setEmail(''); setDesc(''); setTotal(''); setFirst('')
      loadPlans()
    } finally { setLoading(false) }
  }

  return (
    <div className="space-y-8">
      <h2 className="text-2xl font-bold text-gray-900">Installment Plans</h2>

      {/* Merchant: Create plan form */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold text-gray-700 mb-4">Create New Plan (Merchant)</h3>
        <form onSubmit={createPlan} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm text-gray-600 mb-1">Customer Email</label>
              <input type="email" value={email} onChange={e => setEmail(e.target.value)} required
                placeholder="customer@example.com" className="w-full border border-gray-300 rounded px-3 py-2" />
            </div>
            <div>
              <label className="block text-sm text-gray-600 mb-1">Description</label>
              <input type="text" value={desc} onChange={e => setDesc(e.target.value)} required
                placeholder="Kitchen renovation" className="w-full border border-gray-300 rounded px-3 py-2" />
            </div>
            <div>
              <label className="block text-sm text-gray-600 mb-1">Total Amount ($)</label>
              <input type="number" step="0.01" value={total} onChange={e => setTotal(e.target.value)} required
                placeholder="1200.00" className="w-full border border-gray-300 rounded px-3 py-2" />
            </div>
            <div>
              <label className="block text-sm text-gray-600 mb-1">First Payment ($)</label>
              <input type="number" step="0.01" value={first} onChange={e => setFirst(e.target.value)} required
                placeholder="500.00" className="w-full border border-gray-300 rounded px-3 py-2" />
            </div>
            <div>
              <label className="block text-sm text-gray-600 mb-1">Total Installments</label>
              <select value={installments} onChange={e => setInstallments(e.target.value)}
                className="w-full border border-gray-300 rounded px-3 py-2">
                {[2,3,4,5,6,7,8,9,10,11,12].map(n => <option key={n} value={n}>{n}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm text-gray-600 mb-1">Frequency</label>
              <select value={frequency} onChange={e => setFrequency(e.target.value)}
                className="w-full border border-gray-300 rounded px-3 py-2">
                <option value="weekly">Weekly</option>
                <option value="biweekly">Bi-Weekly</option>
                <option value="monthly">Monthly</option>
              </select>
            </div>
          </div>
          {remaining > 0 && (
            <p className="text-sm text-gray-500">
              Breakdown: 1st payment ${parseFloat(first).toFixed(2)} + {parseInt(installments) - 1} × ${remaining.toFixed(2)} = ${parseFloat(total).toFixed(2)}
            </p>
          )}
          <button type="submit" disabled={loading}
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50">
            {loading ? 'Creating...' : 'Create Plan'}
          </button>
        </form>
      </div>

      {/* All plans */}
      {plans.length > 0 && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <h3 className="text-lg font-semibold text-gray-700 p-4 border-b">All Plans</h3>
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="text-left px-4 py-2 text-sm text-gray-500">Customer</th>
                <th className="text-left px-4 py-2 text-sm text-gray-500">Description</th>
                <th className="text-right px-4 py-2 text-sm text-gray-500">Total</th>
                <th className="text-right px-4 py-2 text-sm text-gray-500">1st Payment</th>
                <th className="text-center px-4 py-2 text-sm text-gray-500">Progress</th>
                <th className="text-center px-4 py-2 text-sm text-gray-500">Status</th>
                <th className="px-4 py-2"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {plans.map(p => (
                <tr key={p.id}>
                  <td className="px-4 py-3 text-sm">{p.customerEmail}</td>
                  <td className="px-4 py-3 text-sm">{p.description}</td>
                  <td className="px-4 py-3 text-sm text-right">${(p.totalAmountCents / 100).toFixed(2)}</td>
                  <td className="px-4 py-3 text-sm text-right">${(p.firstPaymentCents / 100).toFixed(2)}</td>
                  <td className="px-4 py-3 text-sm text-center">{p.installmentsPaid}/{p.totalInstallments}</td>
                  <td className="px-4 py-3 text-center">
                    <span className={`px-2 py-1 rounded text-xs ${
                      p.status === 'active' ? 'bg-green-100 text-green-700' :
                      p.status === 'pending' ? 'bg-yellow-100 text-yellow-700' :
                      p.status === 'completed' ? 'bg-blue-100 text-blue-700' :
                      'bg-gray-100 text-gray-700'
                    }`}>{p.status}</span>
                  </td>
                  <td className="px-4 py-3">
                    {p.status === 'pending' && (
                      <button onClick={() => {
                        const link = `${window.location.origin}/pay/installment/${p.id}`
                        navigator.clipboard.writeText(link)
                        alert('Payment link copied!\n\n' + link)
                      }}
                        className="text-sm bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700 whitespace-nowrap">
                        📋 Copy Payment Link
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
