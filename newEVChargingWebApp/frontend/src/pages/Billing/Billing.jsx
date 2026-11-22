import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Api } from "../../services/api";
import { Session } from "../../services/session";
import PageHeader from "../../components/common/PageHeader";
import Message from "../../components/common/Message";
import "./Billing.css";

export default function Billing() {
  const user = Session.get();

  const [payments, setPayments] = useState([]);
  const [energy, setEnergy] = useState(0);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadBilling = async () => {
      try {
        const paymentData = await Api.payments(user.userId);

        const sortedPayments = [...paymentData].sort(
          (a, b) =>
            new Date(b.paymentTime) - new Date(a.paymentTime)
        );

        setPayments(sortedPayments);
      } catch (e) {
        setError(e.message);
      }

      try {
        const sessionData = await Api.sessions(user.userId);

        const totalEnergy = sessionData.reduce(
          (total, session) =>
            total + Number(session.unitsConsumed || 0),
          0
        );

        setEnergy(totalEnergy);
      } catch (e) {
        // Energy loading failure should not stop payments.
      }
    };

    loadBilling();
  }, [user.userId]);

  const spent = payments
    .filter((payment) => payment.paymentStatus === "SUCCESS")
    .reduce((total, payment) => total + Number(payment.amount || 0), 0);

  return (
    <div className="container">
      <PageHeader
        icon="🧾"
        title="Billing & Payments"
        subtitle="View your charging bills and payment history."
      />

      <Message text={error} />

      <div className="billing-stats grid grid-3">
        <div className="card">
          <div className="card-title">Total Energy</div>
          <div className="status-value">
            {energy.toFixed(2)} kWh
          </div>
        </div>

        <div className="card">
          <div className="card-title">Total Spent</div>
          <div className="status-value">
            ₹{spent.toFixed(2)}
          </div>
        </div>

        <div className="card">
          <div className="card-title">Total Payments</div>
          <div className="status-value">
            {payments.length}
          </div>
        </div>
      </div>

      <h2 className="sub-heading">Payment History</h2>

      {!payments.length ? (
        <div className="empty-state">
          No payments yet.
        </div>
      ) : (
        <div className="bill-list">
          {payments.map((p) => (
            <div
              className="bill-card"
              key={p.paymentId}
            >
              <div>
                <h3>Payment #{p.paymentId}</h3>

                <p>
                  Session #{p.sessionId} •{" "}
                  {p.paymentMethod}
                </p>

                <small>
                  {new Date(
                    p.paymentTime
                  ).toLocaleString()}
                </small>
              </div>

              <div className="bill-amount">
                <b>
                  ₹{Number(p.amount || 0).toFixed(2)}
                </b>

                <span className="badge">
                  {p.paymentStatus}
                </span>

                <Link
                  className="btn btn-outline invoice-link"
                  to={`/invoice/${p.sessionId}`}
                >
                  🧾 Invoice
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
