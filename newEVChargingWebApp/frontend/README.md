# Smart EV Charging System — React Frontend

Converted from the supplied HTML frontend into a responsive React/Vite frontend.

## Pages
- Home
- Login
- Register
- Dashboard
- Charging Stations
- My Vehicles
- My Bookings
- Charging Session
- Wallet
- Billing & Payments
- Reports

## Structure
Every page has its own `.jsx` and `.css`. Shared UI components also have separate CSS files.

## Backend
The frontend uses the same API endpoints from the original project.

Default API:
`http://localhost:8080`

For another backend URL, create `.env`:
`VITE_API_BASE=https://your-backend-url`

## Run
```bash
npm install
npm run dev
```

## Build
```bash
npm run build
```
