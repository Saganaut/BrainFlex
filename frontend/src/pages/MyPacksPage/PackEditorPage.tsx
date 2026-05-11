// Pack editor: create a new content pack or edit an existing one's metadata and questions.
// In create mode, saving metadata creates the pack then redirects to edit mode for questions.
// A ?returnTo query param redirects back to the caller (e.g. game creation) after creating.
import { useState } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import {
  useGetDeckQuery,
  useCreateDeckMutation,
  useUpdateDeckMutation,
  useListQuestionsQuery,
  useAddQuestionMutation,
  useUpdateQuestionMutation,
  useDeleteQuestionMutation,
} from "../../store/BrainFlexApi";
import type {
  DeckDto,
  QuestionEditorDto,
  UpsertQuestionRequest,
} from "../../store/BrainFlexApi";
import { Btn } from "@/components/Common/Buttons/Btn";
import { Input } from "@/components/Common/Input/Input";
import { TextArea } from "@/components/Common/Input/TextArea";
import { extractErrorMessage } from "../../utils/utils";
import styles from "./PackEditorPage.module.css";

interface PackEditorPageProps {
  packId?: string;
  returnTo?: string;
}

// ─── Question form ────────────────────────────────────────────────────────────

interface QuestionFormProps {
  initial: UpsertQuestionRequest;
  onSave: (q: UpsertQuestionRequest) => void;
  onCancel: () => void;
  saving: boolean;
  error: string | null;
}

type QuestionKind = NonNullable<UpsertQuestionRequest["type"]>;

