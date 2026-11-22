import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Api } from "../../services/api";
import { Session } from "../../services/session";
import PageHeader from "../../components/common/PageHeader";
import Message from "../../components/common/Message";
import "./Dashboard.css";
export default function Dashboard() {
  const user = Session.get();
  const [stats, setStats] = useState({
    wallet: user.walletBalance || 0,
    vehicles: 0,
    sessions: 0,
  });
  const [error, setError] = useState("");
  useEffect(() => {
    Promise.all([
      Api.vehicles(user.userId),
      Api.sessions(user.userId),
      Api.wallet(user.userId),
    ])
      .then(([v, s, w]) => {
        Session.save({
          ...user,
          walletBalance: w?.walletBalance ?? user.walletBalance ?? 0,
        });

        setStats({
          wallet: w.walletBalance || 0,
          vehicles: v.length,
          sessions: s.length,
        });
      })
      .catch((e) => setError(e.message));
  }, [user.userId]);
  const actions = [
    [
      "📍",
      "Charging Stations",
      "Find and book a nearby charging slot.",
      "/stations",
    ],
    [
      "🚗",
      "My Vehicles",
      "Add a vehicle or update battery status.",
      "/vehicles",
    ],
    ["📅", "My Bookings", "View and manage your reservations.", "/bookings"],
    ["🔌", "Charging Session", "Start or stop an active session.", "/session"],
    ["💳", "Wallet", "Add money and view payment history.", "/wallet"],
    ["🧾", "Billing", "Review energy usage and payments.", "/billing"],
    ["📊", "Reports", "Your usage and spending summary.", "/report"],
    ["👤", "My Profile", "Update your details and change password.", "/profile"],
    ...(["ADMIN", "STATION_OWNER"].includes(String(user.role).toUpperCase())
      ? [["🛠️", "Admin Panel", "Manage stations, users and revenue.", "/admin"]]
      : []),
  ];
  return (
    <div className="container">
      <PageHeader
        back={false}
        title={`Welcome, ${user.fullName || "User"}`}
        subtitle="Manage your vehicles, bookings and charging sessions."
      />
      <Message text={error} />
      <div className="dashboard-stats grid grid-3">
        <div className="card">
          <div className="card-title">💰 Wallet Balance</div>
          <div className="status-value">₹{Number(stats.wallet).toFixed(2)}</div>
          <p className="card-desc">Available balance for charging</p>
        </div>
        <div className="card">
          <div className="card-title">🚗 My Vehicles</div>
          <div className="status-value">{stats.vehicles}</div>
          <p className="card-desc">Registered vehicles</p>
        </div>
        <div className="card">
          <div className="card-title">⚡ Charging Sessions</div>
          <div className="status-value">{stats.sessions}</div>
          <p className="card-desc">Total sessions so far</p>
        </div>
      </div>
      <div className="section-title">Quick Actions</div>
      <div className="action-grid">
        {actions.map(([icon, title, desc, to]) => (
          <Link className="action-card" to={to} key={to}>
            <span className="action-icon">{icon}</span>
            <h3>{title}</h3>
            <p>{desc}</p>
            <span className="action-open">Open →</span>
          </Link>
        ))}
      </div>
    </div>
  );
}
