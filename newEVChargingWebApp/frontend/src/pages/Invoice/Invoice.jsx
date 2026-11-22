import React, { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Api } from "../../services/api";
import { Session } from "../../services/session";
import Message from "../../components/common/Message";
import "./Invoice.css";

const money = (v) => `₹${Number(v || 0).toFixed(2)}`;
const when = (v) => (v ? new Date(v).toLocaleString() : "-");

/** Printable invoice for one charging session (use the browser's "Save as PDF" in the print dialog). */
export default function Invoice() {
  const { sessionId } = useParams();
  const user = Session.get();
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    (async () => {
      try {
        const session = await Api.sessionById(sessionId);
        const [payment, booking] = await Promise.all([
          Api.paymentBySession(sessionId).catch(() => null),
          Api.bookingById(session.bookingId).catch(() => null),
        ]);
        const station = booking ? await Api.stationById(booking.stationId).catch(() => null) : null;
        setData({ session, payment, booking, station });
      } catch (e) {
        setError(e.message);
      }
    })();
  }, [sessionId]);

  if (error)
    return (
      <div className="container">
        <Message text={error} />
        <Link className="btn btn-outline" to="/billing">← Back to billing</Link>
      </div>
    );
  if (!data) return <div className="container"><div className="empty-state">Loading invoice...</div></div>;

  const { session, payment, station } = data;
  const paid = payment?.paymentStatus === "SUCCESS";

  return (
    <div className="container invoice-wrap">
      <div className="no-print invoice-actions">
        <Link className="btn btn-outline" to="/billing">← Back</Link>
        <button className="btn btn-primary" onClick={() => window.print()}>🖨️ Print / Save as PDF</button>
      </div>

      <div className="invoice">
        <div className="invoice-head">
          <div>
            <h1>⚡ SMART <span>EV</span></h1>
            <p>Charging Invoice</p>
          </div>
          <div className="invoice-meta">
            <b>{payment ? `INV-${String(payment.paymentId).padStart(6, "0")}` : `SES-${String(session.sessionId).padStart(6, "0")}`}</b>
            <span>Date: {when(payment?.paymentTime || session.endTime || session.startTime)}</span>
            <span className={`pay-status ${paid ? "paid" : "unpaid"}`}>{paid ? "PAID" : payment ? payment.paymentStatus : "UNPAID"}</span>
          </div>
        </div>

        <div className="invoice-grid">
          <div>
            <h4>Billed to</h4>
            <p><b>{user.fullName}</b><br />{user.email}<br />{user.mobileNo}</p>
          </div>
          <div>
            <h4>Station</h4>
            <p><b>{station?.stationName || "-"}</b><br />{station?.sLocation || ""}</p>
          </div>
        </div>

        <table className="invoice-table">
          <tbody>
            <tr><td>Session</td><td>#{session.sessionId}</td></tr>
            <tr><td>Started</td><td>{when(session.startTime)}</td></tr>
            <tr><td>Ended</td><td>{when(session.endTime)}</td></tr>
            <tr><td>Battery</td><td>{Number(session.startBatteryPercentage).toFixed(0)}% → {Number(session.endBatteryPercentage).toFixed(0)}%</td></tr>
            <tr><td>Energy consumed</td><td>{Number(session.unitsConsumed || 0).toFixed(2)} kWh</td></tr>
            {station && <tr><td>Rate</td><td>₹{station.pricePerKwt} / kWh</td></tr>}
            {payment && <tr><td>Payment method</td><td>{payment.paymentMethod}</td></tr>}
            {payment?.razorpayPaymentId && <tr><td>Transaction ID</td><td>{payment.razorpayPaymentId}</td></tr>}
          </tbody>
          <tfoot>
            <tr><td>Total</td><td>{money(payment?.amount ?? session.totalCost)}</td></tr>
          </tfoot>
        </table>

        <p className="invoice-foot">
          Thank you for charging with Smart EV. This is a computer generated invoice and does not require a signature.
        </p>
      </div>
    </div>
  );
}
