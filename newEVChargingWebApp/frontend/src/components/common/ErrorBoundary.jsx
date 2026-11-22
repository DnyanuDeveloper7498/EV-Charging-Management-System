import React from "react";

/** Catches rendering crashes so one broken page never leaves the user with a blank white screen. */
export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { failed: false };
  }

  static getDerivedStateFromError() {
    return { failed: true };
  }

  componentDidCatch(error, info) {
    console.error("UI error:", error, info);
  }

  render() {
    if (!this.state.failed) return this.props.children;
    return (
      <div className="container text-center">
        <h1>Something went wrong</h1>
        <p style={{ color: "var(--gray)" }}>
          An unexpected error occurred. Please reload the page.
        </p>
        <button
          className="btn btn-primary"
          onClick={() => window.location.assign("/")}
        >
          Go to Home
        </button>
      </div>
    );
  }
}
