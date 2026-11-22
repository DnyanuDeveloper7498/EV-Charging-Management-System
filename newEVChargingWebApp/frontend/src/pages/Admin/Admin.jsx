import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Api } from "../../services/api";
import { Session } from "../../services/session";
import PageHeader from "../../components/common/PageHeader";
import Message from "../../components/common/Message";
import StationForm from "./StationForm";
import "./Admin.css";

const ROLES = ["USER", "STATION_OWNER"];

const DEPARTMENT_ROLES = [
  "USER_ADMIN",
  "VEHICLE_ADMIN",
  "CHARGING_ADMIN",
  "FINANCE_ADMIN",
  "SUPPORT_ADMIN",
  "STATION_ADMIN",
];

function dayKey(date) {
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

export default function Admin() {
  const me = Session.get();
  const isAdmin = String(me.role).toUpperCase() === "ADMIN";
  const [tab, setTab] = useState(isAdmin ? "overview" : "stations");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  return (
    <div className="container">
      <PageHeader
        icon="🛠️"
        title={isAdmin ? "Admin Panel" : "Station Owner Panel"}
        subtitle={
          isAdmin
            ? "Manage stations, users and monitor the system."
            : "Manage the stations you own."
        }
      />

      <div className="admin-tabs">
        {isAdmin && (
          <button
            className={tab === "overview" ? "active" : ""}
            onClick={() => setTab("overview")}
          >
            📊 Overview
          </button>
        )}
        <button
          className={tab === "stations" ? "active" : ""}
          onClick={() => setTab("stations")}
        >
          📍 Stations
        </button>
        {isAdmin && (
          <button
            className={tab === "users" ? "active" : ""}
            onClick={() => setTab("users")}
          >
            👥 Users
          </button>
        )}

        {isAdmin && (
          <button
            className={tab === "department-admins" ? "active" : ""}
            onClick={() => setTab("department-admins")}
          >
            🏢 Department Admins
          </button>
        )}
      </div>

      <Message text={error} />
      <Message text={notice} type="success" />

      {tab === "overview" && isAdmin && <Overview onError={setError} />}
      {tab === "stations" && (
        <StationsTab
          me={me}
          isAdmin={isAdmin}
          onError={setError}
          onNotice={setNotice}
        />
      )}
      {tab === "users" && isAdmin && (
        <UsersTab me={me} onError={setError} onNotice={setNotice} />
      )}

      {tab === "department-admins" && isAdmin && (
        <>
          <DepartmentAdminsTab onError={setError} onNotice={setNotice} />

          <DepartmentAdminsListTab />
        </>
      )}
    </div>
  );
}

/* ------------------------------------------------------------------ Overview */
function Overview({ onError }) {
  const [data, setData] = useState(null);

  useEffect(() => {
    let alive = true;
    Promise.allSettled([
      Api.adminUsers(),
      Api.stations(),
      Api.adminActiveSessions(),
      Api.adminPayments(),
      Api.adminBookings(),
    ]).then((r) => {
      if (!alive) return;
      const val = (i) => (r[i].status === "fulfilled" ? r[i].value : []);
      const failed = r.find((x) => x.status === "rejected");
      if (failed)
        onError(failed.reason?.message || "Some data could not be loaded.");
      setData({
        users: val(0),
        stations: val(1),
        active: val(2),
        payments: val(3),
        bookings: val(4),
      });
    });
    return () => {
      alive = false;
    };
  }, [onError]);

  const stats = useMemo(() => {
    if (!data) return null;
    const paid = data.payments.filter((p) => p.paymentStatus === "SUCCESS");
    const revenue = paid.reduce((t, p) => t + Number(p.amount || 0), 0);

    const days = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      days.push({
        key: dayKey(d),
        label: d.toLocaleDateString(undefined, { weekday: "short" }),
        total: 0,
      });
    }
    paid.forEach((p) => {
      const slot = days.find((d) => d.key === dayKey(p.paymentTime));
      if (slot) slot.total += Number(p.amount || 0);
    });

    const byStatus = {};
    data.bookings.forEach((b) => {
      byStatus[b.bookingStatus] = (byStatus[b.bookingStatus] || 0) + 1;
    });
    const recent = [...data.payments]
      .sort((a, b) => new Date(b.paymentTime) - new Date(a.paymentTime))
      .slice(0, 8);
    return {
      revenue,
      days,
      byStatus,
      recent,
      max: Math.max(1, ...days.map((d) => d.total)),
    };
  }, [data]);

  if (!data || !stats) return <div className="empty-state">Loading...</div>;

  return (
    <>
      <div className="grid grid-3 admin-stats">
        <div className="card">
          <div className="card-title">👥 Users</div>
          <div className="status-value">{data.users.length}</div>
        </div>
        <div className="card">
          <div className="card-title">📍 Stations</div>
          <div className="status-value">{data.stations.length}</div>
        </div>
        <div className="card">
          <div className="card-title">⚡ Active sessions</div>
          <div className="status-value">{data.active.length}</div>
        </div>
        <div className="card">
          <div className="card-title">📅 Bookings</div>
          <div className="status-value">{data.bookings.length}</div>
        </div>
        <div className="card">
          <div className="card-title">💰 Revenue (paid)</div>
          <div className="status-value">₹{stats.revenue.toFixed(2)}</div>
        </div>
        <div className="card">
          <div className="card-title">Booking status</div>
          <div className="status-chips">
            {Object.keys(stats.byStatus).length === 0 && (
              <span className="muted">No bookings yet</span>
            )}
            {Object.entries(stats.byStatus).map(([k, v]) => (
              <span className="badge" key={k}>
                {k}: {v}
              </span>
            ))}
          </div>
        </div>
      </div>

      <h2 className="sub-heading">Revenue — last 7 days</h2>
      <div className="card bars">
        {stats.days.map((d) => (
          <div className="bar-col" key={d.key} title={`₹${d.total.toFixed(2)}`}>
            <span className="bar-val">₹{Math.round(d.total)}</span>
            <div
              className="bar"
              style={{
                height: `${Math.max(4, (d.total / stats.max) * 120)}px`,
              }}
            />
            <span className="bar-label">{d.label}</span>
          </div>
        ))}
      </div>

      <h2 className="sub-heading">Recent payments</h2>
      {!stats.recent.length ? (
        <div className="empty-state">No payments yet.</div>
      ) : (
        <div className="table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Session</th>
                <th>Method</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Time</th>
              </tr>
            </thead>
            <tbody>
              {stats.recent.map((p) => (
                <tr key={p.paymentId}>
                  <td>{p.paymentId}</td>
                  <td>#{p.sessionId}</td>
                  <td>{p.paymentMethod}</td>
                  <td>₹{Number(p.amount || 0).toFixed(2)}</td>
                  <td>
                    <span
                      className={`badge ${p.paymentStatus === "SUCCESS" ? "" : "gray"}`}
                    >
                      {p.paymentStatus}
                    </span>
                  </td>
                  <td>
                    {p.paymentTime
                      ? new Date(p.paymentTime).toLocaleString()
                      : "-"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

/* ------------------------------------------------------------------ Stations */
function StationsTab({ me, isAdmin, onError, onNotice }) {
  const [stations, setStations] = useState(null);
  const [editing, setEditing] = useState(undefined); // undefined = closed, null = add, object = edit

  const load = useCallback(async () => {
    try {
      const all = await Api.stations();
      setStations(isAdmin ? all : all.filter((s) => s.ownerId === me.userId));
    } catch (e) {
      onError(e.message);
      setStations([]);
    }
  }, [isAdmin, me.userId, onError]);

  useEffect(() => {
    load();
  }, [load]);

  const save = async (payload) => {
    onError("");
    if (editing) {
      await Api.updateStation(editing.stationId, payload);
      onNotice("Station updated.");
    } else {
      await Api.addStation(payload);
      onNotice("Station added.");
    }
    setEditing(undefined);
    await load();
  };

  const remove = async (s) => {
    if (!window.confirm(`Delete "${s.stationName}"? This cannot be undone.`))
      return;
    onError("");
    onNotice("");
    try {
      await Api.deleteStation(s.stationId);
      onNotice("Station deleted.");
      await load();
    } catch (e) {
      onError(e.message);
    }
  };

  return (
    <>
      <div className="row-between" style={{ marginBottom: 14 }}>
        <h2 className="sub-heading" style={{ margin: 0 }}>
          {isAdmin ? "All stations" : "My stations"}
        </h2>
        <button className="btn btn-primary" onClick={() => setEditing(null)}>
          + Add station
        </button>
      </div>

      {stations === null ? (
        <div className="empty-state">Loading...</div>
      ) : !stations.length ? (
        <div className="empty-state">
          No stations yet. Click “Add station” to create the first one.
        </div>
      ) : (
        <div className="table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Location</th>
                <th>Type</th>
                <th>₹/kWh</th>
                <th>Slots</th>
                <th>Status</th>
                <th>Map</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {stations.map((s) => (
                <tr key={s.stationId}>
                  <td>
                    <b>{s.stationName}</b>
                  </td>
                  <td>{s.sLocation}</td>
                  <td>{s.chargerType}</td>
                  <td>{s.pricePerKwt}</td>
                  <td>
                    {s.availableSlots}/{s.totalSlots}
                  </td>
                  <td>
                    <span
                      className={`badge ${["AVAILABLE", "ACTIVE"].includes(String(s.status).toUpperCase()) ? "" : "red"}`}
                    >
                      {s.status}
                    </span>
                  </td>
                  <td>
                    {s.latitude != null && s.longitude != null ? (
                      "✓"
                    ) : (
                      <span className="muted">—</span>
                    )}
                  </td>
                  <td className="actions">
                    <button
                      className="btn btn-outline btn-sm"
                      onClick={() => setEditing(s)}
                    >
                      Edit
                    </button>
                    <button
                      className="btn btn-danger btn-sm"
                      onClick={() => remove(s)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
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
    </>
  );
}

/* --------------------------------------------------------------------- Users */
function UsersTab({ me, onError, onNotice }) {
  const [users, setUsers] = useState(null);
  const [query, setQuery] = useState("");

  const load = useCallback(async () => {
    try {
      setUsers(await Api.adminUsers());
    } catch (e) {
      onError(e.message);
      setUsers([]);
    }
  }, [onError]);

  useEffect(() => {
    load();
  }, [load]);

  const changeRole = async (u, role) => {
    if (role === u.role) return;
    if (!window.confirm(`Change ${u.fullName}'s role to ${role}?`)) return;
    onError("");
    onNotice("");
    try {
      const updated = await Api.updateUserRole(u.userId, role);
      setUsers((list) =>
        list.map((x) => (x.userId === u.userId ? updated : x)),
      );
      onNotice(`${u.fullName} is now ${role}.`);
    } catch (e) {
      onError(e.message);
    }
  };

  const shown = (users || []).filter((u) =>
    `${u.fullName} ${u.email} ${u.mobileNo}`
      .toLowerCase()
      .includes(query.toLowerCase()),
  );

  return (
    <>
      <div className="row-between" style={{ marginBottom: 14 }}>
        <h2 className="sub-heading" style={{ margin: 0 }}>
          Users {users ? `(${users.length})` : ""}
        </h2>
        <input
          className="input"
          style={{ maxWidth: 280 }}
          placeholder="Search name, email, mobile"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>
      {users === null ? (
        <div className="empty-state">Loading...</div>
      ) : (
        <div className="table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Mobile</th>
                <th>Wallet</th>
                <th>Joined</th>
                <th>Role</th>
              </tr>
            </thead>
            <tbody>
              {shown.map((u) => (
                <tr key={u.userId}>
                  <td>
                    <b>{u.fullName}</b>
                  </td>
                  <td>{u.email}</td>
                  <td>{u.mobileNo}</td>
                  <td>₹{Number(u.walletBalance || 0).toFixed(2)}</td>
                  <td>
                    {u.createdAt
                      ? new Date(u.createdAt).toLocaleDateString()
                      : "-"}
                  </td>
                  <td>
                    <select
                      className="input role-select"
                      value={u.role}
                      disabled={u.userId === me.userId}
                      title={
                        u.userId === me.userId
                          ? "You cannot change your own role"
                          : ""
                      }
                      onChange={(e) => changeRole(u, e.target.value)}
                    >
                      {ROLES.map((r) => (
                        <option key={r} value={r}>
                          {r}
                        </option>
                      ))}
                    </select>
                  </td>
                </tr>
              ))}
              {!shown.length && (
                <tr>
                  <td
                    colSpan={6}
                    className="muted"
                    style={{ textAlign: "center" }}
                  >
                    No users found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

// --------------------------Department control------------

function DepartmentAdminsTab({ onError, onNotice }) {
  const [loading, setLoading] = useState(false);

  const [form, setForm] = useState({
    fullName: "",
    email: "",
    mobileNo: "",
    password: "",
    role: "USER_ADMIN",
  });

  const handleChange = (e) => {
    setForm({
      ...form,
      [e.target.name]: e.target.value,
    });
  };

  const createAdmin = async (e) => {
    e.preventDefault();

    onError("");
    onNotice("");
    setLoading(true);

    try {
      const created = await Api.createDepartmentAdmin(form);

      onNotice(`${created.fullName} successfully created as ${created.role}.`);

      setForm({
        fullName: "",
        email: "",
        mobileNo: "",
        password: "",
        role: "USER_ADMIN",
      });
    } catch (error) {
      onError(error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card" style={{ maxWidth: 750 }}>
      <h2 className="sub-heading" style={{ marginTop: 0 }}>
        Create Department Admin
      </h2>

      <p className="muted">
        Create an administrator for a specific department.
      </p>

      <form onSubmit={createAdmin}>
        <div className="admin-form-row">
          <div>
            <label>Full Name</label>
            <input
              className="input"
              name="fullName"
              value={form.fullName}
              onChange={handleChange}
              placeholder="Enter full name"
              maxLength={100}
              required
            />
          </div>

          <div>
            <label>Email Address</label>
            <input
              className="input"
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              placeholder="Enter email"
              maxLength={150}
              required
            />
          </div>

          <div>
            <label>Mobile Number</label>
            <input
              className="input"
              name="mobileNo"
              value={form.mobileNo}
              onChange={handleChange}
              placeholder="10 digit mobile number"
              pattern="[0-9]{10}"
              maxLength={10}
              required
            />
          </div>

          <div>
            <label>Temporary Password</label>
            <input
              className="input"
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              placeholder="Minimum 8 characters"
              minLength={8}
              maxLength={72}
              required
            />
          </div>
        </div>

        <div style={{ marginTop: 16 }}>
          <label>Department Role</label>

          <select
            className="input"
            name="role"
            value={form.role}
            onChange={handleChange}
            required
          >
            {DEPARTMENT_ROLES.map((role) => (
              <option key={role} value={role}>
                {role.replace("_", " ")}
              </option>
            ))}
          </select>
        </div>

        <button
          type="submit"
          className="btn btn-primary"
          disabled={loading}
          style={{ marginTop: 20 }}
        >
          {loading ? "Creating..." : "Create Department Admin"}
        </button>
      </form>
    </div>
  );
}

function DepartmentAdminsListTab() {
  const [admins, setAdmins] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadAdmins = useCallback(async () => {
    try {
      setLoading(true);
      setError("");

      const response = await Api.departmentAdmins();
      setAdmins(Array.isArray(response) ? response : []);
    } catch (err) {
      setError(err.message || "Failed to load department admins");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAdmins();
  }, [loadAdmins]);

  const editAdmin = async (admin) => {
    const fullName = window.prompt("Enter full name:", admin.fullName);
    if (fullName === null) return;

    const mobileNo = window.prompt(
      "Enter 10-digit mobile number:",
      admin.mobileNo,
    );
    if (mobileNo === null) return;

    const role = window.prompt(
      "Enter role:\nUSER_ADMIN, VEHICLE_ADMIN, CHARGING_ADMIN, FINANCE_ADMIN, SUPPORT_ADMIN, STATION_ADMIN",
      admin.role,
    );

    if (role === null) return;

    try {
      setError("");

      await Api.updateDepartmentAdmin(admin.userId, {
        fullName: fullName.trim(),
        mobileNo: mobileNo.trim(),
        role: role.trim().toUpperCase(),
      });

      await loadAdmins();
      alert("Department admin updated successfully.");
    } catch (err) {
      setError(err.message || "Failed to update department admin.");
    }
  };

  const deleteAdmin = async (admin) => {
    const confirmed = window.confirm(
      `Are you sure you want to delete ${admin.fullName}?`,
    );

    if (!confirmed) return;

    try {
      setError("");

      await Api.deleteDepartmentAdmin(admin.userId);

      await loadAdmins();

      alert("Department admin deleted successfully.");
    } catch (err) {
      setError(err.message || "Failed to delete department admin.");
    }
  };

  if (loading) return <p>Loading department admins...</p>;

  if (error) {
    return (
      <div className="empty-state">
        <p>{error}</p>
        <button className="btn" onClick={loadAdmins}>
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="row-between">
        <h3 className="sub-heading">Department Admin List</h3>
        <button className="btn" onClick={loadAdmins}>
          Refresh
        </button>
      </div>

      {admins.length === 0 ? (
        <p className="empty-state">No department admins found.</p>
      ) : (
        <div className="admin-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Mobile</th>
                <th>Role</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {admins.map((admin) => (
                <tr key={admin.userId}>
                  <td>{admin.userId}</td>
                  <td>{admin.fullName}</td>
                  <td>{admin.email}</td>
                  <td>{admin.mobileNo || "-"}</td>
                  <td>{admin.role}</td>
                  <td>
                    <button
                      className="btn btn-outline btn-sm"
                      onClick={() => editAdmin(admin)}
                    >
                      Edit
                    </button>

                    <button
                      className="btn btn-sm"
                      onClick={() => deleteAdmin(admin)}
                      style={{
                        marginLeft: 8,
                        background: "#dc2626",
                        color: "#fff",
                      }}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
