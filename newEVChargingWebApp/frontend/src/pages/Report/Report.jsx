import React, { useEffect, useState } from "react";
import { Api } from "../../services/api";
import PageHeader from "../../components/common/PageHeader";
import Message from "../../components/common/Message";
import "./Report.css";

export default function Report() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadReport = async () => {
      try {
        const result = await Api.reportSummary();
        setData(result);
      } catch (e) {
        setError(e.message);
      }
    };

    loadReport();
  }, []);

  const personal = data?.scope === "PERSONAL";

  const cards = data
    ? personal
      ? [
          ["🚗", "My Vehicles", data.totalVehicles],
          ["📅", "My Bookings", data.totalBookings],
          ["🔌", "My Sessions", data.totalSessions],
          [
            "⚡",
            "Energy Charged",
            `${Number(data.totalChargingUnits || 0).toFixed(2)} kWh`,
          ],
          ["₹", "Total Spent", `₹${Number(data.totalSpent || 0).toFixed(2)}`],
        ]
      : [
          ["👥", "Total Users", data.totalUsers],
          ["🚗", "Total Vehicles", data.totalVehicles],
          ["📍", "Total Stations", data.totalStations],
          ["📅", "Total Bookings", data.totalBookings],
          [
            "⚡",
            "Total Charging Units",
            `${Number(data.totalChargingUnits || 0).toFixed(2)} kWh`,
          ],
          [
            "₹",
            "Total Revenue",
            `₹${Number(data.totalRevenue || 0).toFixed(2)}`,
          ],
        ]
    : [];

  return (
    <div className="container">
      <PageHeader
        icon="📊"
        title="Reports"
        subtitle={personal ? "Your personal usage and spending summary." : "System-wide usage and revenue summary."}
      />

      <Message text={error} />

      {data ? (
        <div className="report-grid">
          {cards.map(([icon, title, value]) => (
            <div className="report-card" key={title}>
              <span>{icon}</span>
              <p>{title}</p>
              <strong>{value}</strong>
            </div>
          ))}
        </div>
      ) : (
        !error && (
          <div className="empty-state">
            Loading report...
          </div>
        )
      )}
    </div>
  );
}
