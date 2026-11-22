import React, { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Api } from "../../services/api";
import Message from "../../components/common/Message";
import "../Login/Login.css";

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get("token") || "";
  const nav = useNavigate();
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    if (password.length < 8) return setError("Password must contain at least 8 characters.");
    if (password !== confirm) return setError("Passwords do not match.");
    setLoading(true);
    try {
      await Api.resetPassword(token, password);
      setDone(true);
      setTimeout(() => nav("/login"), 2500);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-showcase">
        <div className="showcase-icon">🔐</div>
        <h1>
          SMART <span>EV</span>
        </h1>
        <strong>Choose a new password</strong>
        <p>Pick a strong password you do not use anywhere else.</p>
      </div>
      <div className="auth-form-wrap">
        <form className="auth-card" onSubmit={submit}>
          <h2>New Password</h2>
          <p className="auth-sub">Set a new password for your account</p>
          {!token && <Message text="This reset link is incomplete. Please request a new one." />}
          <Message text={error} />
          {done ? (
            <Message text="Password updated. Redirecting to login..." type="success" />
          ) : (
            <>
              <div className="input-group">
                <label className="label">New password</label>
                <input className="input" type="password" value={password} minLength={8} maxLength={72}
                  onChange={(e) => setPassword(e.target.value)} required />
              </div>
              <div className="input-group">
                <label className="label">Confirm password</label>
                <input className="input" type="password" value={confirm} minLength={8} maxLength={72}
                  onChange={(e) => setConfirm(e.target.value)} required />
              </div>
              <button className="btn btn-primary btn-block auth-submit" disabled={loading || !token}>
                {loading ? "Saving..." : "Update password"}
              </button>
            </>
          )}
          <p className="auth-switch">
            <Link to="/forgot-password">Request a new link</Link> · <Link to="/login">Login</Link>
          </p>
        </form>
      </div>
    </div>
  );
}
