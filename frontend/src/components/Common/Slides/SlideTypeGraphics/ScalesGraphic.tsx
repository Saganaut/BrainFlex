// Decorative thumbnail icon for the ScalesQuestion element kind: a horizontal
// slider track with a knob, evoking a Likert-style rating scale.
const ScalesGraphic = () => {
  return (
    <div>
      <svg
        width='70'
        height='70'
        viewBox='0 0 70 70'
        fill='none'
        xmlns='http://www.w3.org/2000/svg'>
        <line
          x1='10'
          y1='20'
          x2='10'
          y2='26'
          stroke='#54FFF1'
          strokeWidth='2.5'
          strokeLinecap='round'
          opacity='0.7'
        />
        <line
          x1='24'
          y1='20'
          x2='24'
          y2='26'
          stroke='#54FFF1'
          strokeWidth='2.5'
          strokeLinecap='round'
          opacity='0.7'
        />
        <line
          x1='38'
          y1='20'
          x2='38'
          y2='26'
          stroke='#54FFF1'
          strokeWidth='2.5'
          strokeLinecap='round'
          opacity='0.7'
        />
        <line
          x1='52'
          y1='20'
          x2='52'
          y2='26'
          stroke='#54FFF1'
          strokeWidth='2.5'
          strokeLinecap='round'
          opacity='0.7'
        />
        <rect
          x='6'
          y='33'
          width='58'
          height='6'
          rx='3'
          fill='#54FFF1'
          opacity='0.5'
        />
        <rect x='6' y='33' width='32' height='6' rx='3' fill='#6019FF' />
        <circle
          cx='38'
          cy='36'
          r='10'
          fill='#54FFF1'
          stroke='#6019FF'
          strokeWidth='3'
        />
        <circle cx='12' cy='52' r='2.5' fill='#54FFF1' opacity='0.7' />
        <circle cx='58' cy='52' r='2.5' fill='#54FFF1' opacity='0.7' />
      </svg>
    </div>
  );
};

export { ScalesGraphic };
