// Auth state: the JWT + its expiry live under their own keys, the (password-free) user profile under another.
// Session.get() returns null as soon as the token is missing or expired, so ProtectedRoute / Header never
// show a "logged in" state that the backend would reject.
const USER_KEY = "ev_user";
const TOKEN_KEY = "ev_token";
const EXPIRY_KEY = "ev_token_exp";

const PROFILE_FIELDS = ["userId", "fullName", "email", "mobileNo", "walletBalance", "role", "createdAt"];

function pickProfile(obj) {
  const out = {};
  PROFILE_FIELDS.forEach((k) => {
    if (obj && obj[k] !== undefined) out[k] = obj[k];
  });
  return out;
}

function read(key) {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

export const Session = {
  /** Called after login/register with the backend's { token, expiresAt, user } response. */
  saveAuth({ token, expiresAt, user }) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(EXPIRY_KEY, String(expiresAt));
    localStorage.setItem(USER_KEY, JSON.stringify(pickProfile(user)));
  },

  /** Refreshes the stored profile (e.g. from the wallet response). Never touches the token. */
  save(user) {
    localStorage.setItem(USER_KEY, JSON.stringify(pickProfile(user)));
  },

  token() {
    const token = read(TOKEN_KEY);
    const expiry = Number(read(EXPIRY_KEY));
    if (!token || !expiry || Date.now() >= expiry) return null;
    return token;
  },

  get() {
    if (!Session.token()) {
      Session.clear();
      return null;
    }
    try {
      return JSON.parse(read(USER_KEY));
    } catch {
      return null;
    }
  },

  clear() {
    try {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(EXPIRY_KEY);
      localStorage.removeItem(USER_KEY);
    } catch {
      /* storage unavailable */
    }
  },
};
