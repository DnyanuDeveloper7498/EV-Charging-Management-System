import React, { useEffect, useMemo, useState } from "react";
import { Api } from "../../services/api";
import PageHeader from "../../components/common/PageHeader";
import Message from "../../components/common/Message";

export default function VehicleAdmin() {
  const [vehicles, setVehicles] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  useEffect(() => {
    Api.adminVehicles()
      .then((data) => setVehicles(data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return vehicles;
    return vehicles.filter((v) =>
      [v.vehicleNumber, v.brand, v.model, v.vehicleType]
        .join(" ")
        .toLowerCase()
        .includes(q),
    );
  }, [vehicles, search]);

  return (
    <div className="container">
      <PageHeader
        icon="🚗"
        title="Vehicle Admin Dashboard"
        subtitle="View all registered vehicles."
      />

      <Message text={error} />

      {loading ? (
        <div className="empty-state">Loading vehicles...</div>
      ) : (
        <>
          <div className="grid grid-3 admin-stats">
            <div className="card">
              <div className="card-title">Total Vehicles</div>
              <div className="status-value">{vehicles.length}</div>
            </div>
          </div>

          <input
            className="input"
            placeholder="Search by number, brand, model or type"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />

          <h2 className="sub-heading">All Vehicles</h2>

          <div className="table-wrap">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>User ID</th>
                  <th>Number</th>
                  <th>Brand</th>
                  <th>Model</th>
                  <th>Type</th>
                  <th>Battery Capacity</th>
                  <th>Battery %</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((v) => (
                  <tr key={v.vehicleId}>
                    <td>{v.vehicleId}</td>
                    <td>{v.userId}</td>
                    <td>{v.vehicleNumber}</td>
                    <td>{v.brand}</td>
                    <td>{v.model}</td>
                    <td>{v.vehicleType}</td>
                    <td>{v.batteryCapacity}</td>
                    <td>{v.batteryPercentage}%</td>
                  </tr>
                ))}
                {!filtered.length && (
                  <tr>
                    <td colSpan="8" className="muted">
                      No vehicles found.
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
