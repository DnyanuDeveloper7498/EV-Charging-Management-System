import React from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import Header from "./components/layout/Header";
import Footer from "./components/layout/Footer";
import Home from "./pages/Home/Home";
import Login from "./pages/Login/Login";
import Register from "./pages/Register/Register";
import Dashboard from "./pages/Dashboard/Dashboard";
import Stations from "./pages/Stations/Stations";
import Vehicles from "./pages/Vehicles/Vehicles";
import Bookings from "./pages/Bookings/Bookings";
import ChargingSession from "./pages/ChargingSession/ChargingSession";
import Wallet from "./pages/Wallet/Wallet";
import Billing from "./pages/Billing/Billing";
import Report from "./pages/Report/Report";
import ProtectedRoute from "./components/auth/ProtectedRoute";
import ErrorBoundary from "./components/common/ErrorBoundary";
import ForgotPassword from "./pages/ForgotPassword/ForgotPassword";
import ResetPassword from "./pages/ResetPassword/ResetPassword";
import Profile from "./pages/Profile/Profile";
import Admin from "./pages/Admin/Admin";
import Policy from "./pages/Policy/Policy";
import Invoice from "./pages/Invoice/Invoice";
import NotFound from "./pages/NotFound/NotFound";
import AdminLogin from "./pages/AdminLogin/AdminLogin";
import FinanceAdmin from "./pages/FinanceAdmin/FinanceAdmin";

import VehicleAdmin from "./pages/VehicleAdmin/VehicleAdmin";
import ChargingAdmin from "./pages/ChargingAdmin/ChargingAdmin";
import UserAdmin from "./pages/UserAdmin/UserAdmin";
import StationAdmin from "./pages/StationAdmin/StationAdmin";

export default function App() {
  return (
    <div className="app-shell">
      <Header />
      <main className="app-main">
        <ErrorBoundary>
          <Routes>
            {/* --------------public acesss------------ */}

            <Route path="/stations" element={<Stations />} />

            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/admin/login" element={<AdminLogin />} />

            <Route path="/register" element={<Register />} />

            {/*  -------------------- private acesss-----------------*/}

            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <Dashboard />
                </ProtectedRoute>
              }
            />

            <Route
              path="/vehicles"
              element={
                <ProtectedRoute>
                  <Vehicles />
                </ProtectedRoute>
              }
            />
            <Route
              path="/bookings"
              element={
                <ProtectedRoute>
                  <Bookings />
                </ProtectedRoute>
              }
            />
            <Route
              path="/session"
              element={
                <ProtectedRoute>
                  <ChargingSession />
                </ProtectedRoute>
              }
            />
            <Route
              path="/wallet"
              element={
                <ProtectedRoute>
                  <Wallet />
                </ProtectedRoute>
              }
            />
            <Route
              path="/billing"
              element={
                <ProtectedRoute>
                  <Billing />
                </ProtectedRoute>
              }
            />
            <Route
              path="/report"
              element={
                <ProtectedRoute>
                  <Report />
                </ProtectedRoute>
              }
            />
            <Route
              path="/profile"
              element={
                <ProtectedRoute>
                  <Profile />
                </ProtectedRoute>
              }
            />
            <Route
              path="/invoice/:sessionId"
              element={
                <ProtectedRoute>
                  <Invoice />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin"
              element={
                <ProtectedRoute allowedRoles={["ADMIN", "STATION_OWNER"]}>
                  <Admin />
                </ProtectedRoute>
              }
            />

            <Route
              path="/finance-admin"
              element={
                <ProtectedRoute allowedRoles={["ADMIN", "FINANCE_ADMIN"]}>
                  <FinanceAdmin />
                </ProtectedRoute>
              }
            />


                        <Route
              path="/vehicle-admin"
              element={
                <ProtectedRoute allowedRoles={["ADMIN", "VEHICLE_ADMIN"]}>
                  <VehicleAdmin />
                </ProtectedRoute>
              }
            />

            <Route
              path="/charging-admin"
              element={
                <ProtectedRoute allowedRoles={["ADMIN", "CHARGING_ADMIN"]}>
                  <ChargingAdmin />
                </ProtectedRoute>
              }
            />

            <Route
              path="/user-admin"
              element={
                <ProtectedRoute allowedRoles={["ADMIN", "USER_ADMIN"]}>
                  <UserAdmin />
                </ProtectedRoute>
              }
            />

            <Route
              path="/station-admin"
              element={
                <ProtectedRoute allowedRoles={["ADMIN", "STATION_ADMIN"]}>
                  <StationAdmin />
                </ProtectedRoute>
              }
            />

            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />
            <Route path="/terms" element={<Policy />} />
            <Route path="/privacy" element={<Policy />} />
            <Route path="/refund" element={<Policy />} />
            <Route path="/home" element={<Navigate to="/" replace />} />
            <Route path="*" element={<NotFound />} />
          </Routes>
        </ErrorBoundary>
      </main>
      <Footer />
    </div>
  );
}
