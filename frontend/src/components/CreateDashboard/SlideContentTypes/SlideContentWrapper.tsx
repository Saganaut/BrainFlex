import React, { type ReactNode } from "react";

interface SlideContentWrapperProps {
  children: ReactNode;
}

const SlideContentWrapper: React.FC<SlideContentWrapperProps> = ({
  children,
}) => {
  return <div>{children}</div>;
};

export { SlideContentWrapper };
