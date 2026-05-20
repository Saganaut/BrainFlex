/**
 * Player canvas for an in-flight Drawing round.
 *
 * Pointer events drive a single-finger draw model: pointerdown opens a stroke,
 * pointermove appends points (one per RAF tick), pointerup finalizes the
 * stroke and pushes it onto the stroke list. The list is kept locally — we
 * only POST a DrawingAnswer when the player clicks Submit. Undo pops the
 * last stroke; Clear blanks the canvas.
 *
 * Strokes are captured in *logical* units (`canvasWidth`/`canvasHeight` on
 * the question) so a drawing made on a phone reads identically on a 4K host
 * display. On submit we run a Douglas-Peucker pass to drop redundant points
 * before serializing — keeps the per-answer byte cap honest without changing
 * the look of the sketch.
 *
 * Touch + mouse + pen are unified by Pointer events; we also lock the page
 * from scrolling under finger drags via `touch-action: none` on the canvas.
 * Multi-touch pan/zoom is intentionally out of scope for v1.
 */
import { useEffect, useRef, useState } from "react";
import { Btn } from "@/components/Common/Buttons/Btn";
import type {
  AnswerPayload,
  DrawingQuestion,
  Stroke,
} from "@/types/elements";
import { largestUrl } from "@/utils/image";
import {
  DEFAULT_DRAWING_PALETTE,
  STROKE_THICKNESSES,
  downsampleStrokes,
  renderStrokes,
  resolvePaletteColor,
} from "./drawingUtils";
import styles from "./DrawingCanvas.module.css";

interface DrawingCanvasProps {
  element: DrawingQuestion;
  submittedStrokes: Stroke[] | null;
  disabled: boolean;
  onSubmit: (payload: AnswerPayload) => void;
}

/** Logical-units → pixel-buffer scale for the in-DOM canvas. Bigger = sharper,
 *  but eats memory; 1280px on the long axis is the sweet spot for phone +
 *  desktop. */
const CANVAS_PIXEL_LIMIT = 1280;

