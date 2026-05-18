// Decorative thumbnail icon for the NumberQuestion element kind (numeric
// answer). The hash symbol leans on the universal "#" / number-sign metaphor.
const NumberGraphic = () => {
  return (
    <svg
      width='70'
      height='70'
      viewBox='0 0 70 70'
      fill='none'
      xmlns='http://www.w3.org/2000/svg'>
      <rect
        x='6'
        y='8'
        width='58'
        height='54'
        rx='6'
        fill='#54FFF1'
        opacity='0.6'
      />
      <line
        x1='28'
        y1='16'
        x2='24'
        y2='54'
        stroke='#6019FF'
        strokeWidth='4.5'
        strokeLinecap='round'
      />
      <line
        x1='48'
        y1='16'
        x2='44'
        y2='54'
        stroke='#6019FF'
        strokeWidth='4.5'
        strokeLinecap='round'
      />
      <line
        x1='16'
        y1='28'
        x2='56'
        y2='28'
        stroke='#6019FF'
        strokeWidth='4.5'
        strokeLinecap='round'
      />
      <line
        x1='14'
        y1='42'
        x2='54'
        y2='42'
        stroke='#6019FF'
        strokeWidth='4.5'
        strokeLinecap='round'
      />
    </svg>
  );
};

export { NumberGraphic };
