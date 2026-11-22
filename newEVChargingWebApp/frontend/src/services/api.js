import { Session } from "./session";

const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";

async function apiRequest(method, path, body) {
  const token = Session.token();
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(API_BASE + path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new Error(
      "Cannot reach the server. Please check your connection and try again.",
    );
  }

  let data = null;
  try {
    data = await res.json();
  } catch {
    /* empty body */
  }

  if (res.status === 401 && token) {
    // token rejected / expired: drop the stale login and go to the login page
    Session.clear();
    window.location.assign("/login");
    throw new Error("Your session has expired. Please login again.");
  }

  if (!res.ok) throw new Error(data?.error || `Request failed (${res.status})`);
  return data;
}

export const Api = {
  // ---- auth (login/register return { token, expiresAt, user })
  register: (p) => apiRequest("POST", "/api/users/register", p),
  login: (p) => apiRequest("POST", "/api/users/login", p),
  forgotPassword: (email) =>
    apiRequest("POST", "/api/users/forgot-password", { email }),
  resetPassword: (token, newPassword) =>
    apiRequest("POST", "/api/users/reset-password", { token, newPassword }),

  // ---- profile
  me: () => apiRequest("GET", "/api/users/me"),
  updateProfile: (p) => apiRequest("PUT", "/api/users/me", p),
  changePassword: (p) => apiRequest("PUT", "/api/users/me/password", p),

  // ---- vehicles (the backend takes the owner from the JWT)
  vehicles: (id) => apiRequest("GET", `/api/vehicles/user/${id}`),
  addVehicle: (p) => apiRequest("POST", "/api/vehicles", p),
  updateBattery: (id, batteryPercentage) =>
    apiRequest(
      "PUT",
      `/api/vehicles/${id}/battery?batteryPercentage=${encodeURIComponent(batteryPercentage)}`,
    ),

  // ---- wallet (always the logged-in user's own wallet)
  wallet: () => apiRequest("GET", "/api/wallet"),
  walletTransactions: () => apiRequest("GET", "/api/wallet/transactions"),
  walletAdd: (amount) => apiRequest("POST", "/api/wallet/add", { amount }), // dev-only on the server
  walletTopupOrder: (amount) =>
    apiRequest("POST", "/api/wallet/topup/create-order", { amount }),
  walletTopupVerify: (r) =>
    apiRequest("POST", "/api/wallet/topup/verify", {
      razorpay_order_id: r.razorpay_order_id,
      razorpay_payment_id: r.razorpay_payment_id,
      razorpay_signature: r.razorpay_signature,
    }),

  walletTopupFail: (r) =>
    apiRequest("POST", "/api/wallet/topup/fail", {
      razorpay_order_id: r.razorpay_order_id,
      razorpay_payment_id: r.razorpay_payment_id,
    }),

  // ---- stations
  stations: () => apiRequest("GET", "/api/stations"),
  stationById: (id) => apiRequest("GET", `/api/stations/${id}`),
  addStation: (p) => apiRequest("POST", "/api/stations", p), // ADMIN / STATION_OWNER
  updateStation: (id, p) => apiRequest("PUT", `/api/stations/${id}`, p),
  deleteStation: (id) => apiRequest("DELETE", `/api/stations/${id}`),

  // ---- station reviews
  ratings: () => apiRequest("GET", "/api/stations/ratings"),
  reviews: (stationId) =>
    apiRequest("GET", `/api/stations/${stationId}/reviews`),
  saveReview: (stationId, p) =>
    apiRequest("POST", `/api/stations/${stationId}/reviews`, p),

  // ---- bookings
  bookings: (id) => apiRequest("GET", `/api/bookings/user/${id}`),
  bookingById: (id) => apiRequest("GET", `/api/bookings/${id}`),
  createBooking: (p) => apiRequest("POST", "/api/bookings", p),

  cancelBooking: (bookingId, cancellationReason) =>
    apiRequest("PUT", `/api/bookings/${bookingId}/cancel`, {
      cancellationReason,
    }),

  rescheduleBooking: (bookingId, bookingTime) =>
    apiRequest("PUT", `/api/bookings/${bookingId}/reschedule`, {
      bookingTime,
    }),

  // ---- charging sessions (start time is set by the server)
  startCharging: (bookingId) =>
    apiRequest("POST", "/api/sessions", { bookingId }),
  stopCharging: (sessionId, endBatteryPercentage) =>
    apiRequest(
      "POST",
      `/api/sessions/stop?sessionId=${sessionId}&endBatteryPercentage=${endBatteryPercentage}`,
    ),
  sessions: (id) => apiRequest("GET", `/api/sessions?userId=${id}`),
  sessionById: (id) => apiRequest("GET", `/api/sessions/${id}`),

  sessionByBooking: (bookingId) =>
    apiRequest("GET", `/api/sessions/booking/${bookingId}`),

  // ---- payments (list = the caller's own payments)
  payments: () => apiRequest("GET", "/api/payments"),
  paymentBySession: (sessionId) =>
    apiRequest("GET", `/api/payments/session/${sessionId}`),
  payWithWallet: (sessionId) =>
    apiRequest("POST", `/api/payments/pay-with-wallet?sessionId=${sessionId}`),
  createRazorpayOrder: (sessionId) =>
    apiRequest("POST", `/api/payments/create-order?sessionId=${sessionId}`),
  verifyRazorpayPayment: (sessionId, paymentResponse) =>
    apiRequest("POST", "/api/payments/verify-payment", {
      sessionId,
      razorpay_payment_id: paymentResponse.razorpay_payment_id,
      razorpay_signature: paymentResponse.razorpay_signature,
    }),
  paymentFailed: (sessionId, reason) =>
    apiRequest("POST", "/api/payments/failed", { sessionId, reason }),

  // ---- admin only
  adminUsers: () => apiRequest("GET", "/api/users"),
 createDepartmentAdmin: (data) =>
  apiRequest("POST", "/api/users/department-admins", data),

  updateUserRole: (userId, role) =>
    apiRequest("PUT", `/api/users/${userId}/role`, { role }),
  adminBookings: () => apiRequest("GET", "/api/bookings"),
  adminSessions: () => apiRequest("GET", "/api/sessions/all"),
  adminActiveSessions: () => apiRequest("GET", "/api/sessions/active"),
  adminPayments: () => apiRequest("GET", "/api/payments/all"),

    adminVehicles: () => apiRequest("GET", "/api/vehicles"),

    departmentAdmins: () =>
  apiRequest("GET", "/api/users/department-admins"),

    updateDepartmentAdmin: (userId, data) =>
  apiRequest("PUT", `/api/users/department-admins/${userId}`, data),

    deleteDepartmentAdmin: (userId) =>
  apiRequest("DELETE", `/api/users/department-admins/${userId}`),
    

  // ---- reports (admin: system-wide, others: personal)
  reportSummary: () => apiRequest("GET", "/api/reports/summary"),

  // ---- notifications
  notifications: () => apiRequest("GET", "/api/notifications"),
  unreadNotifications: () => apiRequest("GET", "/api/notifications/unread"),
  unreadNotificationCount: () =>
    apiRequest("GET", "/api/notifications/unread/count"),
  markNotificationAsRead: (notificationId) =>
    apiRequest("PATCH", `/api/notifications/${notificationId}/read`),
  markAllNotificationsAsRead: () =>
    apiRequest("PATCH", "/api/notifications/read-all"),

  deleteNotification: (notificationId) =>
    apiRequest("DELETE", `/api/notifications/${notificationId}`),
  deleteAllNotifications: () => apiRequest("DELETE", "/api/notifications"),
};
