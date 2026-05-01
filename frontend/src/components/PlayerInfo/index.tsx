import React, { useState } from "react";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import styles from "./PlayerInfo.module.css";
import { camelToNormalCase } from "../../utils/utils";
import { CollapseBtn } from "../Common/Buttons/CollapseBtn";

interface PlayerInfoProps {}

const StatRow = ({ label, stat }: { label: string; stat: number }) => {
  return (
    <div className={styles.statRow}>
      <dt>{camelToNormalCase(label)}</dt>
      <dd>{stat}</dd>
    </div>
  );
};

const PlayerInfo: React.FC<PlayerInfoProps> = ({}) => {
  const { user, isLoading, isError } = useCurrentUser();

  const [isCollapsed, collapse] = useState(false);
  if (isLoading) return <div> Loading...</div>;
  if (isError) return <div> Error...</div>;
  if (user == undefined) return <div> No user </div>;

  const statsArray = Object.entries(user.stats ?? {});
  console.log("user", user);
  //TODO: Either save profile images to server or cache google images
  return (
    <div className={styles.playerInfoContainer}>
      <div className={styles.playerInfo}>
        <div className={styles.header}>
          <div className={styles.imgWrapper}>
            {/* <img src={user.pictureUrl} /> */}
            <img src='https://i.pravatar.cc/50' />
          </div>
          <div>
            <h5>{user.userName}</h5>
            {/* {isRegisteredUser(user) && (
              <>
                <p> {user.name} </p>
                <p> {user.email} </p>{" "}
              </>
            )} */}
            <CollapseBtn collapse={collapse} isCollapsed={isCollapsed} />
          </div>
        </div>

        <div className={styles.body}>
          <div
            className={` ${styles.playerStats} ${isCollapsed ? styles.isCollapsed : ""} `}>
            {statsArray.map(([key, value]) => (
              <StatRow label={key} stat={value} />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export { PlayerInfo };
