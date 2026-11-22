import React from "react";
import { useNavigate } from "react-router-dom";
import "./PageHeader.css";
export default function PageHeader({
  icon,
  title,
  subtitle,
  back = true,
  actions,
}) {
  const nav = useNavigate();
  return (
    <div className="page-heading">
      <div>
       
        <h1>
          {icon && <span>{icon}</span>}
          {title}
        </h1>
        <p>{subtitle}</p>
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  );
}
