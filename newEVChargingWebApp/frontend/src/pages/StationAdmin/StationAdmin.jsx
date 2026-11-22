import React, { useCallback, useEffect, useState } from "react";
import { Api } from "../../services/api";
import PageHeader from "../../components/common/PageHeader";
import Message from "../../components/common/Message";
import StationForm from "../Admin/StationForm";
import "../Admin/Admin.css";

export default function StationAdmin() {
  const [stations, setStations] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  // undefined = closed, null = add, object = edit
  const [editing, setEditing] = useState(undefined);

  const load = useCallback(() => {
    setLoading(true);
    return Api.stations()
      .then((data) => setStations(data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const save = async (payload) => {
    if (editing) {
      await Api.updateStation(editing.stationId, payload);
    } else {
      await Api.addStation(payload);
    }
    setEditing(undefined);
    await load();
  };

  return (
    <div className="container">
      <PageHeader
        icon="📍"
        title="Station Admin Dashboard"
        subtitle="Add and edit charging stations."
      />

      <Message text={error} />

      <button className="btn btn-primary" onClick={() => setEditing(null)}>
        + Add station
      </button>

      {loading ? (
        <div className="empty-state">Loading stations...</div>
      ) : (
        <div className="table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Location</th>
                <th>Slots</th>
                <th>Price/kWh</th>
                <th>Charger</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {stations.map((s) => (
                <tr key={s.stationId}>
                  <td>{s.stationId}</td>
                  <td>{s.stationName}</td>
                  <td>{s.sLocation}</td>
                  <td>
                    {s.availableSlots}/{s.totalSlots}
                  </td>
                  <td>₹{s.pricePerKwt}</td>
                  <td>{s.chargerType}</td>
                  <td>{s.status}</td>
                  <td>
                    <button
                      className="btn btn-outline btn-sm"
                      onClick={() => setEditing(s)}
                    >
                      Edit
                    </button>
                  </td>
                </tr>
              ))}
              {!stations.length && (
                <tr>
                  <td colSpan="8" className="muted">
                    No stations found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      <StationForm
        open={editing !== undefined}
        station={editing || null}
        onClose={() => setEditing(undefined)}
        onSave={save}
      />
    </div>
  );
}
