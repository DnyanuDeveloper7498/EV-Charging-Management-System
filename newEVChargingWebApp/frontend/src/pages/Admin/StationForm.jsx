import React, { useEffect, useState } from "react";
import Modal from "../../components/common/Modal";
import Message from "../../components/common/Message";

const EMPTY = {
  stationName: "",
  sLocation: "",
  totalSlots: "4",
  pricePerKwt: "",
  chargerType: "",
  status: "AVAILABLE",
  latitude: "",
  longitude: "",
};

const CHARGER_TYPES = ["AC Slow", "AC Fast", "DC Fast", "CCS2", "CHAdeMO", "Type 2"];
const STATUSES = ["AVAILABLE", "MAINTENANCE", "OFFLINE"];

/** Add / edit a station. `station` = null for "add". onSave(payload) must return a promise. */
export default function StationForm({ open, station, onClose, onSave }) {
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setError("");
    setForm(
      station
        ? {
            stationName: station.stationName ?? "",
            sLocation: station.sLocation ?? "",
            totalSlots: String(station.totalSlots ?? ""),
            pricePerKwt: String(station.pricePerKwt ?? ""),
            chargerType: station.chargerType ?? "",
            status: (station.status || "AVAILABLE").toUpperCase(),
            latitude: station.latitude ?? "",
            longitude: station.longitude ?? "",
          }
        : EMPTY,
    );
  }, [open, station]);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const useMyLocation = () => {
    if (!navigator.geolocation) return setError("Your browser does not support location.");
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        set("latitude", pos.coords.latitude.toFixed(6));
        set("longitude", pos.coords.longitude.toFixed(6));
      },
      () => setError("Could not read your location. Please allow location access or type the coordinates."),
    );
  };

  const submit = async (e) => {
    e.preventDefault();
    setError("");

    const lat = form.latitude === "" ? null : Number(form.latitude);
    const lng = form.longitude === "" ? null : Number(form.longitude);
    if ((lat === null) !== (lng === null)) {
      return setError("Enter both latitude and longitude, or leave both empty.");
    }
    if (lat !== null && (Number.isNaN(lat) || lat < -90 || lat > 90)) {
      return setError("Latitude must be between -90 and 90.");
    }
    if (lng !== null && (Number.isNaN(lng) || lng < -180 || lng > 180)) {
      return setError("Longitude must be between -180 and 180.");
    }

    setSaving(true);
    try {
      await onSave({
        stationName: form.stationName.trim(),
        sLocation: form.sLocation.trim(),
        totalSlots: Number(form.totalSlots),
        pricePerKwt: Number(form.pricePerKwt),
        chargerType: form.chargerType.trim(),
        status: form.status,
        latitude: lat,
        longitude: lng,
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title={station ? "Edit Station" : "Add Station"}>
      <form onSubmit={submit}>
        <Message text={error} />
        <div className="input-group">
          <label className="label">Station name</label>
          <input className="input" value={form.stationName} maxLength={150} required
            onChange={(e) => set("stationName", e.target.value)} />
        </div>
        <div className="input-group">
          <label className="label">Location / address</label>
          <input className="input" value={form.sLocation} maxLength={250} required
            onChange={(e) => set("sLocation", e.target.value)} />
        </div>
        <div className="admin-form-row">
          <div className="input-group">
            <label className="label">Total slots</label>
            <input className="input" type="number" min="1" max="1000" value={form.totalSlots} required
              onChange={(e) => set("totalSlots", e.target.value)} />
          </div>
          <div className="input-group">
            <label className="label">Price per kWh (₹)</label>
            <input className="input" type="number" min="0" step="0.01" value={form.pricePerKwt} required
              onChange={(e) => set("pricePerKwt", e.target.value)} />
          </div>
        </div>
        <div className="admin-form-row">
          <div className="input-group">
            <label className="label">Charger type</label>
            <input className="input" list="charger-types" value={form.chargerType} maxLength={50} required
              onChange={(e) => set("chargerType", e.target.value)} />
            <datalist id="charger-types">
              {CHARGER_TYPES.map((t) => (
                <option key={t} value={t} />
              ))}
            </datalist>
          </div>
          <div className="input-group">
            <label className="label">Status</label>
            <select className="input" value={form.status} onChange={(e) => set("status", e.target.value)}>
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className="admin-form-row">
          <div className="input-group">
            <label className="label">Latitude (for the map)</label>
            <input className="input" inputMode="decimal" value={form.latitude} placeholder="18.5204"
              onChange={(e) => set("latitude", e.target.value)} />
          </div>
          <div className="input-group">
            <label className="label">Longitude (for the map)</label>
            <input className="input" inputMode="decimal" value={form.longitude} placeholder="73.8567"
              onChange={(e) => set("longitude", e.target.value)} />
          </div>
        </div>
        <button type="button" className="btn btn-outline" onClick={useMyLocation}>
          📍 Use my current location
        </button>

        <div className="modal-actions" style={{ marginTop: 18 }}>
          <button type="button" className="btn btn-outline" onClick={onClose}>
            Cancel
          </button>
          <button className="btn btn-primary" disabled={saving}>
            {saving ? "Saving..." : station ? "Save changes" : "Add station"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
