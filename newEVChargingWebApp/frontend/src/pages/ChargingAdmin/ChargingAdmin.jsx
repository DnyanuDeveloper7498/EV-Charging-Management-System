import React, { useEffect, useMemo, useState } from "react";
import { Api } from "../../services/api";
import PageHeader from "../../components/common/PageHeader";
import Message from "../../components/common/Message";

const fmt = (d) => (d ? new Date(d).toLocaleString() : "-");

export default function ChargingAdmin() {
  const [sessions, setSessions] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([Api.adminSessions(), Api.adminBookings()])
      .then(([s, b]) => {
        setSessions(s || []);
        setBookings(b || []);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const summary = useMemo(
    () => ({
      sessions: sessions.length,
      active: sessions.filter((s) => !s.endTime).length,
      bookings: bookings.length,
      pending: bookings.filter((b) =>
        String(b.bookingStatus).startsWith("PENDING"),
      ).length,
    }),
    [sessions, bookings],
  );

  return (
    <div className="container">
      <PageHeader
        icon="🔌"
        title="Charging Admin Dashboard"
        subtitle="Monitor charging sessions and bookings."
      />

      <Message text={error} />

      {loading ? (
        <div className="empty-state">Loading charging data...</div>
      ) : (
        <>
          <div className="grid grid-3 admin-stats">
            <div className="card">
              <div className="card-title">Total Sessions</div>
              <div className="status-value">{summary.sessions}</div>
            </div>
            <div className="card">
              <div className="card-title">Active Sessions</div>
              <div className="status-value">{summary.active}</div>
            </div>
            <div className="card">
              <div className="card-title">Total Bookings</div>
              <div className="status-value">{summary.bookings}</div>
            </div>
            <div className="card">
              <div className="card-title">Pending Bookings</div>
              <div className="status-value">{summary.pending}</div>
            </div>
          </div>

          <h2 className="sub-heading">Charging Sessions</h2>
          <div className="table-wrap">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Session ID</th>
                  <th>Booking ID</th>
                  <th>Start</th>
                  <th>End</th>
                  <th>Units</th>
                  <th>Cost</th>
                  <th>Battery</th>
                </tr>
              </thead>
              <tbody>
                {sessions.map((s) => (
                  <tr key={s.sessionId}>
                    <td>{s.sessionId}</td>
                    <td>{s.bookingId}</td>
                    <td>{fmt(s.startTime)}</td>
                    <td>{s.endTime ? fmt(s.endTime) : "Charging..."}</td>
                    <td>{s.unitsConsumed}</td>
                    <td>₹{Number(s.totalCost || 0).toFixed(2)}</td>
                    <td>
                      {s.startBatteryPercentage}% → {s.endBatteryPercentage}%
                    </td>
                  </tr>
                ))}
                {!sessions.length && (
                  <tr>
                    <td colSpan="7" className="muted">
                      No sessions found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <h2 className="sub-heading">Bookings</h2>
          <div className="table-wrap">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Booking ID</th>
                  <th>Vehicle ID</th>
                  <th>Station ID</th>
                  <th>Booking Time</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {bookings.map((b) => (
                  <tr key={b.bookingId}>
                    <td>{b.bookingId}</td>
                    <td>{b.vehicleId}</td>
                    <td>{b.stationId}</td>
                    <td>{fmt(b.bookingTime)}</td>
                    <td>{b.bookingStatus}</td>
                  </tr>
                ))}
                {!bookings.length && (
                  <tr>
                    <td colSpan="5" className="muted">
                      No bookings found.
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
