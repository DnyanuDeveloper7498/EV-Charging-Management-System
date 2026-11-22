import React from "react";
import { Link } from "react-router-dom";

export default function NotFound() {
  return (
    <div className="container text-center" style={{ paddingTop: 90 }}>
      <div style={{ fontSize: 64 }}>🔌</div>
      <h1>404 — Page not found</h1>
      <p style={{ color: "var(--gray)" }}>The page you are looking for does not exist or was moved.</p>
      <Link className="btn btn-primary" to="/">Go to Home</Link>
    </div>
  );
}