const QuestionForm = ({
  initial,
  onSave,
  onCancel,
  saving,
  error,
}: QuestionFormProps) => {
  const [type, setType] = useState<QuestionKind>(initial.type ?? "MULTIPLE_CHOICE");
  const [questionText, setQuestionText] = useState(initial.questionText ?? "");
  const [options, setOptions] = useState<string[]>(initial.options ?? ["", "", "", ""]);
  const [correctAnswer, setCorrectAnswer] = useState(
    initial.correctAnswer ?? 0,
  );
  const [correctAnswerText, setCorrectAnswerText] = useState(
    initial.correctAnswerText ?? "",
  );
  const [pointValue, setPointValue] = useState(initial.pointValue ?? 100);
  const [timeLimit, setTimeLimit] = useState(initial.timeLimit ?? 15);
  const [difficulty, setDifficulty] = useState<
    UpsertQuestionRequest["difficulty"]
  >(initial.difficulty ?? "MEDIUM");

  const setOption = (i: number, value: string) => {
    setOptions((prev) => prev.map((o, idx) => (idx === i ? value : o)));
  };

  const addOption = () => {
    if (options.length >= 4) return;
    setOptions((prev) => [...prev, ""]);
  };

  const removeOption = (i: number) => {
    if (options.length <= 2) return;
    const next = options.filter((_, idx) => idx !== i);
    setOptions(next);
    if (correctAnswer >= next.length) setCorrectAnswer(next.length - 1);
  };

  const isTextInput = type === "TEXT_INPUT";
  const valid =
    questionText.trim().length > 0 &&
    (isTextInput
      ? correctAnswerText.trim().length > 0
      : options.every((o) => o.trim().length > 0) &&
        correctAnswer < options.length);

  const handleSave = () => {
    const base = {
      type,
      questionText,
      pointValue,
      timeLimit,
      difficulty,
    };
    onSave(
      isTextInput
        ? { ...base, correctAnswerText: correctAnswerText.trim() }
        : { ...base, options, correctAnswer },
    );
  };

  return (
    <div className={styles.questionForm}>
      <h3 className={styles.questionFormTitle}>
        {initial.questionText ? "Edit Question" : "New Question"}
      </h3>

      <div className={styles.fieldGroup}>
        <label className={styles.label}>Question type</label>
        <select
          value={type}
          onChange={(e) => {
            setType(e.target.value as QuestionKind);
          }}>
          <option value='MULTIPLE_CHOICE'>Multiple choice</option>
          <option value='TEXT_INPUT'>Type in</option>
        </select>
      </div>

      <div className={styles.fieldGroup}>
        <label className={styles.label}>Question text</label>
        <TextArea
          value={questionText}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => {
            setQuestionText(e.target.value);
          }}
          placeholder={
            isTextInput
              ? "How many planets are in our solar system?"
              : "What is the capital of Australia?"
          }
          rows={2}
        />
      </div>

      {isTextInput ? (
        <div className={styles.fieldGroup}>
          <label className={styles.label}>
            Correct answer — players' input is matched case-insensitively
          </label>
          <Input
            value={correctAnswerText}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
              setCorrectAnswerText(e.target.value);
            }}
            placeholder='8'
            maxLength={200}
          />
        </div>
      ) : (
        <div className={styles.fieldGroup}>
          <label className={styles.label}>
            Answer options — select the correct one
          </label>
          {options.map((opt, i) => (
            // eslint-disable-next-line react-x/no-array-index-key -- answer options are positional; index is the identity
            <div key={i} className={styles.optionRow}>
              <input
                type='radio'
                name='correctAnswer'
                checked={correctAnswer === i}
                onChange={() => {
                  setCorrectAnswer(i);
                }}
                aria-label={`Mark option ${i + 1} as correct`}
              />
              <Input
                value={opt}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                  setOption(i, e.target.value);
                }}
                placeholder={`Option ${i + 1}`}
              />
              {options.length > 2 && (
                <Btn
                  size='sm'
                  variant='error'
                  type='button'
                  onClick={() => {
                    removeOption(i);
                  }}
                  aria-label={`Remove option ${i + 1}`}>
                  ✕
                </Btn>
              )}
            </div>
          ))}
          {options.length < 4 && (
            <Btn size='sm' type='button' onClick={addOption}>
              + Add option
            </Btn>
          )}
        </div>
      )}

      <div className={styles.settingsRow}>
        <div className={styles.settingField}>
          <label className={styles.label}>Points</label>
          <Input
            type='number'
            min={10}
            max={1000}
            step={10}
            value={pointValue}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
              setPointValue(e.target.valueAsNumber);
            }}
          />
        </div>
        <div className={styles.settingField}>
          <label className={styles.label}>Time limit (sec)</label>
          <Input
            type='number'
            min={5}
            max={120}
            value={timeLimit}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
              setTimeLimit(e.target.valueAsNumber);
            }}
          />
        </div>
        <div className={styles.settingField}>
          <label className={styles.label}>Difficulty</label>
          <select
            value={difficulty}
            onChange={(e) => {
              setDifficulty(
                e.target.value as UpsertQuestionRequest["difficulty"],
              );
            }}>
            <option value='EASY'>Easy</option>
            <option value='MEDIUM'>Medium</option>
            <option value='HARD'>Hard</option>
          </select>
        </div>
      </div>

      {error && <p className={styles.error}>{error}</p>}

      <div className={styles.questionFormActions}>
        <Btn type='button' onClick={handleSave} disabled={!valid || saving}>
          {saving ? "Saving…" : "Save Question"}
        </Btn>
        <Btn type='button' onClick={onCancel}>
          Cancel
        </Btn>
      </div>
    </div>
  );
};

// ─── Pack metadata form (rendered once pack data loads in edit mode) ──────────

interface PackMetaFormProps {
  pack: DeckDto | undefined;
  isEditMode: boolean;
  returnTo: string | undefined;
  onCreated: (id: string) => void;
}

