import React from "react";
import { Link } from "react-router-dom";
import "./Footer.css";

export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-links">
        <Link to="/terms">Terms</Link>
        <Link to="/privacy">Privacy</Link>
        <Link to="/refund">Refund Policy</Link>
      </div>
      © Smart EV Charging System — Made for a greener commute.
    </footer>
  );
}
