import React, { useEffect, useMemo, useState } from "react";
import { Api } from "../../services/api";
import PageHeader from "../../components/common/PageHeader";
import Message from "../../components/common/Message";

export default function UserAdmin() {
  const [users, setUsers] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  useEffect(() => {
    Api.adminUsers()
      .then((data) => setUsers(data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return users;
    return users.filter((u) =>
      [u.fullName, u.email, u.mobileNo].join(" ").toLowerCase().includes(q),
    );
  }, [users, search]);

  return (
    <div className="container">
      <PageHeader
        icon="👥"
        title="User Admin Dashboard"
        subtitle="View registered users."
      />

      <Message text={error} />

      {loading ? (
        <div className="empty-state">Loading users...</div>
      ) : (
        <>
          <div className="grid grid-3 admin-stats">
            <div className="card">
              <div className="card-title">Total Users</div>
              <div className="status-value">{users.length}</div>
            </div>
          </div>

          <input
            className="input"
            placeholder="Search by name, email or mobile"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />

          <h2 className="sub-heading">All Users</h2>
          <div className="table-wrap">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Mobile</th>
                  <th>Role</th>
                  <th>Joined</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((u) => (
                  <tr key={u.userId}>
                    <td>{u.userId}</td>
                    <td>{u.fullName}</td>
                    <td>{u.email}</td>
                    <td>{u.mobileNo}</td>
                    <td>{u.role}</td>
                    <td>
                      {u.createdAt
                        ? new Date(u.createdAt).toLocaleDateString()
                        : "-"}
                    </td>
                  </tr>
                ))}
                {!filtered.length && (
                  <tr>
                    <td colSpan="6" className="muted">
                      No users found.
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