const PackMetaForm = ({
  pack,
  isEditMode,
  returnTo,
  onCreated,
}: PackMetaFormProps) => {
  const [name, setName] = useState(pack?.name ?? "");
  const [description, setDescription] = useState(pack?.description ?? "");
  const [category, setCategory] = useState(pack?.category ?? "");
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const navigate = useNavigate();
  const [createPack, { isLoading: creating }] = useCreateDeckMutation();
  const [updatePack, { isLoading: updating }] = useUpdateDeckMutation();

  const handleSave = async () => {
    setError(null);
    setSaved(false);
    try {
      if (isEditMode && pack?.id) {
        await updatePack({
          id: pack.id,
          updateDeckRequest: { name, description, category },
        }).unwrap();
        setSaved(true);
      } else {
        const created = await createPack({
          createDeckRequest: { name, description, category },
        }).unwrap();
        if (returnTo) {
          await navigate({ to: returnTo as "/" });
        } else if (created.id) {
          onCreated(created.id);
        }
      }
    } catch (e) {
      setError(extractErrorMessage(e, "Failed to save. Please try again."));
    }
  };

  return (
    <section className={styles.section}>
      <h2 className={styles.sectionTitle}>Details</h2>
      <div className={styles.fieldGroup}>
        <label className={styles.label}>Name *</label>
        <Input
          value={name}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
            setName(e.target.value);
          }}
          placeholder='General Knowledge'
          maxLength={100}
        />
      </div>
      <div className={styles.fieldGroup}>
        <label className={styles.label}>Description</label>
        <TextArea
          value={description}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => {
            setDescription(e.target.value);
          }}
          placeholder='A short description of this pack…'
          rows={2}
          maxLength={500}
        />
      </div>
      <div className={styles.fieldGroup}>
        <label className={styles.label}>Category</label>
        <Input
          value={category}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
            setCategory(e.target.value);
          }}
          placeholder='Science, History, Pop Culture…'
          maxLength={50}
        />
      </div>
      {error && <p className={styles.error}>{error}</p>}
      {saved && (
        <p
          style={{
            color: "var(--green-600)",
            fontSize: "var(--font-size-sm)",
          }}>
          Saved.
        </p>
      )}
      <div className={styles.metaActions}>
        <Btn
          onClick={() => {
            void handleSave();
          }}
          disabled={!name.trim() || creating || updating}>
          {creating || updating
            ? "Saving…"
            : isEditMode
              ? "Save Changes"
              : "Create Pack"}
        </Btn>
        {!isEditMode && returnTo && (
          <Link to={returnTo as "/"}>
            <Btn>Cancel</Btn>
          </Link>
        )}
      </div>
    </section>
  );
};

// ─── Question list ────────────────────────────────────────────────────────────

interface QuestionListProps {
  packId: string;
}

const makeInitial = (q: QuestionEditorDto): UpsertQuestionRequest => ({
  type: q.type ?? "MULTIPLE_CHOICE",
  questionText: q.questionText ?? "",
  options: q.options ?? ["", ""],
  correctAnswer: q.correctAnswer ?? 0,
  correctAnswerText: q.correctAnswerText ?? "",
  pointValue: q.pointValue ?? 100,
  timeLimit: q.timeLimit ?? 15,
  difficulty: q.difficulty ?? "MEDIUM",
});

const BLANK_QUESTION: UpsertQuestionRequest = {
  type: "MULTIPLE_CHOICE",
  questionText: "",
  options: ["", "", "", ""],
  correctAnswer: 0,
  pointValue: 100,
  timeLimit: 15,
  difficulty: "MEDIUM",
};

