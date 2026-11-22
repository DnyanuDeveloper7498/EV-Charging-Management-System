import React, { useEffect, useMemo, useState } from "react";

import { Api } from "../../services/api";
import { Session } from "../../services/session";

import PageHeader from "../../components/common/PageHeader";
import Message from "../../components/common/Message";
import Modal from "../../components/common/Modal";
import StarRating from "../../components/common/StarRating";

import StationMap from "../StationMap/StationMap";

import { useNavigate } from "react-router-dom";

import "./Stations.css";

const PAGE_SIZE = 4;

const isBookable = (station) =>
  ["", "available", "active"].includes((station.status || "").toLowerCase());

function distanceKm(lat1, lon1, lat2, lon2) {
  const rad = (degree) => (degree * Math.PI) / 180;

  const a =
    Math.sin(rad(lat2 - lat1) / 2) ** 2 +
    Math.cos(rad(lat1)) *
      Math.cos(rad(lat2)) *
      Math.sin(rad(lon2 - lon1) / 2) ** 2;

  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export default function Stations() {
  const user = Session.get();

  const [stations, setStations] = useState([]);
  const [ratings, setRatings] = useState({});
  const [vehicles, setVehicles] = useState([]);

  const navigate = useNavigate();

  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const [activeStationId, setActiveStationId] = useState(null);

  // Booking
  const [selected, setSelected] = useState(null);
  const [vehicleId, setVehicleId] = useState("");
  const [time, setTime] = useState("");
  const [loading, setLoading] = useState(false);

  // Reviews
  const [reviewStation, setReviewStation] = useState(null);

  // Filters
  const [search, setSearch] = useState("");
  const [charger, setCharger] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [onlyFree, setOnlyFree] = useState(false);
  const [sort, setSort] = useState("default");
  const [here, setHere] = useState(null);

  // Pagination
  const [currentPage, setCurrentPage] = useState(1);

  const load = async () => {
    const stationData = await Api.stations();
    setStations(stationData);

    try {
      const list = await Api.ratings();

      setRatings(Object.fromEntries(list.map((r) => [r.stationId, r])));
    } catch {
      // Ratings are optional.
    }
  };

  

 useEffect(() => {
  (async () => {
    try {
      await load();

      if (user) {
        const vehicleData = await Api.vehicles(user.userId);
        setVehicles(vehicleData);

        if (vehicleData.length) {
          setVehicleId(String(vehicleData[0].vehicleId));
        }
      } else {
        setVehicles([]);
      }
    } catch (e) {
      setError(e.message);
    } finally {
      setLoaded(true);
    }
  })();

  // eslint-disable-next-line react-hooks/exhaustive-deps
}, [user?.userId]);



  const chargerTypes = useMemo(
    () =>
      [...new Set(stations.map((s) => s.chargerType).filter(Boolean))].sort(),
    [stations],
  );

  const distance = (station) =>
    here && station.latitude != null && station.longitude != null
      ? distanceKm(
          here.lat,
          here.lng,
          Number(station.latitude),
          Number(station.longitude),
        )
      : null;

  const visible = useMemo(() => {
    const q = search.trim().toLowerCase();

    let list = stations.filter((s) => {
      if (q && !`${s.stationName} ${s.sLocation}`.toLowerCase().includes(q))
        return false;

      if (charger && s.chargerType !== charger) return false;

      if (maxPrice !== "" && Number(s.pricePerKwt) > Number(maxPrice))
        return false;

      if (onlyFree && (s.availableSlots <= 0 || !isBookable(s))) return false;

      return true;
    });

    const dist = (s) => distance(s) ?? Infinity;

    if (sort === "price") {
      list = [...list].sort(
        (a, b) => Number(a.pricePerKwt) - Number(b.pricePerKwt),
      );
    }

    if (sort === "rating") {
      list = [...list].sort(
        (a, b) =>
          (ratings[b.stationId]?.average || 0) -
          (ratings[a.stationId]?.average || 0),
      );
    }

    if (sort === "nearest") {
      list = [...list].sort((a, b) => dist(a) - dist(b));
    }

    return list;
  }, [stations, search, charger, maxPrice, onlyFree, sort, ratings, here]);

  // Reset to first page when filters or sorting change.
  useEffect(() => {
    setCurrentPage(1);
  }, [search, charger, maxPrice, onlyFree, sort]);

  const pageCount = Math.max(1, Math.ceil(visible.length / PAGE_SIZE));

  const safePage = Math.min(currentPage, pageCount);

  const pageStations = visible.slice(
    (safePage - 1) * PAGE_SIZE,
    safePage * PAGE_SIZE,
  );

  const locate = () => {
    setError("");

    if (!navigator.geolocation) {
      setError("Your browser does not support location.");
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setHere({
          lat: pos.coords.latitude,
          lng: pos.coords.longitude,
        });

        setSort("nearest");
      },
      () =>
        setError("Could not get your location. Please allow location access."),
    );
  };

  const clearFilters = () => {
    setSearch("");
    setCharger("");
    setMaxPrice("");
    setOnlyFree(false);
    setSort("default");
    setCurrentPage(1);
  };

  const handleMapSelect = (station) => {
    setActiveStationId(station.stationId);

    const index = visible.findIndex((s) => s.stationId === station.stationId);

    if (index >= 0) {
      setCurrentPage(Math.floor(index / PAGE_SIZE) + 1);
    }
  };

  const openBooking = (station) => {
    setError("");

    if (!user) {
      navigate("/login");
      return;
    }

    setSelected(station);
  };

  const book = async () => {
    if (!vehicles.length) {
      return setError("Please add a vehicle first.");
    }

    if (!vehicleId) {
      return setError("Please select a vehicle.");
    }

    if (!time) {
      return setError("Please choose a booking time.");
    }

    setLoading(true);
    setError("");

    try {
      await Api.createBooking({
        vehicleId: Number(vehicleId),
        stationId: selected.stationId,
        bookingTime: time.length === 16 ? time + ":00" : time,
      });

      setSelected(null);
      setTime("");

      await load();

      setNotice(
        "Booking created successfully. A confirmation email is on its way.",
      );
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  const filtersActive =
    search || charger || maxPrice !== "" || onlyFree || sort !== "default";

  return (
    <div className="stations-page container">
      <PageHeader
        icon="📍"
        title="Charging Stations"
        subtitle="Discover nearby EV charging stations and book your slot."
      />

      <section className="stations-hero">
        <div className="stations-hero-content">
          <span className="hero-label">SMART EV CHARGING NETWORK</span>

          <h2>
            Find your next
            <br />
            <span>charging destination.</span>
          </h2>

          <p>
            Explore charging stations, check availability and power your
            journey.
          </p>

          <button className="hero-location-btn" onClick={locate}>
            <span>⌖</span> Find Stations Near Me
          </button>
        </div>

        <div className="stations-hero-image" />
        <div className="hero-glow" />
      </section>

      <Message text={error} />
      <Message text={notice} type="success" />

      {loaded && stations.length === 0 ? (
        <div className="stations-empty">
          <span>⚡</span>
          <h3>No charging stations available</h3>
          <p>Please check again later.</p>
        </div>
      ) : (
        <>
          <section className="stations-filter-panel">
            <div className="filter-heading">
              <div>
                <span className="section-eyebrow">EXPLORE NETWORK</span>
                <h2>Find Charging Stations</h2>
              </div>

              <span className="station-total">
                {visible.length} Stations Found
              </span>
            </div>

            <div className="stations-filters">
              <div className="station-search">
                <span>⌕</span>
                <input
                  placeholder="Search station or location..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
              </div>

              <select
                value={charger}
                onChange={(e) => setCharger(e.target.value)}
              >
                <option value="">All Charger Types</option>
                {chargerTypes.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>

              <input
                type="number"
                min="0"
                placeholder="Max price ₹/kWh"
                value={maxPrice}
                onChange={(e) => setMaxPrice(e.target.value)}
              />

              <select value={sort} onChange={(e) => setSort(e.target.value)}>
                <option value="default">Sort: Default</option>
                <option value="price">Price: Low to High</option>
                <option value="rating">Top Rated</option>
                <option value="nearest" disabled={!here}>
                  Nearest to Me
                </option>
              </select>

              <label className="available-filter">
                <input
                  type="checkbox"
                  checked={onlyFree}
                  onChange={(e) => setOnlyFree(e.target.checked)}
                />
                Available only
              </label>

              {filtersActive && (
                <button className="clear-filter-btn" onClick={clearFilters}>
                  Clear Filters
                </button>
              )}
            </div>
          </section>

          <div className="stations-content-heading">
            <div>
              <span className="section-eyebrow">CHARGING NETWORK</span>
              <h2>Available Stations</h2>
              <p>
                Showing {visible.length} of {stations.length} stations
              </p>
            </div>

            <div className="station-page-indicator">
              {visible.length > 0
                ? `${(safePage - 1) * PAGE_SIZE + 1}–${Math.min(
                    safePage * PAGE_SIZE,
                    visible.length,
                  )} of ${visible.length}`
                : "0 stations"}
            </div>
          </div>

          <div className="stations-split-layout">
            {/* LEFT SIDE - STATION CARDS */}
            <div className="station-list-column">
              {!visible.length ? (
                <div className="stations-empty">
                  <span>⌕</span>
                  <h3>No matching stations</h3>
                  <p>Try changing your filters.</p>

                  <button
                    className="station-primary-btn"
                    onClick={clearFilters}
                  >
                    Clear Filters
                  </button>
                </div>
              ) : (
                <>
                  <div className="station-grid">
                    {pageStations.map((st) => {
                      const full = st.availableSlots <= 0;
                      const offline = !isBookable(st);
                      const rating = ratings[st.stationId];
                      const km = distance(st);

                      const isSelected = activeStationId === st.stationId;

                      return (
                        <article
                          className={`station-card ${
                            isSelected ? "station-card-selected" : ""
                          }`}
                          key={st.stationId}
                          onClick={() => setActiveStationId(st.stationId)}
                        >
                          <div className="station-card-top">
                            <div className="station-icon-box">
                              <span>⚡</span>
                            </div>

                            <span
                              className={`station-status ${
                                full || offline ? "status-offline" : ""
                              }`}
                            >
                              <i />
                              {full
                                ? "Full"
                                : offline
                                  ? "Unavailable"
                                  : "Available"}
                            </span>
                          </div>

                          <h3>{st.stationName}</h3>

                          <p className="station-location">
                            <span>⌖</span>
                            {st.sLocation}
                          </p>

                          {km !== null && (
                            <span className="station-distance">
                              {km.toFixed(1)} km away
                            </span>
                          )}

                          <div className="station-divider" />

                          <div className="station-details">
                            <div>
                              <span className="detail-icon">🔌</span>
                              <div>
                                <small>Charger Type</small>
                                <strong>{st.chargerType || "N/A"}</strong>
                              </div>
                            </div>

                            <div>
                              <span className="detail-icon">🅿️</span>
                              <div>
                                <small>Available Slots</small>
                                <strong>
                                  {st.availableSlots} / {st.totalSlots}
                                </strong>
                              </div>
                            </div>
                          </div>

                          <div className="station-price-rating">
                            <div className="station-price">
                              <small>Charging Price</small>
                              <strong>
                                ₹{st.pricePerKwt}
                                <span>/kWh</span>
                              </strong>
                            </div>

                            <div className="station-rating">
                              {rating ? (
                                <>
                                  <StarRating
                                    value={rating.average}
                                    size={14}
                                  />
                                  <strong>
                                    {Number(rating.average).toFixed(1)}
                                  </strong>
                                  <small>({rating.count})</small>
                                </>
                              ) : (
                                <small>No reviews yet</small>
                              )}
                            </div>
                          </div>

                          <div className="station-card-actions">
                            <button
                              className="station-primary-btn"
                              disabled={full || offline}
                              onClick={(e) => {
                                e.stopPropagation();
                                openBooking(st);
                              }}
                            >
                              {full
                                ? "Fully Booked"
                                : offline
                                  ? "Unavailable"
                                  : "Book Charging Slot"}
                            </button>

                            <button
                              className="station-review-btn"
                              onClick={(e) => {
                                e.stopPropagation();
                                setReviewStation(st);
                              }}
                            >
                              ★
                            </button>
                          </div>
                        </article>
                      );
                    })}
                  </div>

                  {/* PREVIOUS / NEXT */}
                  {pageCount > 1 && (
                    <div className="station-pagination">
                      <button
                        onClick={() =>
                          setCurrentPage((p) => Math.max(1, p - 1))
                        }
                        disabled={safePage === 1}
                      >
                        ‹ Previous
                      </button>

                      <div className="pagination-pages">
                        {Array.from({ length: pageCount }, (_, i) => i + 1).map(
                          (page) => (
                            <button
                              key={page}
                              className={safePage === page ? "active-page" : ""}
                              onClick={() => setCurrentPage(page)}
                            >
                              {page}
                            </button>
                          ),
                        )}
                      </div>

                      <button
                        className="next-page-btn"
                        onClick={() =>
                          setCurrentPage((p) => Math.min(pageCount, p + 1))
                        }
                        disabled={safePage === pageCount}
                      >
                        Next ›
                      </button>
                    </div>
                  )}
                </>
              )}
            </div>

            {/* RIGHT SIDE - MAP */}
            <aside className="station-map-column">
              <div className="station-map-sticky">
                <StationMap
                  stations={visible}
                  activeStationId={activeStationId}
                  onSelectStation={handleMapSelect}
                />
              </div>
            </aside>
          </div>
        </>
      )}

      {/* BOOKING MODAL */}
      <Modal
        open={!!selected}
        onClose={() => setSelected(null)}
        title={selected ? `Book — ${selected.stationName}` : "Book Slot"}
      >
        {selected && (
          <>
            <p className="modal-location">📍 {selected.sLocation}</p>

            <div className="input-group">
              <label className="label">Vehicle</label>

              <select
                className="input"
                value={vehicleId}
                onChange={(e) => setVehicleId(e.target.value)}
              >
                {vehicles.length ? (
                  vehicles.map((v) => (
                    <option key={v.vehicleId} value={v.vehicleId}>
                      {v.brand} {v.model} — {v.vehicleNumber}
                    </option>
                  ))
                ) : (
                  <option value="">No vehicles</option>
                )}
              </select>
            </div>

            <div className="input-group">
              <label className="label">Booking Date & Time</label>

              <input
                className="input"
                type="datetime-local"
                min={new Date().toISOString().slice(0, 16)}
                value={time}
                onChange={(e) => setTime(e.target.value)}
              />
            </div>

            <div className="modal-actions">
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => setSelected(null)}
              >
                Cancel
              </button>

              <button
                type="button"
                className="btn btn-primary"
                disabled={loading || !vehicles.length}
                onClick={book}
              >
                {loading ? "Booking..." : "Confirm Booking"}
              </button>
            </div>
          </>
        )}
      </Modal>

      <ReviewsModal
        station={reviewStation}
        onClose={() => setReviewStation(null)}
        onSaved={load}
      />
    </div>
  );
}

// REVIEWS MODAL
function ReviewsModal({ station, onClose, onSaved }) {
  const [reviews, setReviews] = useState([]);
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!station) return;

    setError("");
    setRating(0);
    setComment("");
    setBusy(true);

    Api.reviews(station.stationId)
      .then(setReviews)
      .catch((e) => setError(e.message))
      .finally(() => setBusy(false));
  }, [station]);

  const submit = async (e) => {
    e.preventDefault();

    if (!rating) {
      return setError("Please select a star rating.");
    }

    setSaving(true);
    setError("");

    try {
      await Api.saveReview(station.stationId, {
        rating,
        comment,
      });

      setReviews(await Api.reviews(station.stationId));

      setRating(0);
      setComment("");

      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open={!!station}
      onClose={onClose}
      title={station ? `Reviews — ${station.stationName}` : "Reviews"}
    >
      {station && (
        <>
          <Message text={error} />

          <form onSubmit={submit} className="review-form">
            <label className="label">
              Your rating (posting again updates your review)
            </label>

            <StarRating value={rating} onChange={setRating} size={28} />

            <textarea
              className="input"
              rows={3}
              maxLength={500}
              placeholder="Share your experience (optional)"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />

            <button className="btn btn-primary" disabled={saving}>
              {saving ? "Saving..." : "Submit review"}
            </button>
          </form>

          <div className="review-list">
            {busy && <p className="muted">Loading...</p>}

            {!busy && !reviews.length && (
              <p className="muted">No reviews yet. Be the first!</p>
            )}

            {reviews.map((r) => (
              <div className="review" key={r.reviewId}>
                <div className="row-between">
                  <b>{r.reviewer}</b>
                  <StarRating value={r.rating} size={14} />
                </div>

                {r.comment && <p>{r.comment}</p>}

                <small>{new Date(r.createdAt).toLocaleDateString()}</small>
              </div>
            ))}
          </div>
        </>
      )}
    </Modal>
  );
}
