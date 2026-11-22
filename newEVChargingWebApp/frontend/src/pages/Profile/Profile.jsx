import React, { useEffect, useState } from "react";
import { Api } from "../../services/api";
import { Session } from "../../services/session";
import PageHeader from "../../components/common/PageHeader";
import Message from "../../components/common/Message";
import "./Profile.css";

export default function Profile() {
  const user = Session.get();
  const [me, setMe] = useState(user);
  const [form, setForm] = useState({ fullName: user.fullName || "", mobileNo: user.mobileNo || "" });
  const [pw, setPw] = useState({ currentPassword: "", newPassword: "", confirm: "" });
  const [profileMsg, setProfileMsg] = useState({ text: "", type: "error" });
  const [pwMsg, setPwMsg] = useState({ text: "", type: "error" });
  const [saving, setSaving] = useState(false);
  const [changing, setChanging] = useState(false);

  useEffect(() => {
    Api.me()
      .then((data) => {
        setMe(data);
        setForm({ fullName: data.fullName || "", mobileNo: data.mobileNo || "" });
        Session.save(data);
      })
      .catch((e) => setProfileMsg({ text: e.message, type: "error" }));
  }, []);

  const saveProfile = async (e) => {
    e.preventDefault();
    setProfileMsg({ text: "", type: "error" });
    if (!/^\d{10}$/.test(form.mobileNo)) {
      return setProfileMsg({ text: "Mobile number must contain 10 digits.", type: "error" });
    }
    setSaving(true);
    try {
      const updated = await Api.updateProfile(form);
      setMe(updated);
      Session.save(updated);
      setProfileMsg({ text: "Profile updated.", type: "success" });
    } catch (err) {
      setProfileMsg({ text: err.message, type: "error" });
    } finally {
      setSaving(false);
    }
  };

  const savePassword = async (e) => {
    e.preventDefault();
    setPwMsg({ text: "", type: "error" });
    if (pw.newPassword.length < 8) {
      return setPwMsg({ text: "New password must contain at least 8 characters.", type: "error" });
    }
    if (pw.newPassword !== pw.confirm) {
      return setPwMsg({ text: "New passwords do not match.", type: "error" });
    }
    setChanging(true);
    try {
      await Api.changePassword({ currentPassword: pw.currentPassword, newPassword: pw.newPassword });
      setPw({ currentPassword: "", newPassword: "", confirm: "" });
      setPwMsg({ text: "Password changed successfully.", type: "success" });
    } catch (err) {
      setPwMsg({ text: err.message, type: "error" });
    } finally {
      setChanging(false);
    }
  };

  return (
    <div className="container">
      <PageHeader icon="👤" title="My Profile" subtitle="Manage your account details and password." />

      <div className="profile-grid">
        <div className="card profile-summary">
          <div className="avatar">{(me.fullName || "U").trim().charAt(0).toUpperCase()}</div>
          <h3>{me.fullName}</h3>
          <p>{me.email}</p>
          <span className="badge">{String(me.role || "USER").replace("_", " ")}</span>
          <p className="muted">
            Wallet: <b>₹{Number(me.walletBalance || 0).toFixed(2)}</b>
          </p>
          {me.createdAt && (
            <p className="muted">Member since {new Date(me.createdAt).toLocaleDateString()}</p>
          )}
        </div>

        <div className="profile-forms">
          <form className="card" onSubmit={saveProfile}>
            <h3>Personal details</h3>
            <Message text={profileMsg.text} type={profileMsg.type} />
            <div className="input-group">
              <label className="label">Full name</label>
              <input className="input" value={form.fullName} maxLength={100} required
                onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
            </div>
            <div className="input-group">
              <label className="label">Email (cannot be changed)</label>
              <input className="input" value={me.email || ""} disabled />
            </div>
            <div className="input-group">
              <label className="label">Mobile number</label>
              <input className="input" value={form.mobileNo} inputMode="numeric" maxLength={10} required
                onChange={(e) => setForm({ ...form, mobileNo: e.target.value.replace(/\D/g, "") })} />
            </div>
            <button className="btn btn-primary" disabled={saving}>
              {saving ? "Saving..." : "Save changes"}
            </button>
          </form>

          <form className="card" onSubmit={savePassword}>
            <h3>Change password</h3>
            <Message text={pwMsg.text} type={pwMsg.type} />
            <div className="input-group">
              <label className="label">Current password</label>
              <input className="input" type="password" value={pw.currentPassword} required
                onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })} />
            </div>
            <div className="input-group">
              <label className="label">New password</label>
              <input className="input" type="password" value={pw.newPassword} minLength={8} maxLength={72} required
                onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} />
            </div>
            <div className="input-group">
              <label className="label">Confirm new password</label>
              <input className="input" type="password" value={pw.confirm} minLength={8} maxLength={72} required
                onChange={(e) => setPw({ ...pw, confirm: e.target.value })} />
            </div>
            <button className="btn btn-primary" disabled={changing}>
              {changing ? "Updating..." : "Update password"}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
