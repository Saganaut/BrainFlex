// Floating editor for a single MCQ option. Rendered by McqOptionEditable
// inside the option card's popover and styled with the shared Popover
// primitive. All controls route through Common/Buttons and Common/Input
// so the surface inherits the design-system look automatically.
//
// Color: option.color is stored as a CSS color string. Legacy values may
// be hex (from the old native color input); the parent palette default is
// oklch(0.65 0.18 H). We parse hue out of either format for the picker
// and always write back oklch so storage normalizes over time.
import { TrashIcon } from "@heroicons/react/24/outline";
import { PlusIcon } from "@heroicons/react/24/solid";
import { hexToHsva } from "@uiw/color-convert";

import {
  Popover,
  PopoverRow,
  PopoverDivider,
  PopoverGroupLabel,
} from "@/components/Common/Input/Popover/Popover";
import { ColorPicker } from "@/components/Common/Input/ColorPicker/ColorPicker";
import { Input } from "@/components/Common/Input/Input/Input";
import { Btn } from "@/components/Common/Buttons/Btn";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";

import styles from "./McqOptionEditable.module.css";

const OKLCH_HUE_RX = /oklch\(\s*[\d.]+\s+[\d.]+\s+(-?[\d.]+)/;
const HEX_RX = /^#[0-9a-fA-F]{3,8}$/;

const parseHue = (color: string): number => {
  const oklch = OKLCH_HUE_RX.exec(color);
  if (oklch) {
    const n = Number(oklch[1]);
    return Math.round(((n % 360) + 360) % 360);
  }
  if (HEX_RX.test(color)) {
    return Math.round(hexToHsva(color).h);
  }
  return 0;
};

const hueToColor = (hue: number): string =>
  `oklch(0.65 0.18 ${hue.toString()})`;

interface EditOptionToolbarProps {
  canRemove: boolean;
  pasteUrl: string;
  handlePickFromGallery: () => void;
  hasImage: boolean;
  handleRemove: () => void;
  handleColorChange: (str: string) => void;
  handlePasteUrlChange: (str: string) => void;
  handleClearImage: () => void;
  handleClose: () => void;
  displayIndex: number;
  previewUrl: string;
  /** Resolved color string (option override or palette default). */
  color: string;
  inputIdBase: string;
  flush: () => void;
}

const EditOptionToolbar = ({
  canRemove,
  pasteUrl,
  handlePickFromGallery,
  hasImage,
  handleRemove,
  handleColorChange,
  handlePasteUrlChange,
  handleClearImage,
  handleClose,
  displayIndex,
  previewUrl,
  color,
  inputIdBase,
  flush,
}: EditOptionToolbarProps) => {
  const hue = parseHue(color);

  const handleHuePick = (nextHue: number) => {
    handleColorChange(hueToColor(nextHue));
    flush();
  };

  return (
    <div
      className={styles.popoverWrap}
      onClick={(e) => {
        e.stopPropagation();
      }}>
      <Popover
        role='dialog'
        ariaLabel={`Option ${displayIndex.toString()} settings`}>
        <PopoverRow className={styles.popoverHeader}>
          <IconBtn
            variant='close'
            size='xs'
            aria-label='Close'
            onClick={handleClose}
          />
        </PopoverRow>

        <PopoverRow>
          <PopoverGroupLabel>Image</PopoverGroupLabel>
          <IconBtn
            variant='ghost'
            size='xs'
            className={styles.imageThumbBtn}
            style={
              previewUrl
                ? {
                    backgroundImage: `url(${previewUrl})`,
                    backgroundSize: "cover",
                    backgroundPosition: "center",
                    backgroundRepeat: "no-repeat",
                  }
                : undefined
            }
            aria-label={hasImage ? "Change image" : "Pick image"}
            onClick={handlePickFromGallery}
            icon={hasImage ? undefined : <PlusIcon />}
          />
          {hasImage && (
            <Btn variant='ghost' size='xs' onClick={handleClearImage}>
              Clear
            </Btn>
          )}
        </PopoverRow>

        <PopoverRow>
          <PopoverGroupLabel>URL</PopoverGroupLabel>
          <Input
            id={`${inputIdBase}-url`}
            type='text'
            placeholder='Or paste an image URL'
            value={pasteUrl}
            onChange={(e) => {
              handlePasteUrlChange(e.target.value);
            }}
            onBlur={flush}
            fullWidth
          />
        </PopoverRow>

        <PopoverDivider />

        <PopoverRow>
          <ColorPicker label='Color' value={hue} onChange={handleHuePick} />
        </PopoverRow>

        <PopoverDivider />

        <PopoverRow>
          <IconBtn
            variant='delete'
            size='xs'
            aria-label={`Remove option ${displayIndex.toString()}`}
            disabled={!canRemove}
            onClick={handleRemove}
            icon={<TrashIcon />}
          />
        </PopoverRow>
      </Popover>
    </div>
  );
};

export { EditOptionToolbar };
