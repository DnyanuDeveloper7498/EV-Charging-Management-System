
import { useEffect, useMemo } from "react";
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  useMap
} from "react-leaflet";

import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "./StationMap.css";

const createMarkerIcon = (status, selected) => {
  const color = selected
    ? "#2563EB"
    : status
      ? "#16A34A"
      : "#DC2626";

  return L.divIcon({
    className: "custom-station-marker",
    html: `
      <div class="station-marker ${selected ? "selected" : ""}"
           style="--marker-color:${color}">
        <span></span>
      </div>
    `,
    iconSize: [34, 42],
    iconAnchor: [17, 38],
    popupAnchor: [0, -36]
  });
};

function MapController({ selectedStation }) {
  const map = useMap();

  useEffect(() => {
    if (
      selectedStation &&
      Number.isFinite(Number(selectedStation.latitude)) &&
      Number.isFinite(Number(selectedStation.longitude))
    ) {
      map.flyTo(
        [
          Number(selectedStation.latitude),
          Number(selectedStation.longitude)
        ],
        15,
        { duration: 0.8 }
      );
    }
  }, [selectedStation, map]);

  return null;
}

export default function StationMap({
  stations = [],
  activeStationId,
  onSelectStation
}) {
  const validStations = useMemo(() => {
    return stations.filter((station) => {
      return (
        Number.isFinite(Number(station.latitude)) &&
        Number.isFinite(Number(station.longitude)) &&
        station.latitude !== "" &&
        station.longitude !== ""
      );
    });
  }, [stations]);

  const selectedStation = validStations.find(
    (station) => String(station._id || station.stationId) ===
      String(activeStationId)
  );

  const center = selectedStation
    ? [
        Number(selectedStation.latitude),
        Number(selectedStation.longitude)
      ]
    : validStations.length
      ? [
          Number(validStations[0].latitude),
          Number(validStations[0].longitude)
        ]
      : [18.5204, 73.8567];

  return (
    <div className="premium-station-map">
      <div className="map-heading">
        <div>
          <span className="map-eyebrow">EXPLORE LOCATIONS</span>
          <h3>Charging Stations</h3>
          <p>Find your nearest charging point</p>
        </div>

        <div className="map-live-status">
          <span></span> Live Map
        </div>
      </div>

      <div className="station-map-container">
        <MapContainer
          center={center}
          zoom={13}
          scrollWheelZoom={true}
          className="station-leaflet-map"
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          <MapController selectedStation={selectedStation} />

          {validStations.map((station) => {
            const id = station._id || station.stationId;
            const isSelected =
              String(id) === String(activeStationId);

            const isAvailable = [
              "",
              "available",
              "active"
            ].includes(String(station.status || "").toLowerCase());

            return (
              <Marker
                key={id}
                position={[
                  Number(station.latitude),
                  Number(station.longitude)
                ]}
                icon={createMarkerIcon(isAvailable, isSelected)}
                eventHandlers={{
                  click: () => onSelectStation?.(station)
                }}
              >
                <Popup>
                  <div className="station-map-popup">
                    <strong>{station.stationName}</strong>
                    <p>{station.sLocation}</p>
                    <span>
                      {isAvailable ? "Available" : "Unavailable"}
                    </span>
                    <p>
                      {station.availableSlots ?? 0} slots available
                    </p>
                    <button
                      onClick={() => onSelectStation?.(station)}
                    >
                      View Station
                    </button>
                  </div>
                </Popup>
              </Marker>
            );
          })}
        </MapContainer>

        <div className="map-legend">
          <span><i className="legend-green"></i> Available</span>
          <span><i className="legend-red"></i> Unavailable</span>
          <span><i className="legend-blue"></i> Selected</span>
        </div>
      </div>

      <div className="map-footer">
        <span><strong>{validStations.length}</strong> locations on map</span>
        <span><i></i> OpenStreetMap</span>
      </div>
    </div>
  );
}
