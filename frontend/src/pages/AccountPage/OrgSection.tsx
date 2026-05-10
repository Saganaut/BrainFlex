// Organization management: create, join by ID, view current, or leave.
import { useState } from "react";
import {
  useCreateOrgMutation,
  useGetMyOrgQuery,
  useJoinOrgMutation,
  useLeaveOrgMutation,
} from "../../store/BrainFlexApi";
import styles from "./ThemeSection.module.css";
import accountStyles from "./AccountPage.module.css";
import { Btn } from "@/components/Common/Buttons/Btn";
import { Input } from "@/components/Common/Input/Input";

const OrgSection = () => {
  const { data: org, refetch, isLoading } = useGetMyOrgQuery();

  const [createOrg, { isLoading: isCreating }] = useCreateOrgMutation();
  const [joinOrg, { isLoading: isJoining }] = useJoinOrgMutation();
  const [leaveOrg, { isLoading: isLeaving }] = useLeaveOrgMutation();

  const [createName, setCreateName] = useState("");
  const [joinId, setJoinId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [leaveConfirm, setLeaveConfirm] = useState(false);
  const [copied, setCopied] = useState(false);

  const handleCreate = async () => {
    if (!createName.trim()) {
      setError("Organization name is required.");
      return;
    }
    setError(null);
    try {
      await createOrg({
        createOrganizationRequest: { name: createName.trim() },
      }).unwrap();
      setCreateName("");
      await refetch();
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "data" in err
          ? String(err.data)
          : null;
      setError(msg ?? "Failed to create organization.");
    }
  };

  const handleJoin = async () => {
    if (!joinId.trim()) {
      setError("Organization ID is required.");
      return;
    }
    setError(null);
    try {
      await joinOrg({
        joinOrganizationRequest: { organizationId: joinId.trim() },
      }).unwrap();
      setJoinId("");
      await refetch();
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "data" in err
          ? String(err.data)
          : null;
      setError(msg ?? "Organization not found.");
    }
  };

  const handleLeave = async () => {
    setError(null);
    try {
      await leaveOrg().unwrap();
      setLeaveConfirm(false);
      await refetch();
    } catch {
      setError("Failed to leave organization.");
    }
  };

  const handleCopyId = async () => {
    if (!org?.id) return;
    await navigator.clipboard.writeText(org.id);
    setCopied(true);
    setTimeout(() => {
      setCopied(false);
    }, 2000);
  };

  if (isLoading) return null;

  return (
    <section className={accountStyles.section}>
      <h2 className={accountStyles.sectionTitle}>Organization</h2>

      {org ? (
        <div className={styles.orgInfo}>
          <p className={styles.orgName}>{org.name}</p>
          <p className={styles.orgId}>ID: {org.id}</p>
          <div style={{ display: "flex", gap: "var(--space-3)" }}>
            <Btn
              onClick={() => {
                void handleCopyId();
              }}>
              {copied ? "Copied!" : "Copy ID"}
            </Btn>
            {!leaveConfirm ? (
              <Btn
                onClick={() => {
                  setLeaveConfirm(true);
                }}>
                Leave organization
              </Btn>
            ) : (
              <>
                <Btn
                  onClick={() => {
                    void handleLeave();
                  }}
                  disabled={isLeaving}>
                  {isLeaving ? "Leaving..." : "Confirm leave"}
                </Btn>
                <Btn
                  onClick={() => {
                    setLeaveConfirm(false);
                  }}>
                  Cancel
                </Btn>
              </>
            )}
          </div>
        </div>
      ) : (
        <div className={styles.orgJoinForm}>
          <div className={styles.orgRow}>
            <Input
              type='text'
              className={styles.orgInput}
              placeholder='Organization name'
              value={createName}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                setCreateName(e.target.value);
              }}
              maxLength={128}
              aria-label='New organization name'
            />
            <Btn
              onClick={() => {
                void handleCreate();
              }}
              disabled={isCreating}>
              {isCreating ? "Creating..." : "Create"}
            </Btn>
          </div>

          <div className={styles.orgDivider}>
            <span className={styles.orgDividerText}>
              or join an existing one
            </span>
          </div>

          <div className={styles.orgRow}>
            <Input
              type='text'
              className={styles.orgInput}
              placeholder='Organization ID'
              value={joinId}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                setJoinId(e.target.value);
              }}
              aria-label='Organization ID to join'
            />
            <Btn
              onClick={() => {
                void handleJoin();
              }}
              disabled={isJoining}>
              {isJoining ? "Joining..." : "Join"}
            </Btn>
          </div>
        </div>
      )}

      {error && <p className={accountStyles.error}>{error}</p>}
    </section>
  );
};

export { OrgSection };
