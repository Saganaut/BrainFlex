import { useEffect, useRef, useState } from "react";
import { useNavigate } from "@tanstack/react-router";
import {
  useCloseAccountMutation,
  useGetCurrentUserQuery,
  useUpdateProfileMutation,
  useUploadProfileImageMutation,
} from "../../store/BrainFlexApi";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import { apiBaseUrl } from "../../store/emptyApi";
import { ThemeSection } from "./ThemeSection";
import { OrgSection } from "./OrgSection";
import styles from "./AccountPage.module.css";
import { Btn } from "@components/Common/Buttons/Btn";
import { Checkbox } from "@/components/Common/Input/Checkbox";

const MAX_FILE_SIZE = 1024 * 1024;
const ACCEPTED_TYPES = ["image/jpeg", "image/png", "image/webp", "image/gif"];

const AccountPage = () => {
  const userState = useCurrentUser();
  const { refetch } = useGetCurrentUserQuery();
  const navigate = useNavigate();

  const registeredUser =
    userState.state === "registered" ? userState.user : null;

  const fileInputRef = useRef<HTMLInputElement>(null);
  const [pictureSuccess, setPictureSuccess] = useState(false);
  const [pictureError, setPictureError] = useState<string | null>(null);

  // Pending newsletter value: null means "use server value"; non-null means
  // the user has toggled it locally (optimistic update before the API responds).
  const [pendingNewsletter, setPendingNewsletter] = useState<boolean | null>(
    null,
  );
  const newsletter = pendingNewsletter ?? registeredUser?.newsletter ?? false;
  const [newsletterSuccess, setNewsletterSuccess] = useState(false);

  const [closeConfirm, setCloseConfirm] = useState(false);
  const [closeError, setCloseError] = useState<string | null>(null);

  const [updateProfile] = useUpdateProfileMutation();
  const [uploadProfileImage, { isLoading: isUploading }] =
    useUploadProfileImageMutation();
  const [closeAccount, { isLoading: isClosing }] = useCloseAccountMutation();

  useEffect(() => {
    if (userState.state !== "loading" && userState.state !== "registered") {
      void navigate({ to: "/" });
    }
  }, [userState.state, navigate]);

  if (userState.state === "loading")
    return <div className={styles.loading}>Loading...</div>;
  if (userState.state !== "registered") return null;

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (fileInputRef.current) fileInputRef.current.value = "";
    if (!file) return;

    setPictureError(null);
    setPictureSuccess(false);

    if (!ACCEPTED_TYPES.includes(file.type)) {
      setPictureError(
        "Invalid file type. Please upload a JPEG, PNG, WebP, or GIF.",
      );
      return;
    }
    if (file.size > MAX_FILE_SIZE) {
      setPictureError("Image is too large. Maximum size is 1 MB.");
      return;
    }

    // Build FormData so fetchBaseQuery sends multipart/form-data with the
    // correct boundary instead of JSON-serializing the body.
    const formData = new FormData();
    formData.append("image", file);

    try {
      await uploadProfileImage({
        body: formData as unknown as { image: Blob },
      }).unwrap();
      setPictureSuccess(true);
      await refetch();
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "data" in err
          ? String(err.data)
          : null;
      setPictureError(msg ?? "Upload failed. Please try again.");
    }
  };

  const handleNewsletterChange = async (checked: boolean) => {
    setPendingNewsletter(checked);
    setNewsletterSuccess(false);
    try {
      await updateProfile({
        updateProfileRequest: { newsletter: checked },
      }).unwrap();
      setNewsletterSuccess(true);
      setPendingNewsletter(null); // revert to server value (which now matches)
    } catch {
      setPendingNewsletter(null); // revert optimistic update on failure
    }
  };

  const handleCloseAccount = async () => {
    setCloseError(null);
    try {
      await closeAccount().unwrap();
      await fetch(`${apiBaseUrl}/api/auth/logout`, {
        method: "POST",
        credentials: "include",
      });
      window.location.href = "/";
    } catch {
      setCloseError("Failed to close account. Please try again.");
    }
  };

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Account Settings</h1>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>Profile Picture</h2>
        {registeredUser?.pictureUrl && (
          <img
            src={registeredUser.pictureUrl}
            alt='Profile picture'
            className={styles.avatar}
          />
        )}
        <input
          ref={fileInputRef}
          type='file'
          accept='image/jpeg,image/png,image/webp,image/gif'
          onChange={(e) => {
            void handleFileChange(e);
          }}
          className={styles.fileInputHidden}
          aria-label='Upload profile picture'
        />
        <Btn
          onClick={() => fileInputRef.current?.click()}
          disabled={isUploading}>
          {isUploading ? "Uploading..." : "Upload new photo"}
        </Btn>
        <p className={styles.uploadHint}>
          JPEG, PNG, WebP or GIF · max 1 MB · resized to 500×500
        </p>
        {pictureSuccess && (
          <p className={styles.success}>Profile picture updated.</p>
        )}
        {pictureError && <p className={styles.error}>{pictureError}</p>}
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>Newsletter</h2>
        <label className={styles.checkboxLabel}>
          <Checkbox
            checked={newsletter}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
              void handleNewsletterChange(e.target.checked);
            }}
          />
          Receive newsletter emails
        </label>
        {newsletterSuccess && (
          <p className={styles.success}>Preference saved.</p>
        )}
      </section>

      <ThemeSection />

      <OrgSection />

      <section className={`${styles.section} ${styles.dangerSection}`}>
        <h2 className={styles.sectionTitle}>Close Account</h2>
        <p className={styles.dangerText}>
          Closing your account is permanent. Your account will be deactivated
          and you will be logged out.
        </p>
        {!closeConfirm ? (
          <Btn
            className={styles.dangerBtn}
            onClick={() => {
              setCloseConfirm(true);
            }}>
            Close my account
          </Btn>
        ) : (
          <div className={styles.confirmBox}>
            <p>Are you sure? This cannot be undone.</p>
            <div className={styles.confirmActions}>
              <Btn
                className={styles.dangerBtn}
                onClick={() => void handleCloseAccount()}
                disabled={isClosing}>
                {isClosing ? "Closing..." : "Yes, close my account"}
              </Btn>
              <Btn
                onClick={() => {
                  setCloseConfirm(false);
                }}>
                Cancel
              </Btn>
            </div>
          </div>
        )}
        {closeError && <p className={styles.error}>{closeError}</p>}
      </section>
    </div>
  );
};

export { AccountPage };
