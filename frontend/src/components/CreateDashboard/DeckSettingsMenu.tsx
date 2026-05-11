import { Link } from "@tanstack/react-router";
import { IconBtn } from "../Common/Buttons/IconBtn";
import {
  DropdownMenu,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuLink,
} from "../Menus/DropdownMenu";
const DeckSettingsMenu = () => {
  return (
    <DropdownMenu
      trigger={(toggle) => (
        <IconBtn
          type='avatar'
          shape='round'
          bordered={true}
          backgroundColor={true}
          size='md'
          aria-label='User menu'
          icon={
            <svg
              xmlns='http://www.w3.org/2000/svg'
              fill='none'
              viewBox='0 0 24 24'
              strokeWidth={1.5}
              stroke='currentColor'
              className='size-6'>
              <path
                strokeLinecap='round'
                strokeLinejoin='round'
                d='M3.75 9h16.5m-16.5 6.75h16.5'
              />
            </svg>
          }
          onClick={toggle}
        />
      )}
      align='right'>
      <>
        <DropdownMenuLabel>hi</DropdownMenuLabel>
        <DropdownMenuLink>
          <Link to='/account'>Account</Link>
        </DropdownMenuLink>
        <DropdownMenuItem
          onClick={() => {
            console.log("click");
          }}>
          Logout
        </DropdownMenuItem>
      </>
    </DropdownMenu>
  );
};

export { DeckSettingsMenu };
