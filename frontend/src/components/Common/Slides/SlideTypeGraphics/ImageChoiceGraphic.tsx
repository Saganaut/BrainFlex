// Decorative thumbnail icon for the ImageChoiceQuestion element kind: a 2x2
// grid of image tiles with the selected tile highlighted with a checkmark.
const ImageChoiceGraphic = () => {
  return (
    <div>
      <svg
        width='70'
        height='70'
        viewBox='0 0 70 70'
        fill='none'
        xmlns='http://www.w3.org/2000/svg'>
        <rect
          x='6'
          y='6'
          width='27'
          height='27'
          rx='4'
          fill='#54FFF1'
          opacity='0.5'
        />
        <rect
          x='6'
          y='37'
          width='27'
          height='27'
          rx='4'
          fill='#54FFF1'
          opacity='0.5'
        />
        <rect
          x='37'
          y='6'
          width='27'
          height='27'
          rx='4'
          fill='#54FFF1'
          opacity='0.5'
        />
        <circle cx='15' cy='15' r='3' fill='#54FFF1' />
        <path d='M9 30 L17 20 L25 30 Z' fill='#54FFF1' />
        <circle cx='46' cy='15' r='3' fill='#54FFF1' />
        <path d='M40 30 L48 20 L56 30 Z' fill='#54FFF1' />
        <circle cx='15' cy='46' r='3' fill='#54FFF1' />
        <path d='M9 61 L17 51 L25 61 Z' fill='#54FFF1' />
        <rect x='37' y='37' width='27' height='27' rx='4' fill='#6019FF' />
        <path
          d='M43 51 L48 56 L57 45'
          stroke='#54FFF1'
          strokeWidth='3.5'
          fill='none'
          strokeLinecap='round'
          strokeLinejoin='round'
        />
      </svg>
    </div>
  );
};

export { ImageChoiceGraphic };
