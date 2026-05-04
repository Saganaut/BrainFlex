import styles from "./Common.module.css";

type BadgeType = "error" | "success" | "warning" | "info";
type BadgeSize = "sm" | "md" | "lg";
interface BadgeProps {
  label: string;
  variant?: BadgeType;
  size?: BadgeSize;
}

const Badge = ({ label, size = "md", variant = "info" }: BadgeProps) => {
  return (
    <span
      className={`${styles.badge} 
    ${styles[variant]} ${styles[size]}`}>
      {label}
    </span>
  );
};

export { Badge };
