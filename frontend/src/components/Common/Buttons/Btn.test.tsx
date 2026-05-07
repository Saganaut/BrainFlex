// Unit tests for the Btn component — covers rendering, interaction, and prop-driven class/attribute behavior.
import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Btn } from "./Btn";

describe("Btn", () => {
  describe("rendering", () => {
    it("renders children", () => {
      render(<Btn>Click me</Btn>);
      expect(
        screen.getByRole("button", { name: "Click me" }),
      ).toBeInTheDocument();
    });

    it("defaults to type=button", () => {
      render(<Btn>Click me</Btn>);
      expect(screen.getByRole("button")).toHaveAttribute("type", "button");
    });

    it("applies a given type attribute", () => {
      render(<Btn type='submit'>Submit</Btn>);
      expect(screen.getByRole("button")).toHaveAttribute("type", "submit");
    });

    it("is not disabled by default", () => {
      render(<Btn>Click me</Btn>);
      expect(screen.getByRole("button")).not.toBeDisabled();
    });
  });

  describe("interaction", () => {
    it("calls onClick when clicked", async () => {
      const handleClick = vi.fn();
      render(<Btn onClick={handleClick}>Click me</Btn>);
      await userEvent.click(screen.getByRole("button"));
      expect(handleClick).toHaveBeenCalledTimes(1);
    });

    it("does not call onClick when disabled", async () => {
      const handleClick = vi.fn();
      render(
        <Btn disabled onClick={handleClick}>
          Disabled
        </Btn>,
      );
      await userEvent.click(screen.getByRole("button"));
      expect(handleClick).not.toHaveBeenCalled();
    });
  });

  describe("disabled", () => {
    it("sets the disabled attribute when disabled is true", () => {
      render(<Btn disabled>Disabled</Btn>);
      expect(screen.getByRole("button")).toBeDisabled();
    });
  });

  describe("icon", () => {
    it("renders icon in a span when provided", () => {
      render(<Btn icon={<svg data-testid='test-icon' />}>With Icon</Btn>);
      const icon = screen.getByTestId("test-icon");
      expect(icon.closest("span")).toBeInTheDocument();
    });

    it("does not render an icon span when no icon is provided", () => {
      render(<Btn>No Icon</Btn>);
      expect(screen.getByRole("button").querySelector("span")).toBeNull();
    });
  });

  describe("CSS classes", () => {
    it("applies the default variant class (primary)", () => {
      render(<Btn>Primary</Btn>);
      expect(screen.getByRole("button").className).toContain("primary");
    });

    it("applies the given variant class", () => {
      render(<Btn variant='error'>Error</Btn>);
      expect(screen.getByRole("button").className).toContain("error");
    });

    it("applies the default size class (md)", () => {
      render(<Btn>Default size</Btn>);
      expect(screen.getByRole("button").className).toContain("md");
    });

    it("applies the given size class", () => {
      render(<Btn size='lg'>Large</Btn>);
      expect(screen.getByRole("button").className).toContain("lg");
    });

    it("applies the given shape class", () => {
      render(<Btn shape='pill'>Pill</Btn>);
      expect(screen.getByRole("button").className).toContain("pill");
    });

    it("applies the given iconPosition class", () => {
      render(
        <Btn icon={<svg />} iconPosition='right'>
          Right icon
        </Btn>,
      );
      expect(screen.getByRole("button").className).toContain("right");
    });

    it("applies isDisabled class when disabled", () => {
      render(<Btn disabled>Disabled</Btn>);
      expect(screen.getByRole("button").className).toContain("isDisabled");
    });

    it("does not apply isDisabled class when enabled", () => {
      render(<Btn>Enabled</Btn>);
      expect(screen.getByRole("button").className).not.toContain("isDisabled");
    });

    it("applies withIcon class when an icon is provided", () => {
      render(<Btn icon={<svg />}>With Icon</Btn>);
      expect(screen.getByRole("button").className).toContain("withIcon");
    });

    it("does not apply withIcon class when no icon is provided", () => {
      render(<Btn>No Icon</Btn>);
      expect(screen.getByRole("button").className).not.toContain("withIcon");
    });
  });
});
