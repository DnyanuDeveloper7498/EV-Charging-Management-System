
import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { Session } from "../../services/session";

const ADMIN_ROLES = [
  "ADMIN",
  "USER_ADMIN",
  "VEHICLE_ADMIN",
  "CHARGING_ADMIN",
  "FINANCE_ADMIN",
  "SUPPORT_ADMIN",
  "STATION_ADMIN",
];

export default function ProtectedRoute({ children, allowedRoles }) {
  const location = useLocation();
  const user = Session.get();

  // Login required
  if (!user) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location.pathname }}
      />
    );
  }

  const role = String(user.role || "").toUpperCase();
  const isAdmin = ADMIN_ROLES.includes(role);

  // Admin account cannot access normal user protected pages
  if (!allowedRoles && isAdmin) {
    Session.clear();

    return <Navigate to="/login" replace />;
  }

  // Only permitted roles can access role-specific pages
  if (
    allowedRoles &&
    !allowedRoles.map((r) => String(r).toUpperCase()).includes(role)
  ) {
    Session.clear();

    const isAdminPage = allowedRoles.some((r) =>
      ADMIN_ROLES.includes(String(r).toUpperCase())
    );

    return (
      <Navigate
        to={isAdminPage ? "/admin/login" : "/login"}
        replace
      />
    );
  }

  return children;
}
