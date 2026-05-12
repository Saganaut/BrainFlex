// Form components showcase for the design system page.
// Shows all Common/Form primitives (Input, TextArea, Checkbox, Radio, RadioGroup, HuePicker)
// with controlled state so they are actually interactive.
import { useState } from "react";
import { Input } from "../../components/Common/Input/Input";
import { TextArea } from "../../components/Common/Input/TextArea";
import { Checkbox } from "../../components/Common/Input/Checkbox";
import { RadioGroup } from "../../components/Common/Input/RadioGroup";
import { HuePicker } from "../../components/Common/Input/HuePicker";
import { RichTextInput } from "../../components/Common/Input/RichTextInput";
import { Accordion } from "../../components/Containers/Accordion";
import styles from "./DesignSystem.module.css";

const GAME_MODE_OPTIONS: { value: string; label: string }[] = [
  { value: "solo", label: "Solo" },
  { value: "team", label: "Team" },
  { value: "tournament", label: "Tournament" },
];

const FormsSection = () => {
  const [hue, setHue] = useState(260);
  const [gameMode, setGameMode] = useState("solo");
  const [richText, setRichText] = useState(
    "<p>Click anywhere to <strong>edit</strong> — try the toolbar.</p>",
  );

  return (
    <section>
      <div className={styles.examplesContainer}>
        <Accordion titleBar='Form Inputs'>
          <h4>Text Input</h4>
          <div className={styles.formExampleRow}>
            <Input
              label='Username'
              id='ds-input'
              value={"input"}
              placeholder='e.g. GandalfTheGrey'
            />
            <Input
              label='Disabled'
              id='ds-input-disabled'
              value='frodo_baggins'
              disabled
            />
          </div>

          <h4>Text Area</h4>
          <div className={styles.formExampleRow}>
            <TextArea
              label='Biography'
              id='ds-textarea'
              placeholder='Tell the Fellowship about yourself...'
              rows={3}
            />
          </div>

          <h4>Checkbox</h4>
          <div className={styles.formExampleRow}>
            <Checkbox label='Subscribe to newsletter' id='ds-checkbox-1' />
            <Checkbox label='Accept terms & conditions' id='ds-checkbox-2' />
            <Checkbox
              label='Disabled'
              id='ds-checkbox-3'
              checked={true}
              disabled
            />
          </div>

          <h4>Radio Group</h4>
          <div className={styles.formExampleRow}>
            <RadioGroup
              name='game-mode'
              legend='Game Mode'
              options={GAME_MODE_OPTIONS}
              value={gameMode}
              onChange={setGameMode}
            />
            <RadioGroup
              name='game-mode-disabled'
              legend='Disabled'
              options={GAME_MODE_OPTIONS}
              value='team'
              onChange={() => { /* disabled */ }}
              disabled
            />
          </div>

          <h4>Hue Picker</h4>
          <div className={styles.formHueRow}>
            <HuePicker label='Color' value={hue} onChange={setHue} />
          </div>

          <h4>Rich Text Input</h4>
          <div className={styles.formExampleRow}>
            <RichTextInput
              label='Question'
              id='ds-rich-text'
              placeholder='Type your question…'
              value={richText}
              onChange={setRichText}
            />
          </div>
        </Accordion>
      </div>
    </section>
  );
};

export { FormsSection };
