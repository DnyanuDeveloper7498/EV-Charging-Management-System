import React, { useEffect, useState } from "react";
import { Api } from "../../services/api";
import { Session } from "../../services/session";
import { loadRazorpay } from "../../services/razorpay";
import PageHeader from "../../components/common/PageHeader";
import Message from "../../components/common/Message";
import "./Wallet.css";

export default function Wallet() {
  const user = Session.get();
  const [balance, setBalance] = useState(Number(user?.walletBalance || 0));
  const [payments, setPayments] = useState([]);
  const [amount, setAmount] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const refresh = () => {
    Api.wallet()
      .then((w) => {
        Session.save(w);
        setBalance(Number(w.walletBalance || 0));
      })
      .catch((e) => setError(e.message));

    Api.payments()
      .then((p) =>
        setPayments(
          [...p].sort(
            (a, b) => new Date(b.paymentTime) - new Date(a.paymentTime),
          ),
        ),
      )
      .catch((e) => setError(e.message));
  };

  useEffect(refresh, []);

  // Money is added only after Razorpay confirms the payment (the server verifies it).
  // In local development the server may allow a direct credit (WALLET_DEMO_TOPUP=true).
  const add = async () => {
    const n = Number(amount);

    if (!n || n <= 0) {
      setError("Enter a valid amount.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const wallet = await Api.wallet();

      if (wallet.demoTopupEnabled) {
        const w = await Api.walletAdd(n);
        Session.save(w);
        setBalance(Number(w.walletBalance || 0));
        setAmount("");
        refresh();
        setLoading(false);
        return;
      }

      const order = await Api.walletTopupOrder(n);
      const Razorpay = await loadRazorpay();

      const checkout = new Razorpay({
        key: order.keyId,
        amount: order.amount,
        currency: order.currency,
        name: "EV Charging System",
        description: "Wallet top-up",
        order_id: order.orderId,
        handler: async (response) => {
          try {
            const w = await Api.walletTopupVerify(response);
            Session.save(w);
            setBalance(Number(w.walletBalance || 0));
            setAmount("");
            refresh();
          } catch (e) {
            setError(e.message || "Top-up verification failed.");
          } finally {
            setLoading(false);
          }
        },
        modal: { ondismiss: () => setLoading(false) },
      });

      checkout.on("payment.failed", async (r) => {
        setError(r.error?.description || "Payment failed.");

        try {
          const orderId = r.error?.metadata?.order_id;
          const paymentId = r.error?.metadata?.payment_id;

          if (orderId && paymentId) {
            await Api.walletTopupFail({
              razorpay_order_id: orderId,
              razorpay_payment_id: paymentId,
            });
          }
        } catch (e) {
          console.error("Could not report top-up failure:", e);
        } finally {
          setLoading(false);
        }
      });

      checkout.open();
    } catch (e) {
      setError(e.message);
      setLoading(false);
    }
  };

  return (
    <div className="container">
      <PageHeader
        icon="💳"
        title="Wallet"
        subtitle="Manage your balance and view payment history."
      />
      <Message text={error} />

      <div className="wallet-top">
        <div className="balance-card">
          <span>Current Balance</span>
          <strong>₹{balance.toFixed(2)}</strong>
          <p>Can be used to pay for a charging session after it ends.</p>
        </div>

        <div className="add-money-card">
          <span>Add Money</span>
          <p>Top up your wallet to keep charging.</p>
          <div className="add-row">
            <input
              className="input"
              type="number"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="₹ amount"
            />
            <button className="btn add-btn" disabled={loading} onClick={add}>
              {loading ? "Adding..." : "Add"}
            </button>
          </div>
        </div>
      </div>

      <h2 className="sub-heading">Payment History</h2>

      {!payments.length ? (
        <div className="empty-state">No payments yet.</div>
      ) : (
        <div className="payment-list">
          {payments.map((p) => (
            <div className="payment-card" key={p.paymentId}>
              <div>
                <h3>Payment #{p.paymentId}</h3>
                <p>
                  Session #{p.sessionId} • {p.paymentMethod} •{" "}
                  {new Date(p.paymentTime).toLocaleString()}
                </p>
              </div>
              <div className="payment-right">
                <b>₹{Number(p.amount).toFixed(2)}</b>
                <span className="badge">{p.paymentStatus}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
