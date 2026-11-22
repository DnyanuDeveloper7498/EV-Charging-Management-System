import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Api } from "../../services/api";
import { Session } from "../../services/session";
import PageHeader from "../../components/common/PageHeader";
import Message from "../../components/common/Message";
import { jsPDF } from "jspdf";
import "./Bookings.css";

export default function Bookings() {
  const user = Session.get();

  const [items, setItems] = useState([]);
  const [stations, setStations] = useState({});
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const [rescheduleId, setRescheduleId] = useState(null);
  const [newBookingTime, setNewBookingTime] = useState("");

  const [selectedFilter, setSelectedFilter] = useState("ALL");

  const [searchBookingId, setSearchBookingId] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const [selectedBooking, setSelectedBooking] = useState(null);

  const [cancelBookingId, setCancelBookingId] = useState(null);
  const [cancellationReason, setCancellationReason] = useState("");

  // Load bookings and stations
  const load = async () => {
    try {
      const [stationData, bookingData] = await Promise.all([
        Api.stations(),
        Api.bookings(user.userId),
      ]);

      const map = {};

      stationData.forEach((station) => {
        map[station.stationId] = station;
      });

      setStations(map);

      const sortedBookings = [...bookingData].sort(
        (a, c) => new Date(c.bookingTime) - new Date(a.bookingTime),
      );

      setItems(sortedBookings);
    } catch (e) {
      setError(e.message);
    }
  };

  useEffect(() => {
    load();
  }, []);

  // Open cancellation popup
  const cancel = (id) => {
    setCancelBookingId(id);
    setCancellationReason("");
  };

  // Confirm cancellation
  const confirmCancel = async () => {
    if (!cancelBookingId) return;

    setLoading(true);
    setError("");

    try {
      await Api.cancelBooking(cancelBookingId, cancellationReason.trim());

      setCancelBookingId(null);
      setCancellationReason("");

      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  // Reschedule booking
  const reschedule = async (id) => {
    if (!newBookingTime) {
      setError("Please select a new booking date and time.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      await Api.rescheduleBooking(id, newBookingTime);

      setRescheduleId(null);
      setNewBookingTime("");

      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  // Status badge
  const badge = (s) => {
    s = (s || "").toUpperCase();

    return s === "CANCELLED" || s === "EXPIRED"
      ? "red"
      : s === "COMPLETED"
        ? "gray"
        : "";
  };

  // Current local date and time
  const getLocalDateTime = () => {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());

    return now.toISOString().slice(0, 16);
  };

  // Filter bookings
  const filteredItems = items.filter((b) => {
    const status = (b.bookingStatus || "").toUpperCase();
    const bookingDate = new Date(b.bookingTime);

    const isUpcoming =
      (status === "CONFIRMED" || status === "BOOKED") &&
      bookingDate > new Date();

    let matchesStatus = true;

    switch (selectedFilter) {
      case "UPCOMING":
        matchesStatus = isUpcoming;
        break;

      case "COMPLETED":
        matchesStatus = status === "COMPLETED";
        break;

      case "CANCELLED":
        matchesStatus = status === "CANCELLED";
        break;

      case "EXPIRED":
        matchesStatus = status === "EXPIRED";
        break;

      default:
        matchesStatus = true;
    }

    const matchesBookingId = String(b.bookingId).includes(
      searchBookingId.trim(),
    );

    const matchesStartDate =
      !startDate || bookingDate >= new Date(`${startDate}T00:00:00`);

    const matchesEndDate =
      !endDate || bookingDate <= new Date(`${endDate}T23:59:59.999`);

    return (
      matchesStatus && matchesBookingId && matchesStartDate && matchesEndDate
    );
  });

  // Download booking receipt
  const downloadReceipt = (booking) => {
    const doc = new jsPDF();
    const station = stations[booking.stationId];

    const bookingDate = new Date(booking.bookingTime).toLocaleString();

    doc.setFontSize(22);
    doc.setFont("helvetica", "bold");
    doc.text("EV CHARGING SYSTEM", 20, 25);

    doc.setFontSize(16);
    doc.text("BOOKING RECEIPT", 20, 40);

    doc.setFontSize(11);
    doc.setFont("helvetica", "normal");

    doc.text(`Receipt Date: ${new Date().toLocaleDateString()}`, 20, 55);

    doc.line(20, 62, 190, 62);

    doc.setFont("helvetica", "bold");
    doc.text("Booking Information", 20, 75);

    doc.setFont("helvetica", "normal");
    doc.text(`Booking ID: #${booking.bookingId}`, 20, 90);
    doc.text(`Vehicle ID: #${booking.vehicleId}`, 20, 102);
    doc.text(`Station: ${station?.stationName || "Not available"}`, 20, 114);
    doc.text(`Location: ${station?.sLocation || "Not available"}`, 20, 126);
    doc.text(`Booking Date: ${bookingDate}`, 20, 138);
    doc.text(`Status: ${booking.bookingStatus}`, 20, 150);

    doc.line(20, 162, 190, 162);

    doc.setFontSize(10);
    doc.text("Thank you for choosing our EV Charging System.", 20, 178);

    doc.save(`Booking_Receipt_${booking.bookingId}.pdf`);
  };

  return (
    <div className="container">
      <PageHeader
        icon="📅"
        title="My Bookings"
        subtitle="View and manage your charging reservations."
      />

      <Message text={error} />

      {/* Booking Status Filters */}
      <div className="booking-filters">
        {[
          { label: "All", value: "ALL" },
          { label: "Upcoming", value: "UPCOMING" },
          { label: "Completed", value: "COMPLETED" },
          { label: "Cancelled", value: "CANCELLED" },
          { label: "Expired", value: "EXPIRED" },
        ].map((filter) => (
          <button
            key={filter.value}
            className={`filter-btn ${
              selectedFilter === filter.value ? "active" : ""
            }`}
            onClick={() => setSelectedFilter(filter.value)}
          >
            {filter.label}
          </button>
        ))}
      </div>

      {/* Booking Search */}
      <div className="booking-search-panel">
        <div className="booking-search-field">
          <label>Search Booking ID</label>
          <input
            type="text"
            placeholder="Enter Booking ID..."
            value={searchBookingId}
            onChange={(e) => setSearchBookingId(e.target.value)}
          />
        </div>

        <div className="booking-search-field">
          <label>Start Date</label>
          <input
            type="date"
            value={startDate}
            max={endDate || undefined}
            onChange={(e) => setStartDate(e.target.value)}
          />
        </div>

        <div className="booking-search-field">
          <label>End Date</label>
          <input
            type="date"
            value={endDate}
            min={startDate || undefined}
            onChange={(e) => setEndDate(e.target.value)}
          />
        </div>

        <button
          className="btn btn-primary"
          onClick={() => {
            setSearchBookingId("");
            setStartDate("");
            setEndDate("");
            setSelectedFilter("ALL");
          }}
        >
          Clear Filters
        </button>
      </div>

      {/* Booking List */}
      {!filteredItems.length ? (
        <div className="empty-state">
          {items.length === 0
            ? "No bookings yet."
            : "No bookings found for this filter."}
          <br />
          <Link className="text-link" to="/stations">
            Book a charging slot
          </Link>
          .
        </div>
      ) : (
        <div className="booking-list">
          {filteredItems.map((b) => {
            const st = stations[b.stationId];
            const status = (b.bookingStatus || "").toUpperCase();

            return (
              <div className="booking-card" key={b.bookingId}>
                <div className="booking-main">
                  <div>
                    <h3>{st?.stationName || `Station #${b.stationId}`}</h3>

                    <p>📍 {st?.sLocation || "Location unavailable"}</p>

                    <p>🕒 {new Date(b.bookingTime).toLocaleString()}</p>
                  </div>

                  <span className={`badge ${badge(status)}`}>
                    {b.bookingStatus}
                  </span>
                </div>

                <div className="booking-details">
                  <span>
                    Booking ID <b>#{b.bookingId}</b>
                  </span>

                  <span>
                    Vehicle ID <b>#{b.vehicleId}</b>
                  </span>
                </div>

                <div className="booking-actions">
                  {(status === "BOOKED" || status === "CONFIRMED") && (
                    <Link
                      className="btn btn-primary"
                      to={`/session?bookingId=${b.bookingId}`}
                    >
                      ⚡ Start Charging
                    </Link>
                  )}

                  {/* View Details is available for every booking */}
                  <button
                    className="btn btn-primary"
                    onClick={() => setSelectedBooking(b)}
                  >
                    View Details
                  </button>

                  {status === "IN_PROGRESS" && (
                    <Link
                      className="btn btn-danger"
                      to={`/session?bookingId=${b.bookingId}`}
                    >
                      ⏹ Stop Charging
                    </Link>
                  )}

                  {status === "CONFIRMED" && (
                    <>
                      <button
                        className="btn btn-primary"
                        disabled={loading}
                        onClick={() => {
                          setRescheduleId(
                            rescheduleId === b.bookingId ? null : b.bookingId,
                          );
                          setNewBookingTime("");
                          setError("");
                        }}
                      >
                        Reschedule
                      </button>

                      <button
                        className="btn btn-primary"
                        onClick={() => downloadReceipt(b)}
                      >
                        Download Receipt
                      </button>

                      <button
                        className="btn btn-danger"
                        disabled={loading}
                        onClick={() => cancel(b.bookingId)}
                      >
                        {loading ? "Cancelling..." : "Cancel"}
                      </button>
                    </>
                  )}
                </div>

                {/* Reschedule Form */}
                {rescheduleId === b.bookingId && status === "CONFIRMED" && (
                  <div className="reschedule-form">
                    <label>Choose New Booking Date & Time</label>

                    <input
                      type="datetime-local"
                      value={newBookingTime}
                      min={getLocalDateTime()}
                      onChange={(e) => setNewBookingTime(e.target.value)}
                    />

                    <button
                      className="btn btn-primary"
                      disabled={loading || !newBookingTime}
                      onClick={() => reschedule(b.bookingId)}
                    >
                      {loading ? "Updating..." : "Confirm Reschedule"}
                    </button>

                    <button
                      className="btn"
                      disabled={loading}
                      onClick={() => {
                        setRescheduleId(null);
                        setNewBookingTime("");
                      }}
                    >
                      Close
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Booking Details Modal */}
      {selectedBooking && (
        <div
          className="booking-modal-overlay"
          onClick={() => setSelectedBooking(null)}
        >
          <div className="booking-modal" onClick={(e) => e.stopPropagation()}>
            <div className="booking-modal-header">
              <h2>Booking Details</h2>

              <button
                className="modal-close-btn"
                onClick={() => setSelectedBooking(null)}
              >
                ×
              </button>
            </div>

            <div className="booking-modal-body">
              <div className="detail-row">
                <span>Booking ID</span>
                <strong>#{selectedBooking.bookingId}</strong>
              </div>

              <div className="detail-row">
                <span>Station Name</span>
                <strong>
                  {stations[selectedBooking.stationId]?.stationName ||
                    `Station #${selectedBooking.stationId}`}
                </strong>
              </div>

              <div className="detail-row">
                <span>Location</span>
                <strong>
                  {stations[selectedBooking.stationId]?.sLocation ||
                    "Not available"}
                </strong>
              </div>

              <div className="detail-row">
                <span>Vehicle ID</span>
                <strong>#{selectedBooking.vehicleId}</strong>
              </div>

              <div className="detail-row">
                <span>Booking Date & Time</span>
                <strong>
                  {new Date(selectedBooking.bookingTime).toLocaleString()}
                </strong>
              </div>

              <div className="detail-row">
                <span>Booking Status</span>
                <strong>{selectedBooking.bookingStatus}</strong>
              </div>

              {/* Cancellation details are INSIDE booking-modal-body */}
              {(selectedBooking.bookingStatus || "").toUpperCase() ===
                "CANCELLED" && (
                <>
                  <div className="detail-row">
                    <span>Cancelled At</span>
                    <strong>
                      {selectedBooking.cancelledAt
                        ? new Date(selectedBooking.cancelledAt).toLocaleString()
                        : "Not available"}
                    </strong>
                  </div>

                  <div className="detail-row">
                    <span>Cancellation Type</span>
                    <strong>
                      {selectedBooking.cancellationType || "Not available"}
                    </strong>
                  </div>

                  <div className="detail-row">
                    <span>Cancellation Reason</span>
                    <strong>
                      {selectedBooking.cancellationReason || "Not provided"}
                    </strong>
                  </div>

                  <div className="detail-row">
                    <span>Refund Status</span>
                    <strong>
                      {selectedBooking.refundStatus || "NOT_APPLICABLE"}
                    </strong>
                  </div>
                </>
              )}
            </div>

            <button
              className="btn btn-primary modal-done-btn"
              onClick={() => setSelectedBooking(null)}
            >
              Close
            </button>
          </div>
        </div>
      )}

      {/* Cancellation Confirmation Modal */}
      {cancelBookingId && (
        <div
          className="booking-modal-overlay"
          onClick={() => {
            if (!loading) setCancelBookingId(null);
          }}
        >
          <div
            className="booking-modal cancellation-modal"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="booking-modal-header">
              <h2>Cancel Booking</h2>

              <button
                className="modal-close-btn"
                disabled={loading}
                onClick={() => setCancelBookingId(null)}
              >
                ×
              </button>
            </div>

            {(() => {
              const booking = items.find(
                (b) => b.bookingId === cancelBookingId,
              );

              const hoursLeft = booking
                ? (new Date(booking.bookingTime) - new Date()) /
                  (1000 * 60 * 60)
                : 0;

              const isFreeCancellation = hoursLeft > 2;

              return (
                <>
                  <p className="cancellation-warning">
                    Are you sure you want to cancel booking #{cancelBookingId}?
                  </p>

                  <div className="cancellation-policy">
                    <strong>
                      {isFreeCancellation
                        ? "Free Cancellation"
                        : "Late Cancellation"}
                    </strong>

                    <p>
                      {isFreeCancellation
                        ? "You are cancelling more than 2 hours before your booking."
                        : "You are cancelling within 2 hours of your booking."}
                    </p>
                  </div>

                  <div className="cancellation-reason">
                    <label>Reason for Cancellation (Optional)</label>

                    <select
                      value={cancellationReason}
                      onChange={(e) => setCancellationReason(e.target.value)}
                      disabled={loading}
                    >
                      <option value="">Select a reason</option>
                      <option value="Change of plans">Change of plans</option>
                      <option value="Wrong booking">Wrong booking</option>
                      <option value="Vehicle unavailable">
                        Vehicle unavailable
                      </option>
                      <option value="Station issue">Station issue</option>
                      <option value="Other">Other</option>
                    </select>
                  </div>

                  <div className="cancellation-actions">
                    <button
                      className="btn"
                      disabled={loading}
                      onClick={() => setCancelBookingId(null)}
                    >
                      Keep Booking
                    </button>

                    <button
                      className="btn btn-danger"
                      disabled={loading}
                      onClick={confirmCancel}
                    >
                      {loading ? "Cancelling..." : "Confirm Cancellation"}
                    </button>
                  </div>
                </>
              );
            })()}
          </div>
        </div>
      )}
    </div>
  );
}
