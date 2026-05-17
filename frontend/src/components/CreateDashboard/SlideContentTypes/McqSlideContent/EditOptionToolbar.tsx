import React from "react";
import { useEffect, useRef, useState } from "react";
import { getRouteApi } from "@tanstack/react-router";
import { TrashIcon } from "@heroicons/react/24/outline";
import {
  BrainFlex,
  type McqOption as McqOptionType,
} from "@/store/BrainFlexApi";
import { useAppDispatch } from "@/store/hooks";
import { Input } from "@/components/Common/Input/Input/Input";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";
import { Toggle } from "@/components/Common/Input/Toggle/Toggle";
import {
  Popover,
  PopoverRow,
  PopoverButton,
  PopoverDivider,
  PopoverGroupLabel,
} from "@/components/Common/Input/Popover/Popover";

import styles from "./McqOptionEditable.module.css";

interface EditOptionToolbarProps {
  canRemove: boolean;
  pasteUrl: string;
  handlePickFromGallery: () => void;
  hasImage: boolean;
  handleRemove: () => void;
  handleColorChange: (str: string) => void;
  handlePasteUrlChange: (str: string) => void;
  handleClearImage: () => void;
  displayIndex: number;
  previewUrl: string;
  option: McqOptionType;
  inputIdBase: string;
  flush: () => void;
}

const EditOptionToolbar: React.FC<EditOptionToolbarProps> = ({
  canRemove,
  pasteUrl,
  handlePickFromGallery,
  hasImage,
  handleRemove,
  handleColorChange,
  handlePasteUrlChange,
  displayIndex,
  previewUrl,
  option,
  inputIdBase,
  handleClearImage,
  flush,
}) => {
  return (
    <div
      className={styles.popoverWrap}
      onClick={(e) => {
        e.stopPropagation();
      }}>
      <Popover
        role='dialog'
        ariaLabel={`Option ${displayIndex.toString()} settings`}>
        <PopoverRow>
          <PopoverGroupLabel>Image</PopoverGroupLabel>
          <button
            type='button'
            className={[
              styles.imageThumb,
              hasImage ? "" : styles.imageThumbEmpty,
            ]
              .filter(Boolean)
              .join(" ")}
            style={
              previewUrl ? { backgroundImage: `url(${previewUrl})` } : undefined
            }
            aria-label={hasImage ? "Change image" : "Pick image"}
            onClick={handlePickFromGallery}>
            {!hasImage && <span aria-hidden='true'>+</span>}
          </button>
          {hasImage && (
            <PopoverButton ariaLabel='Clear image' onClick={handleClearImage}>
              Clear
            </PopoverButton>
          )}

          <PopoverDivider />

          <PopoverGroupLabel>Color</PopoverGroupLabel>
          <input
            type='color'
            id={`${inputIdBase}-color`}
            className={styles.colorSwatch}
            value={option.color ?? "#000"}
            onChange={(e) => {
              handleColorChange(e.target.value);
            }}
            onBlur={flush}
            aria-label='Option color'
          />

          <PopoverDivider />

          <PopoverButton
            ariaLabel={`Remove option ${displayIndex.toString()}`}
            disabled={!canRemove}
            onClick={handleRemove}>
            <TrashIcon />
          </PopoverButton>
        </PopoverRow>

        <PopoverRow>
          <PopoverGroupLabel>URL</PopoverGroupLabel>
          <input
            type='text'
            id={`${inputIdBase}-url`}
            placeholder='Or paste an image URL'
            value={pasteUrl}
            onChange={(e) => {
              handlePasteUrlChange(e.target.value);
            }}
            onBlur={flush}
            className={styles.popoverUrlInput}
          />
        </PopoverRow>
      </Popover>
    </div>
  );
};

export { EditOptionToolbar };
