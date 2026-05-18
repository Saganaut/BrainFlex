// Reference catalog organized by variant name. Each section is one variant
// from BtnTypes.ts and renders a live Btn + IconBtn (where applicable) so
// the page itself is the source of truth for what the variant looks like.
// The rule + naming convention is in STYLE-RULES.md "Named button +
// icon-button variants".
import type { ReactNode } from "react";
import {
  BellIcon,
  TrashIcon,
  StarIcon,
  HeartIcon,
  PencilSquareIcon,
  Cog6ToothIcon,
  UserIcon,
  MagnifyingGlassIcon,
} from "@heroicons/react/24/outline";
import type { BtnVariant } from "../../components/Common/Buttons/BtnTypes";
import { Btn } from "../../components/Common/Buttons/Btn";
import { IconBtn } from "../../components/Common/Buttons/IconBtn";
import styles from "./DesignSystem.module.css";

interface VariantSectionProps {
  name: BtnVariant;
  alias?: string;
  description: string;
  tokens: string[];
  children: ReactNode;
}

const VariantSection = ({
  name,
  alias,
  description,
  tokens,
  children,
}: VariantSectionProps) => {
  return (
    <div className={styles.combinationGroup}>
      <h4 className={styles.combinationGroupTitle}>
        <code>{name}</code>
        {alias && ` — also called "${alias}"`}
      </h4>
      <p className={styles.sectionDescription}>{description}</p>
      <div className={styles.combinationGrid}>
        <div className={styles.combinationActionCard}>
          <div className={styles.combinationActionSlot}>{children}</div>
          <div className={styles.combinationTokens}>
            {tokens.map((t) => (
              <div key={t}>{t}</div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

const ActionCombinations = () => {
  return (
    <>
      <VariantSection
        name='primary'
        description='Default for both Btn and IconBtn. Filled neutral CTA — the workhorse button.'
        tokens={["--bg-secondary", "--text-primary"]}>
        <Btn variant='primary'>Save deck</Btn>
        <Btn variant='primary' size='sm'>
          Save deck
        </Btn>
        <IconBtn variant='primary' icon={<BellIcon />} />
        <IconBtn variant='primary' icon={<Cog6ToothIcon />} shape='round' />
      </VariantSection>

      <VariantSection
        name='ghost'
        description='Transparent background AND transparent border. Use for tertiary actions and toolbar icons where the affordance is implicit.'
        tokens={["transparent bg + border", "--text-primary"]}>
        <Btn variant='ghost'>Learn more</Btn>
        <Btn variant='ghost' size='sm'>
          Learn more
        </Btn>
        <IconBtn variant='ghost' icon={<BellIcon />} />
        <IconBtn variant='ghost' icon={<MagnifyingGlassIcon />} size='sm' />
      </VariantSection>

      <VariantSection
        name='bordered'
        description='Transparent bg, 2px text-primary border. Use for secondary actions and toolbar icons that need a visible affordance without a fill.'
        tokens={["transparent bg", "--text-primary border + text"]}>
        <Btn variant='bordered'>Cancel</Btn>
        <Btn variant='bordered' size='sm'>
          Cancel
        </Btn>
        <IconBtn variant='bordered' icon={<StarIcon />} />
        <IconBtn variant='bordered' icon={<PencilSquareIcon />} shape='round' />
      </VariantSection>

      <VariantSection
        name='filled'
        description='Explicit alias for "primary". Use when the call site wants to emphasize "this is the filled treatment" — also pairs with shape="avatar" for IconBtn.'
        tokens={["--bg-secondary", "--text-primary"]}>
        <Btn variant='filled'>Save deck</Btn>
        <IconBtn variant='filled' icon={<BellIcon />} />
        <IconBtn variant='filled' icon={<UserIcon />} shape='avatar' />
      </VariantSection>

      <VariantSection
        name='close'
        description='IconBtn only. Renders an XMarkIcon on a transparent surface — the icon prop is ignored. Use for modal/toast/banner dismiss.'
        tokens={["transparent bg", "--text-primary X-mark"]}>
        <IconBtn variant='close' />
        <IconBtn variant='close' size='sm' />
        <IconBtn variant='close' size='xs' />
      </VariantSection>

      <VariantSection
        name='error'
        alias='destructive'
        description='Destructive action. Filled red, both Btn and IconBtn. Reserve for permanent-removal actions; pair with a confirm dialog for anything irreversible.'
        tokens={["--bg-error", "--text-error", "--border-error"]}>
        <Btn variant='error'>Delete</Btn>
        <Btn variant='error' size='sm'>
          Delete
        </Btn>
        <IconBtn variant='error' icon={<TrashIcon />} />
        <IconBtn variant='error' icon={<TrashIcon />} size='sm' />
      </VariantSection>

      <VariantSection
        name='delete'
        alias='error'
        description='Semantic alias for error. Reads more naturally at call sites that perform a delete ("Delete user", "Remove deck") — visually identical to error.'
        tokens={["--bg-error", "--text-error", "--border-error"]}>
        <Btn variant='delete'>Delete user</Btn>
        <Btn variant='delete' size='sm'>
          Delete user
        </Btn>
        <IconBtn variant='delete' icon={<TrashIcon />} />
        <IconBtn variant='delete' icon={<TrashIcon />} size='sm' />
      </VariantSection>

      <VariantSection
        name='success'
        description='Confirmation of a completed operation. Green filled treatment.'
        tokens={["--bg-success", "--text-success", "--border-success"]}>
        <Btn variant='success'>Confirm</Btn>
        <Btn variant='success' size='sm'>
          Confirm
        </Btn>
        <IconBtn variant='success' icon={<HeartIcon />} />
        <IconBtn variant='success' icon={<HeartIcon />} size='sm' />
      </VariantSection>

      <VariantSection
        name='warning'
        description='Proceed-with-caution action. Yellow filled treatment.'
        tokens={["--bg-warning", "--text-warning", "--border-warning"]}>
        <Btn variant='warning'>Proceed</Btn>
        <Btn variant='warning' size='sm'>
          Proceed
        </Btn>
        <IconBtn variant='warning' icon={<MagnifyingGlassIcon />} />
        <IconBtn variant='warning' icon={<MagnifyingGlassIcon />} size='sm' />
      </VariantSection>

      <VariantSection
        name='info'
        description='Informational / "read more" action. Blue filled treatment.'
        tokens={["--bg-info", "--text-info", "--border-info"]}>
        <Btn variant='info'>Read more</Btn>
        <Btn variant='info' size='sm'>
          Read more
        </Btn>
        <IconBtn variant='info' icon={<PencilSquareIcon />} />
        <IconBtn variant='info' icon={<PencilSquareIcon />} size='sm' />
      </VariantSection>

      <VariantSection
        name='brand'
        description='Brand-colored CTA. Use sparingly — one per view at most.'
        tokens={["--bg-brand", "--text-on-brand", "--border-brand"]}>
        <Btn variant='brand'>Upgrade</Btn>
        <Btn variant='brand' size='sm'>
          Upgrade
        </Btn>
        <IconBtn variant='brand' icon={<StarIcon />} />
        <IconBtn variant='brand' icon={<StarIcon />} size='sm' />
      </VariantSection>
    </>
  );
};

export { ActionCombinations };
