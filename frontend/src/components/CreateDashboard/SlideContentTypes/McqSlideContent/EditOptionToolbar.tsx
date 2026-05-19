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
import { hexToHsva, type HexColor } from "@uiw/color-convert";

import {
  Popover,
  PopoverRow,
  PopoverDivider,
  PopoverGroupLabel,
} from "@/components/Common/Input/Popover/Popover";
import { Input } from "@/components/Common/Input/Input/Input";
import { Btn } from "@/components/Common/Buttons/Btn";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";

import styles from "./McqOptionEditable.module.css";
import {
  ColorPoint,
  ColorSwatch,
} from "@/components/Common/Input/ColorPicker/ColorSwatch";
import { hueToHex, parseHue, toHexColor } from "@/utils/color";

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
  const handleColorPick = (colorPick: HexColor) => {
    handleColorChange(colorPick);
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
          <ColorPoint color={toHexColor(hueToHex(parseHue(color)))} />
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
            ariaLabel={hasImage ? "Change image" : "Pick image"}
            onClick={handlePickFromGallery}
            icon={hasImage ? undefined : <PlusIcon />}
          />
          {hasImage && (
            <Btn variant='ghost' size='xs' onClick={handleClearImage}>
              Clear
            </Btn>
          )}

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

          <PopoverDivider />

          <ColorSwatch
            color={toHexColor(hueToHex(parseHue(color)))}
            onChange={handleColorPick}
          />

          <PopoverDivider />

          <IconBtn
            variant='delete'
            size='xs'
            ariaLabel={`Remove option ${displayIndex.toString()}`}
            disabled={!canRemove}
            onClick={handleRemove}
            icon={<TrashIcon />}
          />
          <PopoverDivider />

          <IconBtn
            variant='close'
            size='xs'
            ariaLabel='Close'
            onClick={handleClose}
          />
        </PopoverRow>
      </Popover>
    </div>
  );
};

export { EditOptionToolbar };
