import React, { useEffect, useState, useRef } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Session } from "../../services/session";
import { Client } from "@stomp/stompjs";

import {
  LuCircleCheck,
  LuCircleX,
  LuCalendarClock,
  LuClock,
  LuBell,
  LuZap,
  LuCreditCard,
} from "react-icons/lu";

import { Api } from "../../services/api";
import "./Header.css";

export default function Header() {
  const [open, setOpen] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const [toast, setToast] = useState(null);

  const knownNotificationIds = useRef(new Set());

  const notificationRef = useRef(null);

  const location = useLocation();
  const navigate = useNavigate();

 const [user, setUser] = useState(() => Session.get());
  

  useEffect(() => {
    if (!notificationOpen) return;

    const handleOutsideClick = (event) => {
      if (
        notificationRef.current &&
        !notificationRef.current.contains(event.target)
      ) {
        setNotificationOpen(false);
      }
    };

    const handleEscape = (event) => {
      if (event.key === "Escape") {
        setNotificationOpen(false);
      }
    };

    document.addEventListener("mousedown", handleOutsideClick);
    document.addEventListener("keydown", handleEscape);

    return () => {
      document.removeEventListener("mousedown", handleOutsideClick);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [notificationOpen]);

  useEffect(() => {
    setNotificationOpen(false);
  }, [location.pathname]);


  useEffect(() => {
  const currentUser = Session.get();
  const role = String(currentUser?.role || "").toUpperCase();

  const adminRoles = [
    "ADMIN",
    "USER_ADMIN",
    "VEHICLE_ADMIN",
    "CHARGING_ADMIN",
    "FINANCE_ADMIN",
    "SUPPORT_ADMIN",
    "STATION_ADMIN",
  ];

  const isAdmin = adminRoles.includes(role);

  // Admin login page: only Admin accounts allowed
  if (
    location.pathname === "/admin/login" &&
    currentUser &&
    !isAdmin
  ) {
    Session.clear();
    setUser(null);
    return;
  }

  // Normal login/register pages: Admin session should logout
  if (
    ["/login", "/register"].includes(location.pathname) &&
    currentUser &&
    isAdmin
  ) {
    Session.clear();
    setUser(null);
    return;
  }

  setUser(currentUser);
}, [location.pathname]);











  const logout = () => {
    Session.clear();
    setOpen(false);
    navigate("/login");
  };

  useEffect(() => {
    if (!user) {
      setNotifications([]);
      setUnreadCount(0);
      knownNotificationIds.current.clear();
      return;
    }

    let active = true;

    const loadNotifications = async () => {
      try {
        const [notificationData, countData] = await Promise.all([
          Api.notifications(),
          Api.unreadNotificationCount(),
        ]);

        if (!active) return;

        const latestNotifications = Array.isArray(notificationData)
          ? notificationData
          : [];

        knownNotificationIds.current = new Set(
          latestNotifications.map((item) => item.notificationId),
        );

        setNotifications(latestNotifications);

        const count =
          typeof countData === "number"
            ? countData
            : Number(
                countData?.unreadCount ??
                  countData?.count ??
                  countData?.total ??
                  0,
              );

        setUnreadCount(Number.isFinite(count) ? count : 0);
      } catch (error) {
        console.error("Failed to load notifications:", error);
      }
    };

    loadNotifications();

    const wsBase = (import.meta.env.VITE_API_BASE || "http://localhost:8080")
      .replace(/\/$/, "")
      .replace(/^http/, "ws");

    const client = new Client({
      brokerURL: `${wsBase}/ws`,

      connectHeaders: {
        Authorization: `Bearer ${Session.token()}`,
      },

      reconnectDelay: 5000,

      onConnect: () => {
        // offine add
        loadNotifications();

        client.subscribe("/user/queue/notifications", (message) => {
          console.log("REAL-TIME NOTIFICATION RECEIVED:", message.body);

          if (!active) return;

          const notification = JSON.parse(message.body);

          if (knownNotificationIds.current.has(notification.notificationId)) {
            return;
          }

          knownNotificationIds.current.add(notification.notificationId);

          setNotifications((previous) => [notification, ...previous]);

          if (!notification.isRead) {
            setUnreadCount((previous) => previous + 1);
          }

          setToast(notification);
        });
      },

      onStompError: (frame) => {
        console.error("STOMP ERROR MESSAGE:", frame.headers.message);
        console.error("STOMP ERROR BODY:", frame.body);
        console.error("STOMP ERROR HEADERS:", frame.headers);
      },

      onWebSocketError: (error) => {
        console.error("WEBSOCKET ERROR:", error);
      },

      onWebSocketClose: (event) => {
        console.log("WEBSOCKET CLOSED:", event.code, event.reason);
      },
    });

    client.activate();

    return () => {
      active = false;
      client.deactivate();
    };
  }, [user?.userId]);

  useEffect(() => {
    if (!toast) return;

    const timer = setTimeout(() => {
      setToast(null);
    }, 5000);

    return () => clearTimeout(timer);
  }, [toast]);

  const markAsRead = async (notificationId) => {
    try {
      await Api.markNotificationAsRead(notificationId);

      setNotifications((previous) =>
        previous.map((item) =>
          item.notificationId === notificationId
            ? { ...item, isRead: true }
            : item,
        ),
      );

      setUnreadCount((count) => Math.max(0, count - 1));
    } catch (error) {
      alert(error.message || "Unable to mark notification as read.");
    }
  };

  const markAllAsRead = async () => {
    try {
      await Api.markAllNotificationsAsRead();

      setNotifications((previous) =>
        previous.map((item) => ({ ...item, isRead: true })),
      );

      setUnreadCount(0);
    } catch (error) {
      alert(error.message || "Unable to mark all notifications as read.");
    }
  };

  const removeNotification = async (notificationId) => {
    const target = notifications.find(
      (item) => item.notificationId === notificationId,
    );

    try {
      await Api.deleteNotification(notificationId);

      setNotifications((previous) =>
        previous.filter((item) => item.notificationId !== notificationId),
      );

      if (target && !target.isRead) {
        setUnreadCount((count) => Math.max(0, count - 1));
      }
    } catch (error) {
      alert(error.message || "Unable to delete notification.");
    }
  };

  const clearAllNotifications = async () => {
    if (!window.confirm("Delete all notifications?")) return;

    try {
      await Api.deleteAllNotifications();

      setNotifications([]);
      setUnreadCount(0);
    } catch (error) {
      alert(error.message || "Unable to clear notifications.");
    }
  };

  const formatDate = (date) => {
    if (!date) return "";

    const parsedDate = new Date(date);

    if (Number.isNaN(parsedDate.getTime())) return "";

    return parsedDate.toLocaleString("en-IN", {
      day: "numeric",
      month: "short",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const getNotificationRoute = (type) => {
    switch (type) {
      case "BOOKING_CONFIRMATION":
      case "BOOKING_CANCELLATION":
      case "BOOKING_RESCHEDULED":
      case "BOOKING_EXPIRED":
      case "BOOKING_REMINDER":
        return "/bookings";

      case "CHARGING_STARTED":
        return "/session";

      case "CHARGING_COMPLETED":
      case "PAYMENT_SUCCESS":
      case "PAYMENT_FAILED":
        return "/billing";

      case "WALLET_TOPUP_SUCCESS":
      case "WALLET_TOPUP_FAILED":
        return "/wallet";

      default:
        return null;
    }
  };

  const openNotification = async (item) => {
    if (!item.isRead) {
      await markAsRead(item.notificationId);
    }

    const route = getNotificationRoute(item.notificationType);

    setNotificationOpen(false);

    if (route) {
      navigate(route);
    }
  };

  const getNotificationIcon = (type) => {
    switch (type) {
      case "BOOKING_CONFIRMATION":
        return <LuCircleCheck className="notification-icon confirmed" />;

      case "BOOKING_CANCELLATION":
        return <LuCircleX className="notification-icon cancelled" />;

      case "BOOKING_RESCHEDULED":
        return <LuCalendarClock className="notification-icon rescheduled" />;

      case "BOOKING_REMINDER":
        return <LuClock className="notification-icon rescheduled" />;

      case "BOOKING_EXPIRED":
        return <LuClock className="notification-icon expired" />;

      case "CHARGING_STARTED":
        return <LuZap className="notification-icon confirmed" />;
      case "CHARGING_COMPLETED":
        return <LuZap className="notification-icon confirmed" />;

      case "PAYMENT_SUCCESS":
        return <LuCreditCard className="notification-icon confirmed" />;

      case "PAYMENT_FAILED":
        return <LuCreditCard className="notification-icon cancelled" />;

      case "WALLET_TOPUP_SUCCESS":
        return <LuCreditCard className="notification-icon confirmed" />;

      case "WALLET_TOPUP_FAILED":
        return <LuCreditCard className="notification-icon cancelled" />;

      default:
        return <LuBell className="notification-icon default" />;
    }
  };

  const protectedLinks = [
    ["/dashboard", "Dashboard"],
    ["/stations", "Stations"],
    ["/vehicles", "Vehicles"],
    ["/bookings", "Bookings"],
    ["/wallet", "Wallet"],
    ["/billing", "Billing"],
  ];

  return (
    <>
      <header className="site-header">
        <Link className="brand" to="/">
          <span className="brand-icon">⚡</span>
          <span>
            SMART <b>EV</b>
          </span>
        </Link>

        <button
          className="menu-toggle"
          onClick={() => setOpen((value) => !value)}
          aria-label="Menu"
        >
          ☰
        </button>

        <nav className={`site-nav ${open ? "open" : ""}`}>
          <Link
            className={location.pathname === "/" ? "active" : ""}
            to="/"
            onClick={() => setOpen(false)}
          >
            Home
          </Link>

          <Link
            className={location.pathname === "/stations" ? "active" : ""}
            to="/stations"
            onClick={() => setOpen(false)}
          >
            Stations
          </Link>

          {user &&
            protectedLinks
              .filter((item) => item[0] !== "/stations")
              .map(([to, label]) => (
                <Link
                  key={to}
                  className={location.pathname === to ? "active" : ""}
                  to={to}
                  onClick={() => setOpen(false)}
                >
                  {label}
                </Link>
              ))}

          {user &&
            ["ADMIN", "STATION_OWNER"].includes(
              String(user.role).toUpperCase(),
            ) && (
              <Link
                className={location.pathname === "/admin" ? "active" : ""}
                to="/admin"
                onClick={() => setOpen(false)}
              >
                Admin
              </Link>
            )}

          {user && (
            <div className="notification-wrapper" ref={notificationRef}>
              <button
                className="notification-bell"
                onClick={() => setNotificationOpen((value) => !value)}
                aria-label="Notifications"
                title="Notifications"
              >
                {/* <span className="bell-icon">🔔</span> */}

                <LuBell className="bell-icon" />

                {unreadCount > 0 && (
                  <span className="notification-count">
                    {unreadCount > 99 ? "99+" : unreadCount}
                  </span>
                )}
              </button>

              {notificationOpen && (
                <div className="notification-dropdown">
                  <div className="notification-header">
                    <div>
                      <h3>Notifications</h3>
                      <span>{unreadCount} unread</span>
                    </div>

                    {unreadCount > 0 && (
                      <button className="mark-all-read" onClick={markAllAsRead}>
                        Mark all read
                      </button>
                    )}

                    {notifications.length > 0 && (
                      <button
                        className="mark-all-read clear-all"
                        onClick={clearAllNotifications}
                      >
                        Clear all
                      </button>
                    )}

                    <button
                      className="notification-close"
                      onClick={() => setNotificationOpen(false)}
                      aria-label="Close notifications"
                      title="Close"
                    >
                      ✕
                    </button>
                  </div>

                  <div className="notification-list">
                    {notifications.length === 0 ? (
                      <div className="notification-empty">
                        <span>🔔</span>
                        <p>No notifications yet</p>
                      </div>
                    ) : (
                      notifications.map((item) => (
                        <div
                          key={item.notificationId}
                          className={`notification-item ${
                            item.isRead ? "read" : "unread"
                          }`}
                        >
                          <div
                            className="notification-item-content"
                            onClick={() => openNotification(item)}
                            style={{ cursor: "pointer" }}
                          >
                            <div className="notification-item-title">
                              <strong>{item.title}</strong>
                              {!item.isRead && (
                                <span className="notification-dot"></span>
                              )}
                            </div>

                            <p>{item.message}</p>

                            <small>{formatDate(item.createdAt)}</small>
                          </div>

                          {!item.isRead && (
                            <button
                              className="mark-read-button"
                              onClick={(event) => {
                                event.stopPropagation();
                                markAsRead(item.notificationId);
                              }}
                            >
                              ✓
                            </button>
                          )}

                          <button
                            className="mark-read-button delete-notification"
                            title="Delete"
                            onClick={(event) => {
                              event.stopPropagation();
                              removeNotification(item.notificationId);
                            }}
                          >
                            🗑
                          </button>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>
          )}

          {user ? (
            <>
              <Link
                className="welcome"
                to="/profile"
                onClick={() => setOpen(false)}
                title="My profile"
              >
                Hi, {user.fullName?.split(" ")[0] || "User"}
              </Link>

              <button className="btn btn-danger header-action" onClick={logout}>
                Logout
              </button>
            </>
          ) : (
            <>
              <Link
                className="btn btn-outline header-action"
                to="/login"
                onClick={() => setOpen(false)}
              >
                Login
              </Link>

              <Link
                className="btn btn-primary header-action"
                to="/register"
                onClick={() => setOpen(false)}
              >
                Register
              </Link>
            </>
          )}
        </nav>
      </header>

      {toast && (
        <div className="notification-toast">
          <div className="toast-icon">
            {getNotificationIcon(toast.notificationType)}
          </div>

          <div className="toast-content">
            <strong>{toast.title}</strong>
            <p>{toast.message}</p>
            <small>Just now</small>
          </div>

          <button
            className="toast-close"
            onClick={() => setToast(null)}
            aria-label="Close notification"
          >
            ×
          </button>
        </div>
      )}
    </>
  );
}
