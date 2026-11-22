import React, { useState, useEffect } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { loadRazorpay } from "../../services/razorpay";
import { Api } from "../../services/api";
import PageHeader from "../../components/common/PageHeader";
import Message from "../../components/common/Message";
import "./ChargingSession.css";

export default function ChargingSession() {
  const nav = useNavigate();
  const [params] = useSearchParams();
  const bookingId = params.get("bookingId");

  // const [phase, setPhase] = useState(bookingId ? "start" : "missing");

  const [phase, setPhase] = useState(bookingId ? "loading" : "missing");

  const [session, setSession] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const [showStopModal, setShowStopModal] = useState(false);
  const [endBattery, setEndBattery] = useState("");

  // Restore state on reload / when coming back to an already running session
  useEffect(() => {
    if (!bookingId) return;

    let cancelled = false;

    (async () => {
      try {
        const s = await Api.sessionByBooking(Number(bookingId));
        if (cancelled) return;

        setSession(s);
        setPhase(s.endTime ? "done" : "active");
      } catch (e) {
        // no session yet for this booking, so it is ready to start
        if (!cancelled) setPhase("start");
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [bookingId]);

  const start = async () => {
    setLoading(true);
    setError("");

    try {
      const s = await Api.startCharging(Number(bookingId));
      setSession(s);
      setPhase("active");
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  const openStopModal = () => {
    setError("");
    setEndBattery("");
    setShowStopModal(true);
  };

  const closeStopModal = () => {
    if (loading) return;

    setShowStopModal(false);
    setEndBattery("");
  };

  const stop = async () => {
    if (endBattery === "") {
      setError("Please enter end battery percentage.");
      return;
    }

    const battery = Number(endBattery);

    if (Number.isNaN(battery) || battery < 0 || battery > 100) {
      setError("Battery percentage must be between 0 and 100.");
      return;
    }

    if (battery < Number(session.startBatteryPercentage)) {
      setError("End battery cannot be lower than start battery.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const s = await Api.stopCharging(session.sessionId, battery);

      setSession(s);
      setShowStopModal(false);
      setEndBattery("");
      setPhase("done");
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  // One bill per session. It can be paid from the wallet or online; either way the backend
  // updates the same payment record, and we continue to the Billing page (no page reload).
  const payFromWallet = async () => {
    setLoading(true);
    setError("");

    try {
      await Api.payWithWallet(session.sessionId);
      nav("/billing");
    } catch (e) {
      setError(e.message || "Wallet payment failed.");
      setLoading(false);
    }
  };

  const payNow = async () => {
    setLoading(true);
    setError("");

    try {
      const order = await Api.createRazorpayOrder(session.sessionId);
      const Razorpay = await loadRazorpay();

      const options = {
        method: { upi: true, card: true, netbanking: true, wallet: true },
        config: {
          display: {
            sequence: ["block.upi", "block.other"],
            blocks: {
              upi: {
                name: "Pay using UPI",
                instruments: [{ method: "upi" }],
              },
              other: {
                name: "Other payment methods",
                instruments: [
                  { method: "card" },
                  { method: "netbanking" },
                  { method: "wallet" },
                ],
              },
            },
            preferences: { show_default_blocks: false },
          },
        },

        key: order.keyId,
        amount: order.amount,
        currency: order.currency,
        name: "EV Charging System",
        description: "Charging Session Payment",
        order_id: order.orderId,

        handler: async (response) => {
          try {
            const result = await Api.verifyRazorpayPayment(
              session.sessionId,
              response,
            );

            if (result.paymentStatus !== "SUCCESS") {
              throw new Error("Payment verification is not completed.");
            }

            nav("/billing");
          } catch (err) {
            setError(err.message || "Payment verification failed.");
            setLoading(false);
          }
        },

        modal: { ondismiss: () => setLoading(false) },
      };

      const checkout = new Razorpay(options);

      checkout.on("payment.failed", (response) => {
        const reason = response.error?.description || "Payment failed.";
        setError(reason);
        setLoading(false);
        // persist the failure so the bill shows FAILED and can be retried
        Api.paymentFailed(session.sessionId, reason).catch(() => {});
      });

      checkout.open();
    } catch (e) {
      setError(e.message || "Could not start the payment.");
      setLoading(false);
    }
  };

  return (
    <div className="container session-page">
      <PageHeader
        icon="🔌"
        title="Charging Session"
        subtitle="Monitor your active EV charging session."
        back={false}
      />

      <Message text={error} />

      {phase === "loading" && (
        <div className="card session-card">
          <p className="card-desc">Loading session...</p>
        </div>
      )}

      {phase === "missing" && (
        <div className="card session-card">
          <h3>Booking required</h3>

          <p className="card-desc">
            Open this page from My Bookings by clicking Start Charging.
          </p>

          <Link className="btn btn-primary" to="/bookings">
            Go to My Bookings
          </Link>
        </div>
      )}

      {phase === "start" && (
        <div className="card session-card">
          <div className="session-icon">⚡</div>

          <h3>Booking #{bookingId} is ready to charge.</h3>

          <p className="card-desc">
            Start the charging session when your vehicle is connected.
          </p>

          <button
            className="btn btn-primary btn-block"
            onClick={start}
            disabled={loading}
          >
            {loading ? "Starting..." : "⚡ Start Charging"}
          </button>
        </div>
      )}

      {phase === "active" && (
        <div className="card session-card">
          <div className="charging-panel">
            <span className="charging-status">● CHARGING</span>

            <p>Your vehicle is currently being charged.</p>
          </div>

          <div className="session-info">
            <div>
              <span>Booking ID</span>
              <b>#{session.bookingId}</b>
            </div>

            <div>
              <span>Session ID</span>
              <b>#{session.sessionId}</b>
            </div>

            <div>
              <span>Start Battery</span>
              <b>{session.startBatteryPercentage}%</b>
            </div>

            <div>
              <span>Started At</span>
              <b>{new Date(session.startTime).toLocaleString()}</b>
            </div>
          </div>

          <p className="card-desc">
            When charging is complete, stop the session and enter the current
            battery percentage.
          </p>

          <button
            className="btn btn-danger btn-block"
            onClick={openStopModal}
            disabled={loading}
          >
            ⏹ Stop Charging
          </button>
        </div>
      )}

      {phase === "done" && (
        <div className="card session-card">
          <div className="complete-icon">✓</div>

          <h3>Charging Complete</h3>

          <div className="result-grid">
            <div>
              <span>Units Consumed</span>
              <b>{Number(session.unitsConsumed).toFixed(2)} kWh</b>
            </div>

            <div>
              <span>Total Cost</span>
              <b>₹{Number(session.totalCost).toFixed(2)}</b>
            </div>

            <div>
              <span>End Battery</span>
              <b>{session.endBatteryPercentage}%</b>
            </div>
          </div>

          <div className="result-actions">
            <Link className="btn btn-outline" to="/wallet">
              View Wallet
            </Link>

            <button
              className="btn btn-primary btn-block"
              onClick={payNow}
              disabled={loading}
            >
              {loading ? "Processing Payment..." : "Pay Now"}
            </button>

            <button
              className="btn btn-outline"
              onClick={payFromWallet}
              disabled={loading}
            >
              Pay from Wallet
            </button>

            <Link className="btn btn-primary" to="/dashboard">
              Dashboard
            </Link>
          </div>
        </div>
      )}

      {showStopModal && (
        <div className="charging-modal-overlay">
          <div className="charging-modal">
            <div className="charging-modal-header">
              <div>
                <h3>Stop Charging</h3>
                <p>Enter your current battery percentage.</p>
              </div>

              <button
                className="modal-close"
                onClick={closeStopModal}
                disabled={loading}
              >
                ×
              </button>
            </div>

            <div className="charging-modal-body">
              <div className="battery-summary">
                <div>
                  <span>Start Battery</span>
                  <strong>{session.startBatteryPercentage}%</strong>
                </div>

                <div>
                  <span>Current Battery</span>
                  <strong>{endBattery === "" ? "--" : `${endBattery}%`}</strong>
                </div>
              </div>

              <label htmlFor="endBattery">End Battery Percentage</label>

              <input
                id="endBattery"
                type="number"
                min="0"
                max="100"
                placeholder="Enter battery percentage"
                value={endBattery}
                onChange={(e) => setEndBattery(e.target.value)}
                disabled={loading}
                autoFocus
              />

              <small>
                Battery must be between 0% and 100% and cannot be lower than the
                starting battery.
              </small>
            </div>

            <div className="charging-modal-actions">
              <button
                className="btn btn-outline"
                onClick={closeStopModal}
                disabled={loading}
              >
                Cancel
              </button>

              <button
                className="btn btn-danger"
                onClick={stop}
                disabled={loading}
              >
                {loading ? "Processing..." : "Confirm Stop"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
