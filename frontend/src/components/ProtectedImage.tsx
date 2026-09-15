"use client";

import Image from "next/image";
import { useEffect, useState } from "react";

export default function ProtectedImage({ src, token, alt }: { src: string; token: string; alt: string }) {
  const [loaded, setLoaded] = useState<{ src: string; url: string }>();

  useEffect(() => {
    const controller = new AbortController();
    let objectUrl = "";
    void fetch(src, { headers: { Authorization: `Bearer ${token}` }, signal: controller.signal })
      .then((response) => response.ok ? response.blob() : undefined)
      .then((blob) => {
        if (!blob) return;
        objectUrl = URL.createObjectURL(blob);
        setLoaded({ src, url: objectUrl });
      })
      .catch(() => undefined);
    return () => {
      controller.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [src, token]);

  if (loaded?.src !== src) return <div className="proof-image-loading">Loading proof...</div>;
  return <Image src={loaded.url} alt={alt} width={700} height={420} unoptimized />;
}
