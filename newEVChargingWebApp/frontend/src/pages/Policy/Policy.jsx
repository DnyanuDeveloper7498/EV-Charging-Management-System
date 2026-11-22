import React from "react";
import { Link, useLocation } from "react-router-dom";
import "./Policy.css";

// TODO before launch: replace SUPPORT_EMAIL / COMPANY with your real details and have a lawyer review the text.
const COMPANY = "Smart EV Charging";
const SUPPORT_EMAIL = "support@your-domain.com";
const UPDATED = "6 November 2026";

const PAGES = {
  "/terms": {
    title: "Terms & Conditions",
    sections: [
      ["Using the service", `${COMPANY} lets you find charging stations, book slots, charge your vehicle and pay for the energy used. You must provide correct information and keep your password confidential. You are responsible for activity on your account.`],
      ["Bookings", "A booking reserves a slot for the chosen time. Bookings that are not started within the grace period after the booked time expire automatically and the slot is released. You may cancel a booking before it starts."],
      ["Charging & pricing", "You are charged for the energy delivered (kWh) at the station's displayed price per kWh. The final amount is calculated when the session ends and is shown before payment."],
      ["Payments", "Payments are processed securely by Razorpay or taken from your in-app wallet. We never store your card, UPI or bank details."],
      ["Acceptable use", "Do not misuse the service, attempt to access other accounts, interfere with stations or use the platform for unlawful purposes. We may suspend accounts that violate these terms."],
      ["Liability", "Stations are operated by independent station owners. We work to keep information accurate, but availability, charging speed and station status may change without notice. To the extent permitted by law we are not liable for indirect losses."],
      ["Changes", "We may update these terms from time to time. Continuing to use the service after an update means you accept the new terms."],
    ],
  },
  "/privacy": {
    title: "Privacy Policy",
    sections: [
      ["What we collect", "Your name, email, mobile number, vehicle details, booking and charging history, wallet and payment records, and (only if you allow it) your device location to show nearby stations."],
      ["How we use it", "To run your account, process bookings and payments, send confirmations and password-reset emails, prevent fraud and improve the service."],
      ["Passwords & payments", "Passwords are stored only as salted hashes. Payment card / UPI details are handled by Razorpay and never reach our servers."],
      ["Sharing", "We share data only with service providers needed to run the service (hosting, email, payment gateway) and when required by law. We do not sell your personal data."],
      ["Location", "Location is used in your browser to sort stations by distance. It is not stored on our servers unless you add it as a station location."],
      ["Your rights", `You can view and update your details on the Profile page. To request deletion of your account and data, email ${SUPPORT_EMAIL}.`],
      ["Security & retention", "We use encrypted connections (HTTPS), access controls and rate limiting. We keep records as long as needed for the service and legal/accounting obligations."],
    ],
  },
  "/refund": {
    title: "Refund & Cancellation Policy",
    sections: [
      ["Booking cancellation", "You can cancel a booking before it starts at no charge. Cancelled and expired bookings are not billed."],
      ["Wallet top-ups", "Money added to your wallet stays in your wallet and can be used for any charging payment. Wallet balance is not withdrawable in cash except where required by law."],
      ["Failed payments", "If money was deducted but the payment failed or the session was not paid, the amount is returned to the original payment method by Razorpay, normally within 5–7 working days."],
      ["Charging disputes", `If you believe a session was billed incorrectly (for example a charger fault), email ${SUPPORT_EMAIL} within 7 days with the session number. Verified errors are refunded to your wallet or original payment method.`],
      ["Contact", `Questions about refunds: ${SUPPORT_EMAIL}.`],
    ],
  },
};

export default function Policy() {
  const { pathname } = useLocation();
  const page = PAGES[pathname] || PAGES["/terms"];

  return (
    <div className="container policy">
      <div className="policy-nav">
        <Link className={pathname === "/terms" ? "active" : ""} to="/terms">Terms</Link>
        <Link className={pathname === "/privacy" ? "active" : ""} to="/privacy">Privacy</Link>
        <Link className={pathname === "/refund" ? "active" : ""} to="/refund">Refunds</Link>
      </div>
      <h1>{page.title}</h1>
      <p className="muted">Last updated: {UPDATED}</p>
      {page.sections.map(([h, t]) => (
        <section key={h}>
          <h3>{h}</h3>
          <p>{t}</p>
        </section>
      ))}
      <p className="muted">Contact: {SUPPORT_EMAIL}</p>
    </div>
  );
}