const DrawingCanvas = ({
  element,
  submittedStrokes,
  disabled,
  onSubmit,
}: DrawingCanvasProps) => {
  const canvasWidth = element.canvasWidth ?? 1920;
  const canvasHeight = element.canvasHeight ?? 1080;
  const maxStrokes = element.maxStrokesPerPlayer ?? 200;
  const maxPoints = element.maxPointsPerStroke ?? 500;
  const palette =
    element.palette && element.palette.length > 0
      ? element.palette
      : [...DEFAULT_DRAWING_PALETTE];

  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const backgroundImgRef = useRef<HTMLImageElement | null>(null);
  const activeStrokeRef = useRef<Stroke | null>(null);
  // Append points on the next RAF tick so we coalesce fast pointermoves.
  const pendingPointRef = useRef<{ x: number; y: number } | null>(null);
  const rafIdRef = useRef<number | null>(null);

  const [strokes, setStrokes] = useState<Stroke[]>([]);
  const [color, setColor] = useState<string>(palette[0]);
  const [thickness, setThickness] = useState<number>(STROKE_THICKNESSES[1]);

  const locked = submittedStrokes !== null || disabled;
  const backgroundUrl = largestUrl(element.backingImage, element.id ?? "draw");

  // Resize the pixel buffer to match the canvas aspect ratio, capped at
  // CANVAS_PIXEL_LIMIT on the long axis. Runs once per mount and on resize.
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const aspect = canvasWidth / canvasHeight;
    if (aspect >= 1) {
      canvas.width = CANVAS_PIXEL_LIMIT;
      canvas.height = Math.round(CANVAS_PIXEL_LIMIT / aspect);
    } else {
      canvas.height = CANVAS_PIXEL_LIMIT;
      canvas.width = Math.round(CANVAS_PIXEL_LIMIT * aspect);
    }
  }, [canvasWidth, canvasHeight]);

  // Pre-load the backing image once so renderStrokes can blit it on every
  // repaint. The blank-canvas case skips this entirely.
  useEffect(() => {
    if (!backgroundUrl) {
      backgroundImgRef.current = null;
      const canvas = canvasRef.current;
      if (canvas) renderStrokes(canvas, strokes, canvasWidth, canvasHeight);
      return;
    }
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.src = backgroundUrl;
    img.onload = () => {
      backgroundImgRef.current = img;
      const canvas = canvasRef.current;
      if (canvas)
        renderStrokes(canvas, strokes, canvasWidth, canvasHeight, img);
    };
    // Strokes/canvas-size are intentionally not deps — the dedicated repaint
    // effect below handles those. This effect runs once per backing-image URL.
    /* eslint-disable-next-line react-hooks/exhaustive-deps, react-x/exhaustive-deps */
  }, [backgroundUrl]);

  // Repaint the canvas whenever the stroke list changes.
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    renderStrokes(
      canvas,
      strokes,
      canvasWidth,
      canvasHeight,
      backgroundImgRef.current ?? undefined,
    );
  }, [strokes, canvasWidth, canvasHeight]);

  const logicalCoords = (e: React.PointerEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width) * canvasWidth;
    const y = ((e.clientY - rect.top) / rect.height) * canvasHeight;
    return {
      x: Math.max(0, Math.min(canvasWidth, x)),
      y: Math.max(0, Math.min(canvasHeight, y)),
    };
  };

  const flushPendingPoint = () => {
    rafIdRef.current = null;
    const point = pendingPointRef.current;
    const stroke = activeStrokeRef.current;
    pendingPointRef.current = null;
    if (!point || !stroke) return;
    const pts = stroke.points ?? [];
    const last = pts.length >= 2 ? { x: pts[pts.length - 2], y: pts[pts.length - 1] } : null;
    // Drop sub-pixel duplicates — keeps the cap budget for real movement.
    if (last && Math.abs(last.x - point.x) < 0.5 && Math.abs(last.y - point.y) < 0.5) {
      return;
    }
    if (pts.length / 2 >= maxPoints) return;
    const nextStroke: Stroke = {
      ...stroke,
      points: [...pts, point.x, point.y],
    };
    activeStrokeRef.current = nextStroke;
    setStrokes((prev) => {
      // Replace the active stroke (always the last one once started).
      if (prev.length === 0) return [nextStroke];
      const head = prev.slice(0, -1);
      return [...head, nextStroke];
    });
  };

  const handlePointerDown = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (locked) return;
    if (strokes.length >= maxStrokes) return;
    e.preventDefault();
    e.currentTarget.setPointerCapture(e.pointerId);
    const { x, y } = logicalCoords(e);
    const stroke: Stroke = {
      color,
      thickness,
      points: [x, y],
    };
    activeStrokeRef.current = stroke;
    setStrokes((prev) => [...prev, stroke]);
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (!activeStrokeRef.current) return;
    e.preventDefault();
    const { x, y } = logicalCoords(e);
    pendingPointRef.current = { x, y };
    if (rafIdRef.current !== null) return;
    rafIdRef.current = requestAnimationFrame(flushPendingPoint);
  };

  const finishStroke = () => {
    if (rafIdRef.current !== null) {
      cancelAnimationFrame(rafIdRef.current);
      rafIdRef.current = null;
      flushPendingPoint();
    }
    activeStrokeRef.current = null;
    pendingPointRef.current = null;
  };

  const handlePointerUp = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (!activeStrokeRef.current) return;
    e.currentTarget.releasePointerCapture(e.pointerId);
    finishStroke();
  };

  const handleUndo = () => {
    if (locked) return;
    setStrokes((prev) => prev.slice(0, -1));
  };

  const handleClear = () => {
    if (locked) return;
    setStrokes([]);
  };

  const handleSubmit = () => {
    if (locked) return;
    const simplified = downsampleStrokes(strokes);
    onSubmit({ kind: "DrawingAnswer", strokes: simplified });
  };

  const strokeCount = strokes.length;
  const canSubmit = !locked && strokeCount > 0;

  return (
    <div className={styles.wrapper}>
      <div className={styles.canvasFrame} style={{ aspectRatio: `${String(canvasWidth)} / ${String(canvasHeight)}` }}>
        <canvas
          ref={canvasRef}
          className={styles.canvas}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
          onPointerLeave={(e) => {
            if (activeStrokeRef.current) {
              handlePointerUp(e);
            }
          }}
          aria-label='Drawing canvas'
        />
        {locked && (
          <div className={styles.lockedBadge}>
            Locked in. Waiting for the round to end…
          </div>
        )}
      </div>

      <div className={styles.toolbar}>
        <div
          className={styles.palette}
          role='radiogroup'
          aria-label='Stroke color'>
          {palette.map((swatch) => {
            const selected = swatch === color;
            return (
              <button
                key={swatch}
                type='button'
                className={`${styles.swatch} ${selected ? styles.swatchSelected : ""}`}
                style={{ background: resolvePaletteColor(swatch) }}
                role='radio'
                aria-checked={selected}
                aria-label={`Color ${swatch}`}
                disabled={locked}
                onClick={() => {
                  setColor(swatch);
                }}
              />
            );
          })}
        </div>

        <div
          className={styles.thicknessRow}
          role='radiogroup'
          aria-label='Stroke thickness'>
          {STROKE_THICKNESSES.map((t) => {
            const selected = t === thickness;
            return (
              <button
                key={t}
                type='button'
                className={`${styles.thickness} ${selected ? styles.thicknessSelected : ""}`}
                role='radio'
                aria-checked={selected}
                aria-label={`Thickness ${String(t)}`}
                disabled={locked}
                onClick={() => {
                  setThickness(t);
                }}>
                <span
                  className={styles.thicknessDot}
                  style={{
                    width: `${String(Math.min(28, t / 2 + 4))}px`,
                    height: `${String(Math.min(28, t / 2 + 4))}px`,
                    background: resolvePaletteColor(color),
                  }}
                />
              </button>
            );
          })}
        </div>

        <div className={styles.actions}>
          <Btn
            type='button'
            variant='secondary'
            size='sm'
            disabled={locked || strokeCount === 0}
            onClick={handleUndo}>
            Undo
          </Btn>
          <Btn
            type='button'
            variant='secondary'
            size='sm'
            disabled={locked || strokeCount === 0}
            onClick={handleClear}>
            Clear
          </Btn>
          <Btn
            type='button'
            disabled={!canSubmit}
            onClick={handleSubmit}>
            Submit
          </Btn>
        </div>
      </div>

      <p className={styles.counts} aria-live='polite'>
        {strokeCount} / {maxStrokes} {strokeCount === 1 ? "stroke" : "strokes"}
      </p>
    </div>
  );
};

export { DrawingCanvas };
