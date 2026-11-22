import React, { useEffect, useMemo, useState } from "react";
import { Api } from "../../services/api";
import PageHeader from "../../components/common/PageHeader";
import Message from "../../components/common/Message";

export default function FinanceAdmin() {
  const [payments, setPayments] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Api.adminPayments()
      .then((data) => setPayments(data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const summary = useMemo(() => {
    const successful = payments.filter((p) => p.paymentStatus === "SUCCESS");
    const pending = payments.filter((p) => p.paymentStatus === "PENDING");
    const failed = payments.filter((p) => p.paymentStatus === "FAILED");

    const revenue = successful.reduce(
      (total, p) => total + Number(p.amount || 0),
      0,
    );

    return {
      total: payments.length,
      successful: successful.length,
      pending: pending.length,
      failed: failed.length,
      revenue,
    };
  }, [payments]);

  return (
    <div className="container">
      <PageHeader
        icon="💰"
        title="Finance Admin Dashboard"
        subtitle="Monitor payments, revenue and transaction records."
      />

      <Message text={error} />

      {loading ? (
        <div className="empty-state">Loading payments...</div>
      ) : (
        <>
          <div className="grid grid-3 admin-stats">
            <div className="card">
              <div className="card-title">Total Payments</div>
              <div className="status-value">{summary.total}</div>
            </div>

            <div className="card">
              <div className="card-title">Successful Payments</div>
              <div className="status-value">{summary.successful}</div>
            </div>

            <div className="card">
              <div className="card-title">Pending Payments</div>
              <div className="status-value">{summary.pending}</div>
            </div>

            <div className="card">
              <div className="card-title">Failed Payments</div>
              <div className="status-value">{summary.failed}</div>
            </div>

            <div className="card">
              <div className="card-title">Total Revenue</div>
              <div className="status-value">₹{summary.revenue.toFixed(2)}</div>
            </div>
          </div>

          <h2 className="sub-heading">All Payment Records</h2>

          <div className="table-wrap">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Payment ID</th>
                  <th>Session ID</th>
                  <th>Amount</th>
                  <th>Method</th>
                  <th>Status</th>
                  <th>Payment Time</th>
                </tr>
              </thead>

              <tbody>
                {payments.map((p) => (
                  <tr key={p.paymentId}>
                    <td>{p.paymentId}</td>
                    <td>{p.sessionId}</td>
                    <td>₹{Number(p.amount || 0).toFixed(2)}</td>
                    <td>{p.paymentMethod || "-"}</td>
                    <td>{p.paymentStatus}</td>
                    <td>
                      {p.paymentTime
                        ? new Date(p.paymentTime).toLocaleString()
                        : "-"}
                    </td>
                  </tr>
                ))}

                {!payments.length && (
                  <tr>
                    <td colSpan="6" className="muted">
                      No payment records found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
