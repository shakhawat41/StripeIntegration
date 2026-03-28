import { useNavigate } from 'react-router-dom'

/**
 * Page 3: Payment success confirmation.
 * Shown after Stripe redirects back from the hosted checkout page.
 */
export default function SuccessPage() {
  const navigate = useNavigate()

  return (
    <div className="text-center py-16">
      <div className="text-6xl mb-4">✓</div>
      <h2 className="text-3xl font-bold text-green-600 mb-2">Payment Successful</h2>
      <p className="text-gray-500 mb-8">
        Thank you for your payment. A receipt has been sent to your email.
      </p>
      <button
        onClick={() => navigate('/')}
        className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700"
      >
        Create New Invoice
      </button>
    </div>
  )
}
