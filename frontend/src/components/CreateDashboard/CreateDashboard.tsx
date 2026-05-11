import styles from "./CreateDashboard.module.css";

const CreateDashboard = () => {
  return (
    <div className={styles.createDashboard}>
      {/* Dashboard nav and control bar 
        Deck
        
      */}

      <div className={styles.navbar}>Navbar</div>
      {/* Canvas */}

      <div className={styles.mainCanvas}>
        {/* Sidebar right - displays all questions/slides*/}
        <div className={styles.rightSidebar}>right sidebar</div>
        {/* Editable slide/*/}

        <div className={styles.slides}>Slides</div>
        {/* Sidebar Left */}

        <div className={styles.leftSidebar}>Left sidebar</div>
      </div>
    </div>
  );
};

export { CreateDashboard };
