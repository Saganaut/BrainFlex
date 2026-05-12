// Unit tests for the Btn component — covers rendering, interaction, and prop-driven
// data-attribute + className behavior. Variants/sizes are asserted on data-* attrs
// because that's the contract that drives styling (see frontend/STYLES.md).
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

    it("sets data-icon-position when an icon is provided", () => {
      render(
        <Btn icon={<svg />} iconPosition='right'>
          Right icon
        </Btn>,
      );
      expect(screen.getByRole("button")).toHaveAttribute(
        "data-icon-position",
        "right",
      );
    });

    it("does not set data-icon-position without an icon", () => {
      render(<Btn>No Icon</Btn>);
      expect(screen.getByRole("button")).not.toHaveAttribute(
        "data-icon-position",
      );
    });
  });

  describe("data-attribute modifiers", () => {
    it("defaults to data-variant=default", () => {
      render(<Btn>Default</Btn>);
      expect(screen.getByRole("button")).toHaveAttribute(
        "data-variant",
        "default",
      );
    });

    it("applies the given variant", () => {
      render(<Btn variant='error'>Error</Btn>);
      expect(screen.getByRole("button")).toHaveAttribute(
        "data-variant",
        "error",
      );
    });

    it("defaults to data-size=md", () => {
      render(<Btn>Default size</Btn>);
      expect(screen.getByRole("button")).toHaveAttribute("data-size", "md");
    });

    it("applies the given size", () => {
      render(<Btn size='lg'>Large</Btn>);
      expect(screen.getByRole("button")).toHaveAttribute("data-size", "lg");
    });

    it("applies the given mode", () => {
      render(<Btn mode='outline'>Outline</Btn>);
      expect(screen.getByRole("button")).toHaveAttribute("data-mode", "outline");
    });

    it("does not set data-mode by default", () => {
      render(<Btn>Default</Btn>);
      expect(screen.getByRole("button")).not.toHaveAttribute("data-mode");
    });
  });

  describe("shape", () => {
    it("applies the given shape class", () => {
      render(<Btn shape='pill'>Pill</Btn>);
      expect(screen.getByRole("button").className).toContain("pill");
    });

    it("does not apply a shape class for default shape", () => {
      render(<Btn>Default shape</Btn>);
      expect(screen.getByRole("button").className).not.toContain("pill");
    });
  });
});
