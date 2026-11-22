import React, { useState } from "react";
import { Link } from "react-router-dom";
import { Api } from "../../services/api";
import Message from "../../components/common/Message";
import "../Login/Login.css";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [done, setDone] = useState("");
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await Api.forgotPassword(email.trim());
      setDone(res?.message || "If this email is registered, a reset link has been sent.");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-showcase">
        <div className="showcase-icon">🔑</div>
        <h1>
          SMART <span>EV</span>
        </h1>
        <strong>Forgot your password?</strong>
        <p>Enter your registered email and we will send you a link to choose a new password.</p>
      </div>
      <div className="auth-form-wrap">
        <form className="auth-card" onSubmit={submit}>
          <h2>Reset Password</h2>
          <p className="auth-sub">We will email you a reset link</p>
          <Message text={error} />
          <Message text={done} type="success" />
          {!done && (
            <>
              <div className="input-group">
                <label className="label">Email</label>
                <input
                  className="input"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@example.com"
                  required
                />
              </div>
              <button className="btn btn-primary btn-block auth-submit" disabled={loading}>
                {loading ? "Sending..." : "Send reset link"}
              </button>
            </>
          )}
          <p className="auth-switch">
            Remembered it? <Link to="/login">Back to login</Link>
          </p>
        </form>
      </div>
    </div>
  );
}
