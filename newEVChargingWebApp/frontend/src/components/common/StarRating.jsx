import React from "react";
import "./StarRating.css";

/** Read-only stars (value) or clickable stars (onChange). */
export default function StarRating({ value = 0, onChange, size = 18 }) {
  const rounded = Math.round(value);
  return (
    <span className="stars" style={{ fontSize: size }} aria-label={`${value} out of 5`}>
      {[1, 2, 3, 4, 5].map((n) =>
        onChange ? (
          <button
            type="button"
            key={n}
            className={`star star-btn ${n <= rounded ? "on" : ""}`}
            onClick={() => onChange(n)}
            aria-label={`${n} star${n > 1 ? "s" : ""}`}
          >
            ★
          </button>
        ) : (
          <span key={n} className={`star ${n <= rounded ? "on" : ""}`}>
            ★
          </span>
        ),
      )}
    </span>
  );
}
