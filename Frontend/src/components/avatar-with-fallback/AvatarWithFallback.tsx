import { useState, type CSSProperties } from "react";
import classes from "./scss/avatar-with-fallback.module.css";

interface AvatarWithFallbackProps {
  src: string;
  fallbackText: string;
  alt: string;
  width: CSSProperties["width"];
  height: CSSProperties["height"];
}

const AvatarWithFallback = (props: AvatarWithFallbackProps) => {
  const { src, alt, fallbackText, width, height } = props;
  const [hasError, setHasError] = useState(false);

  return hasError ? (
    <div className={classes.avatar_fallback} style={{ width, height }}>
      {fallbackText.slice(0, 2).toUpperCase()}
    </div>
  ) : (
    <img src={src} alt={alt} onError={() => setHasError(true)} width={width} height={height} />
  );
};

export default AvatarWithFallback;
