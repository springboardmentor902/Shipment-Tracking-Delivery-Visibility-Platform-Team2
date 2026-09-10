"use client";

import { FormEvent } from "react";

export type RegistrationRole = "CUSTOMER" | "BUSINESS_CLIENT";

const roleDetails: Record<RegistrationRole, { label: string; description: string }> = {
  CUSTOMER: {
    label: "Customer",
    description: "Create and track your own shipments.",
  },
  BUSINESS_CLIENT: {
    label: "Business Client",
    description: "Manage business shipments, analytics, and reports.",
  },
};

type Props = {
  mode: "login" | "register";
  onModeChange: (mode: "login" | "register") => void;
  onClose: () => void;
  onLogin: (event: FormEvent<HTMLFormElement>) => void;
  onRegister: (event: FormEvent<HTMLFormElement>) => void;
  loginEmail: string;
  loginPassword: string;
  registerName: string;
  registerEmail: string;
  registerPassword: string;
  registerRole: RegistrationRole;
  setLoginEmail: (value: string) => void;
  setLoginPassword: (value: string) => void;
  setRegisterName: (value: string) => void;
  setRegisterEmail: (value: string) => void;
  setRegisterPassword: (value: string) => void;
  setRegisterRole: (value: RegistrationRole) => void;
  feedback?: { text: string; tone: "success" | "error" };
}

export default function AuthModal(props: Props) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && props.onClose()}>
      <section className="auth-modal" role="dialog" aria-modal="true" aria-labelledby="auth-title">
        <button className="modal-close" onClick={props.onClose} aria-label="Close authentication dialog">×</button>
        <p className="eyebrow">SHIPTRACK PRO</p>
        <h2 id="auth-title">{props.mode === "login" ? "Welcome back" : "Create your account"}</h2>
        <p className="subtitle">{props.mode === "login" ? "Sign in to manage your delivery operations." : "Start tracking shipments with your team."}</p>
        {props.feedback && <p className={`auth-feedback ${props.feedback.tone}`} role="alert">{props.feedback.text}</p>}
        <div className="auth-switcher">
          <button className={props.mode === "login" ? "active" : ""} onClick={() => props.onModeChange("login")}>Login</button>
          <button className={props.mode === "register" ? "active" : ""} onClick={() => props.onModeChange("register")}>Register</button>
        </div>
        {props.mode === "login" ? (
          <form className="form" onSubmit={props.onLogin}>
            <input required type="email" value={props.loginEmail} onChange={(event) => props.setLoginEmail(event.target.value)} placeholder="Work email" />
            <input required type="password" value={props.loginPassword} onChange={(event) => props.setLoginPassword(event.target.value)} placeholder="Password" />
            <button type="submit">Sign in</button>
          </form>
        ) : (
          <form className="form" onSubmit={props.onRegister}>
            <input required value={props.registerName} onChange={(event) => props.setRegisterName(event.target.value)} placeholder="Full name" />
            <input required type="email" value={props.registerEmail} onChange={(event) => props.setRegisterEmail(event.target.value)} placeholder="Work email" />
            <input required minLength={8} type="password" value={props.registerPassword} onChange={(event) => props.setRegisterPassword(event.target.value)} placeholder="Password (minimum 8 characters)" />
            <label className="role-field">
              <span>Account role</span>
              <select
                value={props.registerRole}
                onChange={(event) => props.setRegisterRole(event.target.value as RegistrationRole)}
              >
                {Object.entries(roleDetails).map(([value, details]) => (
                  <option key={value} value={value}>{details.label}</option>
                ))}
              </select>
            </label>
            <p className="role-description">
              <strong>{roleDetails[props.registerRole].label}</strong>
              {roleDetails[props.registerRole].description}
            </p>
            <button type="submit">Create {roleDetails[props.registerRole].label.toLowerCase()} account</button>
            <small>Operator, Support Agent, and Administrator roles are assigned internally.</small>
          </form>
        )}
      </section>
    </div>
  );
}
