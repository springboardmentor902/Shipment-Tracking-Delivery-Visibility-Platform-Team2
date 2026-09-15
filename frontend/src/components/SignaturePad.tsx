"use client";

import { forwardRef, useEffect, useImperativeHandle, useRef, useState } from "react";

export type SignaturePadHandle = {
  toFile: () => Promise<File>;
  clear: () => void;
};

const SignaturePad = forwardRef<SignaturePadHandle>(function SignaturePad(_, ref) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [signed, setSigned] = useState(false);

  function clear() {
    const canvas = canvasRef.current;
    const context = canvas?.getContext("2d");
    if (!canvas || !context) return;
    context.clearRect(0, 0, canvas.width, canvas.height);
    setSigned(false);
  }

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ratio = window.devicePixelRatio || 1;
    canvas.width = canvas.clientWidth * ratio;
    canvas.height = canvas.clientHeight * ratio;
    canvas.getContext("2d")?.scale(ratio, ratio);
  }, []);

  useImperativeHandle(ref, () => ({
    clear,
    toFile: () => new Promise((resolve, reject) => {
      if (!signed || !canvasRef.current) {
        reject(new Error("Recipient signature is required."));
        return;
      }
      canvasRef.current.toBlob((blob) => {
        if (!blob) reject(new Error("Could not save the signature."));
        else resolve(new File([blob], "signature.png", { type: "image/png" }));
      }, "image/png");
    }),
  }), [signed]);

  function point(event: React.PointerEvent<HTMLCanvasElement>) {
    const bounds = event.currentTarget.getBoundingClientRect();
    return { x: event.clientX - bounds.left, y: event.clientY - bounds.top };
  }

  function start(event: React.PointerEvent<HTMLCanvasElement>) {
    const context = event.currentTarget.getContext("2d");
    if (!context) return;
    event.currentTarget.setPointerCapture(event.pointerId);
    const current = point(event);
    context.beginPath();
    context.moveTo(current.x, current.y);
    context.strokeStyle = "#0f172a";
    context.lineWidth = 2.2;
    context.lineCap = "round";
    setSigned(true);
  }

  function draw(event: React.PointerEvent<HTMLCanvasElement>) {
    if (!event.currentTarget.hasPointerCapture(event.pointerId)) return;
    const context = event.currentTarget.getContext("2d");
    if (!context) return;
    const current = point(event);
    context.lineTo(current.x, current.y);
    context.stroke();
  }

  return (
    <div className="signature-pad">
      <canvas
        ref={canvasRef}
        onPointerDown={start}
        onPointerMove={draw}
        onPointerUp={(event) => event.currentTarget.releasePointerCapture(event.pointerId)}
        aria-label="Recipient signature"
      />
      <button type="button" className="soft" onClick={clear}>Clear signature</button>
    </div>
  );
});

export default SignaturePad;
