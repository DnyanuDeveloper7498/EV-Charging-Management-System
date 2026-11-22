import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Api } from "../../services/api";
import { Session } from "../../services/session";
import Message from "../../components/common/Message";
import "../Login/Login.css";

export default function AdminLogin() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    email: "",
    password: "",
  });

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const updateForm = (key, value) => {
    setForm((prev) => ({
      ...prev,
      [key]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const auth = await Api.login(form);

      const role = String(auth.user?.role || "").toUpperCase();

      const adminRoutes = {
        ADMIN: "/admin",
        FINANCE_ADMIN: "/finance-admin",
        VEHICLE_ADMIN: "/vehicle-admin",
        CHARGING_ADMIN: "/charging-admin",
        USER_ADMIN: "/user-admin",
        STATION_ADMIN: "/station-admin"
      };

      const targetRoute = adminRoutes[role];

      if (!targetRoute) {
        setError("Access denied. Authorized Admin account required.");
        return;
      }

      Session.saveAuth(auth);
      navigate(targetRoute);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-showcase">
        <div className="showcase-icon">⚡</div>
        <h1>
          SMART <span>EV</span>
        </h1>
        <strong>Admin Portal</strong>

        <p>
          Secure administration portal for managing EV charging stations, users
          and system operations.
        </p>

        <div className="showcase-points">
          <span>✓ Secure admin access</span>
          <span>✓ Station management</span>
          <span>✓ System monitoring</span>
        </div>
      </div>

      <div className="auth-form-wrap">
        <form className="auth-card" onSubmit={handleSubmit}>
          <h2>Admin Login</h2>
          <p className="auth-sub">Sign in with your authorized admin account</p>

          <Message text={error} />

          <div className="input-group">
            <label className="label">Admin Email</label>
            <input
              className="input"
              type="email"
              value={form.email}
              onChange={(e) => updateForm("email", e.target.value)}
              placeholder="admin@example.com"
              required
            />

            
          </div>

          <div className="input-group">
            <label className="label">Password</label>
            <input
              className="input"
              type="password"
              value={form.password}
              onChange={(e) => updateForm("password", e.target.value)}
              placeholder="Enter admin password"
              required
            />
          </div>

          <button
            className="btn btn-primary btn-block auth-submit"
            disabled={loading}
          >
            {loading ? "Verifying..." : "Secure Admin Login"}
          </button>

          <p className="auth-switch">
            <Link to="/">Back to Home</Link>
          </p>
        </form>
      </div>
    </div>
  );
}
