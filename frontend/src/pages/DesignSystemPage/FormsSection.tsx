// Form components showcase for the design system page.
// Shows all Common/Form primitives (Input, TextArea, Checkbox, Radio, HuePicker)
// with controlled state so they are actually interactive.
import { useState } from "react";
import { Input } from "../../components/Common/Input/Input";
import { TextArea } from "../../components/Common/Input/TextArea";
import { Checkbox } from "../../components/Common/Input/Checkbox";
import { Radio } from "../../components/Common/Input/Radio";
import { HuePicker } from "../../components/Common/Input/HuePicker";
import { Accordion } from "../../components/Containers/Accordion";
import styles from "./DesignSystem.module.css";

const FormsSection = () => {
  const [hue, setHue] = useState(260);

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
            <Radio label='Game Mode' />
          </div>

          <h4>Hue Picker</h4>
          <div className={styles.formHueRow}>
            <HuePicker label='Color' value={hue} onChange={setHue} />
          </div>
        </Accordion>
      </div>
    </section>
  );
};

export { FormsSection };