const QuestionList = ({ packId }: QuestionListProps) => {
  const { data: questions = [], refetch } = useListQuestionsQuery({
    id: packId,
  });
  const [addQuestion, { isLoading: adding }] = useAddQuestionMutation();
  const [updateQuestion, { isLoading: updating }] = useUpdateQuestionMutation();
  const [deleteQuestion] = useDeleteQuestionMutation();

  const [editingId, setEditingId] = useState<string | null>(null);
  const [addingNew, setAddingNew] = useState(false);
  const [questionError, setQuestionError] = useState<string | null>(null);

  const handleAdd = async (form: UpsertQuestionRequest) => {
    setQuestionError(null);
    try {
      await addQuestion({ id: packId, upsertQuestionRequest: form }).unwrap();
      setAddingNew(false);
      void refetch();
    } catch (e) {
      setQuestionError(extractErrorMessage(e, "Failed to save question."));
    }
  };

  const handleUpdate = async (
    questionId: string,
    form: UpsertQuestionRequest,
  ) => {
    setQuestionError(null);
    try {
      await updateQuestion({
        id: packId,
        questionId,
        upsertQuestionRequest: form,
      }).unwrap();
      setEditingId(null);
      void refetch();
    } catch (e) {
      setQuestionError(extractErrorMessage(e, "Failed to save question."));
    }
  };

  const handleDelete = async (questionId: string) => {
    if (!confirm("Delete this question?")) return;
    await deleteQuestion({ id: packId, questionId }).unwrap();
    void refetch();
  };

  return (
    <section className={styles.section}>
      <h2 className={styles.sectionTitle}>Questions ({questions.length})</h2>

      {questions.length === 0 && !addingNew && (
        <p className={styles.emptyQuestions}>
          No questions yet. Add your first one below.
        </p>
      )}

      <div className={styles.questionList}>
        {questions.map((q, i) => (
          <div key={q.id}>
            {editingId === q.id && q.id ? (
              <QuestionForm
                initial={makeInitial(q)}
                onSave={(form) => {
                  if (q.id) void handleUpdate(q.id, form);
                }}
                onCancel={() => {
                  setEditingId(null);
                  setQuestionError(null);
                }}
                saving={updating}
                error={questionError}
              />
            ) : (
              <div className={styles.questionRow}>
                <span className={styles.questionNum}>{i + 1}.</span>
                <span className={styles.questionText}>{q.questionText}</span>
                <div className={styles.questionRowActions}>
                  <Btn
                    size='sm'
                    onClick={() => {
                      if (q.id) {
                        setEditingId(q.id);
                        setQuestionError(null);
                      }
                    }}>
                    Edit
                  </Btn>
                  <Btn
                    size='sm'
                    variant='error'
                    onClick={() => {
                      if (q.id) {
                        void handleDelete(q.id);
                      }
                    }}>
                    Delete
                  </Btn>
                </div>
              </div>
            )}
          </div>
        ))}

        {addingNew && (
          <QuestionForm
            initial={BLANK_QUESTION}
            onSave={(form) => {
              void handleAdd(form);
            }}
            onCancel={() => {
              setAddingNew(false);
              setQuestionError(null);
            }}
            saving={adding}
            error={questionError}
          />
        )}
      </div>

      {!addingNew && editingId === null && (
        <Btn
          style={{ marginTop: "var(--space-3)" }}
          onClick={() => {
            setAddingNew(true);
          }}>
          + Add Question
        </Btn>
      )}
    </section>
  );
};

// ─── Page ─────────────────────────────────────────────────────────────────────

const PackEditorPage = ({ packId, returnTo }: PackEditorPageProps) => {
  const navigate = useNavigate();
  const isEditMode = !!packId;
  const [activePack, setActivePack] = useState<string | undefined>(packId);

  const { data: pack, isLoading: packLoading } = useGetDeckQuery(
    { id: activePack ?? "" },
    { skip: !activePack },
  );

  const handleCreated = async (newPackId: string) => {
    setActivePack(newPackId);
    await navigate({
      to: "/my-packs/$packId/edit",
      params: { packId: newPackId },
    });
  };

  if (isEditMode && packLoading) {
    return <div className={styles.page}>Loading…</div>;
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <Link to='/my-packs' className={styles.backLink} viewTransition>
          ← My Packs
        </Link>
        <h1 className={styles.title}>
          {isEditMode ? "Edit Pack" : "New Pack"}
        </h1>
      </div>

      <PackMetaForm
        pack={pack}
        isEditMode={isEditMode}
        returnTo={returnTo}
        onCreated={(id) => {
          void handleCreated(id);
        }}
      />

      {activePack && (
        <>
          <hr className={styles.divider} />
          <QuestionList packId={activePack} />
        </>
      )}
    </div>
  );
};

export { PackEditorPage };
