import React, { useEffect, useState } from "react";
import { Api } from "../../services/api";
import { Session } from "../../services/session";
import PageHeader from "../../components/common/PageHeader";
import Message from "../../components/common/Message";
import Modal from "../../components/common/Modal";
import "./Vehicles.css";

const empty = {
  brand: "",
  model: "",
  vehicleType: "Car",
  vehicleNumber: "",
  batteryCapacity: "",
  batteryPercentage: "",
};

export default function Vehicles() {
  const user = Session.get();

  const [vehicles, setVehicles] = useState([]);
  const [form, setForm] = useState(empty);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  // Load vehicles
  const load = async () => {
    try {
      const data = await Api.vehicles(user.userId);
      setVehicles(data);
    } catch (e) {
      setError(e.message);
    }
  };

  // IMPORTANT: Do not use useEffect(load, [])
  // because load() returns a Promise.
  useEffect(() => {
    load();
  }, []);

  const save = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      await Api.addVehicle({
        ...form,
        userId: user.userId,
        batteryCapacity: Number(form.batteryCapacity || 0),
        batteryPercentage: Number(form.batteryPercentage || 0),
      });

      setOpen(false);
      setForm(empty);

      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  const update = async (v) => {
    const value = prompt(
      "Enter current battery percentage (0-100):",
      v.batteryPercentage
    );

    if (value === null) return;

    const pct = Number(value);

    if (Number.isNaN(pct) || pct < 0 || pct > 100) {
      alert("Please enter a number between 0 and 100.");
      return;
    }

    try {
      await Api.updateBattery(v.vehicleId, pct);
      await load();
    } catch (e) {
      alert(e.message);
    }
  };

  const color = (p) =>
    p >= 60
      ? "var(--green)"
      : p >= 25
        ? "var(--yellow)"
        : "var(--red)";

  return (
    <div className="container">
      <PageHeader
        icon="🚗"
        title="My Vehicles"
        subtitle="Manage your registered vehicles and battery status."
        actions={
          <button
            className="btn btn-primary"
            onClick={() => setOpen(true)}
          >
            + Add Vehicle
          </button>
        }
      />

      <Message text={error} />

      {!vehicles.length ? (
        <div className="empty-state">
          No vehicles added yet.{" "}
          <button
            className="text-link"
            onClick={() => setOpen(true)}
          >
            Add your first vehicle
          </button>
          .
        </div>
      ) : (
        <div className="vehicle-grid">
          {vehicles.map((v) => (
            <div className="vehicle-card" key={v.vehicleId}>
              <div className="vehicle-top">
                <div>
                  <h3>
                    {v.brand} {v.model}
                  </h3>
                  <p>{v.vehicleNumber}</p>
                </div>

                <strong
                  style={{
                    color: color(v.batteryPercentage),
                  }}
                >
                  {v.batteryPercentage}%
                </strong>
              </div>

              <div className="battery-track">
                <div
                  className="battery-fill"
                  style={{
                    width: `${Math.min(
                      100,
                      Math.max(0, v.batteryPercentage)
                    )}%`,
                    background: color(v.batteryPercentage),
                  }}
                />
              </div>

              <div className="vehicle-meta">
                <span>
                  Type <b>{v.vehicleType}</b>
                </span>

                <span>
                  Capacity <b>{v.batteryCapacity} kWh</b>
                </span>
              </div>

              <button
                className="btn btn-outline btn-block"
                onClick={() => update(v)}
              >
                Update Battery
              </button>
            </div>
          ))}
        </div>
      )}

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        title="Add Vehicle"
      >
        <form onSubmit={save}>
          {[
            ["brand", "Brand", "e.g. Tata"],
            ["model", "Model", "e.g. Nexon EV"],
            ["vehicleNumber", "Vehicle Number", "e.g. MH12AB1234"],
            ["batteryCapacity", "Battery Capacity (kWh)", "e.g. 30.2"],
            ["batteryPercentage", "Current Battery %", "e.g. 45"],
          ].map(([k, l, p]) => (
            <div className="input-group" key={k}>
              <label className="label">{l}</label>

              <input
                className="input"
                type={
                  k === "batteryCapacity" ||
                  k === "batteryPercentage"
                    ? "number"
                    : "text"
                }
                value={form[k]}
                onChange={(e) =>
                  setForm((x) => ({
                    ...x,
                    [k]: e.target.value,
                  }))
                }
                placeholder={p}
                required
              />
            </div>
          ))}

          <div className="input-group">
            <label className="label">Vehicle Type</label>

            <select
              className="input"
              value={form.vehicleType}
              onChange={(e) =>
                setForm((x) => ({
                  ...x,
                  vehicleType: e.target.value,
                }))
              }
            >
              <option>Car</option>
              <option>Bike</option>
              <option>Scooter</option>
              <option>Other</option>
            </select>
          </div>

          <div className="modal-actions">
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => setOpen(false)}
            >
              Cancel
            </button>

            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
            >
              {loading ? "Saving..." : "Save Vehicle"}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
