import React from "react";
import { Link } from "react-router-dom";
import "./Home.css";
export default function Home() {
  return (
    <div className="home-page">
      <section className="hero">
        <div className="hero-badge">⚡ SMART & EASY EV CHARGING</div>
        <h1>
          Find. Book. <span>Charge.</span> Drive.
        </h1>
        <p className="hero-accent">Charge & Go</p>
        <p className="hero-text">
          Your smarter way to find and book electric vehicle charging stations,
          manage your vehicle and track every charging session.
        </p>
        <div className="hero-actions">
          <Link className="btn btn-primary" to="/register">
            Get Started
          </Link>
          <Link className="btn btn-outline" to="/stations">
            Find Charging Stations
          </Link>
        </div>
      </section>
      <section className="feature-section">
        <h2>Everything You Need</h2>
        <p className="section-sub">
          Simple tools for a smoother EV charging experience.
        </p>
        <div className="feature-grid">
          {[
            [
              "📍",
              "Charging Stations",
              "Discover available charging stations and choose a convenient slot.",
            ],
            [
              "📅",
              "Book Your Slot",
              "Reserve your preferred charging time in a few clicks.",
            ],
            [
              "🔋",
              "Track Your EV",
              "Manage vehicles, battery status and charging sessions.",
            ],
            [
              "💳",
              "Smart Wallet",
              "Keep your charging balance ready and view payments.",
            ],
            ["🧾", "Billing", "Review energy usage and payment history."],
            ["📊", "Reports", "See system usage and revenue summaries."],
          ].map((x) => (
            <div className="feature-card" key={x[1]}>
              <div className="feature-icon">{x[0]}</div>
              <h3>{x[1]}</h3>
              <p>{x[2]}</p>
            </div>
          ))}
        </div>
      </section>
      <section className="home-stats">
        <div>
          <b>500+</b>
          <span>Stations</span>
        </div>
        <div>
          <b>10K+</b>
          <span>Happy Users</span>
        </div>
        <div>
          <b>24/7</b>
          <span>Service Available</span>
        </div>
      </section>
    </div>
  );
}
