import { useId, type InputHTMLAttributes, type ReactNode } from "react";

export interface GlassTextFieldProps
  extends InputHTMLAttributes<HTMLInputElement> {
  label?: ReactNode;
}

/**
 * GlassTextField — capsule input (reference 2's "Ask AI" input shape):
 * glass shell, pill radius, focus ring via the shell's :focus-within.
 */
export function GlassTextField({
  label,
  id,
  className,
  ...rest
}: GlassTextFieldProps) {
  const auto = useId();
  const inputId = id ?? auto;
  return (
    <div className={`gfield${className ? ` ${className}` : ""}`}>
      {label ? (
        <label className="gfield-label" htmlFor={inputId}>
          {label}
        </label>
      ) : null}
      <div className="glass gfield-shell">
        <input id={inputId} className="gfield-input" {...rest} />
      </div>
    </div>
  );
}
