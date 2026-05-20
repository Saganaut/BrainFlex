// Style panel for the deck-editor right sidebar. Two scopes:
//   1. Deck-wide theme — a single Dropdown collapsing the preset list +
//      the user's custom themes into one compact control. "+ New theme"
//      opens the shared ThemeEditor modal.
//   2. Per-element style — content image (element.image) and background
//      image (element.background). Both go through useGalleryPicker so the
//      full Image record (useExternalImg / internalImgId / variants) is
//      what's committed; the backend strips imgUrl on write and rehydrates
//      variants on read.
//
// Every DeckElement kind carries `image` and `background` on the shared
// interface, so the per-element block mounts for any selected element. It's
// only hidden when no element is selected (e.g. the deck editor is open
// without a focused slide in the route).
import type { DeckDto, Image, ThemeResponse } from "@/store/BrainFlexApi";
import { useThemePicker } from "@/hooks/useThemePicker";
import { useElementEditor } from "../SlideContentTypes/useElementEditor";
import { useGalleryPicker } from "@/hooks/useGalleryPicker";
import { emptyImage, isImageEmpty, resolveImageUrl } from "@/utils/image";
import { Btn } from "@/components/Common/Buttons/Btn";
import { IconBtn } from "@/components/Common/Buttons/IconBtn";
import { Dropdown } from "@/components/Common/Input/Dropdown/Dropdown";
import { XMarkIcon, PhotoIcon } from "@heroicons/react/24/outline";
import styles from "./ThemePanel.module.css";

const PRESET_PREFIX = "preset:";
const THEME_PREFIX = "theme:";

type DeckElement = NonNullable<DeckDto["elements"]>[number];

// useElementEditor requires a type predicate to narrow the union. Since the
// section applies to every kind, this is a tautology that just satisfies the
// signature.
const anyElement = (_e: DeckElement): _e is DeckElement => true;

interface ImagePickerProps {
  label: string;
  image: Image | undefined;
  seed: string;
  onPick: () => void;
  onClear: () => void;
}

const ImagePicker = ({
  label,
  image,
  seed,
  onPick,
  onClear,
}: ImagePickerProps) => {
  const hasImage = !isImageEmpty(image);
  const thumbnailSrc = resolveImageUrl(image, "SM", seed, 200, 200, false);

  return (
    <div className={styles.imagePicker}>
      <span className={styles.imagePickerLabel}>{label}</span>
      <button
        type='button'
        className={styles.imageTile}
        onClick={onPick}
        aria-label={`Pick ${label.toLowerCase()}`}>
        {hasImage && thumbnailSrc ? (
          <img src={thumbnailSrc} alt='' />
        ) : (
          <span className={styles.imageTileEmpty}>
            <PhotoIcon aria-hidden='true' />
            <span>Choose image</span>
          </span>
        )}
        {hasImage && (
          <IconBtn
            fill='ghost'
            size='xs'
            className={styles.imageClear}
            icon={<XMarkIcon />}
            aria-label={`Clear ${label.toLowerCase()}`}
            onClick={(e) => {
              e.stopPropagation();
              onClear();
            }}
          />
        )}
      </button>
    </div>
  );
};

const PerSlideStyle = () => {
  const { element, commit, syncedFromId, markSynced } =
    useElementEditor<DeckElement>(anyElement);
  const openPicker = useGalleryPicker();

  if (element && syncedFromId !== element.id) {
    markSynced(element.id);
  }

  if (!element) return null;

  // Spreading a discriminated union and overriding shared fields keeps the
  // `kind` discriminator intact, but TS can't prove that for the union
  // member type, so the cast is required on the way out.
  const handlePickImage = () => {
    openPicker((image) => {
      commit({ ...element, image });
    });
  };

  const handleClearImage = () => {
    commit({ ...element, image: emptyImage() });
  };

  const handlePickBackground = () => {
    openPicker((background) => {
      commit({ ...element, background });
    });
  };

  const handleClearBackground = () => {
    commit({ ...element, background: emptyImage() });
  };

  const elId = element.id ?? "";

  return (
    <section className={styles.section}>
      <h4 className={styles.heading}>This slide</h4>
      <ImagePicker
        label='Content image'
        image={element.image}
        seed={`${elId}-content`}
        onPick={handlePickImage}
        onClear={handleClearImage}
      />
      <ImagePicker
        label='Background image'
        image={element.background}
        seed={`${elId}-background`}
        onPick={handlePickBackground}
        onClear={handleClearBackground}
      />
    </section>
  );
};

const ThemePanel = () => {
  const {
    presets,
    themes,
    activeThemeId,
    customPresetActive,
    activatePreset,
    activateCustom,
    openEditor,
  } = useThemePicker();

  const options = [
    ...presets.map((preset) => ({
      value: `${PRESET_PREFIX}${preset.label}`,
      label: preset.label,
    })),
    ...themes
      .filter((t): t is ThemeResponse & { id: string } => !!t.id)
      .map((theme) => ({
        value: `${THEME_PREFIX}${theme.id}`,
        label: theme.name ?? "Untitled",
      })),
  ];

  // No clean preset match when the user has hand-tweaked hues away from any
  // preset — show the dropdown empty in that case so we don't misrepresent.
  let selected: string[] = [];
  if (activeThemeId) {
    selected = [`${THEME_PREFIX}${activeThemeId}`];
  } else if (!customPresetActive) {
    selected = [`${PRESET_PREFIX}Brand`];
  }

  const handleChange = (vals: string[]) => {
    const v = vals[0];
    if (!v) return;
    if (v.startsWith(PRESET_PREFIX)) {
      const label = v.slice(PRESET_PREFIX.length);
      const preset = presets.find((p) => p.label === label);
      if (preset) void activatePreset(preset);
      return;
    }
    if (v.startsWith(THEME_PREFIX)) {
      const id = v.slice(THEME_PREFIX.length);
      const theme = themes.find((t) => t.id === id);
      if (theme) void activateCustom(theme);
    }
  };

  return (
    <div className={styles.panel}>
      <section className={styles.section}>
        <h4 className={styles.heading}>Deck theme</h4>
        <Dropdown
          options={options}
          value={selected}
          onChange={handleChange}
          placeholder='Custom (hand-tweaked)'
        />
        <Btn
          type='button'
          className={styles.newBtn}
          onClick={() => {
            openEditor();
          }}>
          + New theme
        </Btn>
      </section>

      <PerSlideStyle />
    </div>
  );
};

export { ThemePanel };
