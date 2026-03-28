import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { LineItem } from '../App'

/**
 * Page 1: Add items with description and amount.
 * Shows a running list and total. "Checkout" navigates to the review page.
 */
export default function ItemsPage({
  items,
  setItems,
}: {
  items: LineItem[]
  setItems: (items: LineItem[]) => void
}) {
  const [desc, setDesc] = useState('')
  const [amount, setAmount] = useState('')
  const navigate = useNavigate()

  const addItem = () => {
    if (!desc.trim() || !amount) return
    setItems([...items, { description: desc.trim(), amount: parseFloat(amount) }])
    setDesc('')
    setAmount('')
  }

  const removeItem = (index: number) => {
    setItems(items.filter((_, i) => i !== index))
  }

  const subtotal = items.reduce((sum, item) => sum + item.amount, 0)

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-900">Add Items</h2>

      {/* Add item form */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex gap-3 items-end">
          <div className="flex-1">
            <label className="block text-sm text-gray-600 mb-1">Description</label>
            <input
              type="text"
              value={desc}
              onChange={(e) => setDesc(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && addItem()}
              placeholder="e.g. Web development service"
              className="w-full border border-gray-300 rounded px-3 py-2"
            />
          </div>
          <div className="w-32">
            <label className="block text-sm text-gray-600 mb-1">Amount ($)</label>
            <input
              type="number"
              step="0.01"
              min="0"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && addItem()}
              placeholder="50.00"
              className="w-full border border-gray-300 rounded px-3 py-2"
            />
          </div>
          <button
            onClick={addItem}
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
          >
            Add
          </button>
        </div>
      </div>

      {/* Items list */}
      {items.length > 0 && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Description</th>
                <th className="text-right px-6 py-3 text-sm font-medium text-gray-500">Amount</th>
                <th className="w-16"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {items.map((item, i) => (
                <tr key={i}>
                  <td className="px-6 py-4">{item.description}</td>
                  <td className="px-6 py-4 text-right">${item.amount.toFixed(2)}</td>
                  <td className="px-2 py-4">
                    <button
                      onClick={() => removeItem(i)}
                      className="text-red-500 hover:text-red-700 text-sm"
                      aria-label={`Remove ${item.description}`}
                    >
                      ✕
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot className="bg-gray-50">
              <tr>
                <td className="px-6 py-3 font-semibold">Total</td>
                <td className="px-6 py-3 text-right font-semibold">${subtotal.toFixed(2)}</td>
                <td></td>
              </tr>
            </tfoot>
          </table>
        </div>
      )}

      {/* Checkout button */}
      {items.length > 0 && (
        <div className="flex justify-end">
          <button
            onClick={() => navigate('/checkout')}
            className="bg-green-600 text-white px-6 py-3 rounded-lg text-lg font-medium hover:bg-green-700"
          >
            Proceed to Checkout →
          </button>
        </div>
      )}
    </div>
  )
}
