// File upload component with drag-and-drop support and multi-file selection
import React, { useRef, useState, useCallback } from "react";
import styles from "./Form.module.css";
import { IconBtn } from "../Buttons/IconBtn";

interface FileUploadProps {
  label?: string;
  accept?: string;
  errorMessage?: string;
  infoMessage?: string;
  onChange?: (files: File[]) => void;
}

const FileUpload: React.FC<FileUploadProps> = ({
  label,
  accept,
  errorMessage,
  infoMessage,
  onChange,
}) => {
  const [files, setFiles] = useState<File[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const addFiles = useCallback(
    (incoming: FileList | null) => {
      if (!incoming) return;
      const next = [...files, ...Array.from(incoming)];
      setFiles(next);
      onChange?.(next);
    },
    [files, onChange],
  );

  const removeFile = (index: number) => {
    const next = files.filter((_, i) => i !== index);
    setFiles(next);
    onChange?.(next);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    if (!e.currentTarget.contains(e.relatedTarget as Node)) {
      setIsDragging(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    addFiles(e.dataTransfer.files);
  };

  return (
    <div className={styles.fileUploadContainer}>
      {label && <label>{label}</label>}
      <div
        className={[styles.dropZone, isDragging && styles.dragging]
          .filter(Boolean)
          .join(" ")}
        onClick={() => {
          inputRef.current?.click();
        }}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        role='button'
        tabIndex={0}
        onKeyDown={(e) => {
          if (e.key === "Enter") {
            inputRef.current?.click();
          }
        }}>
        <input
          ref={inputRef}
          type='file'
          multiple
          accept={accept}
          onChange={(e) => {
            addFiles(e.target.files);
          }}
        />
        <span className={styles.dropZoneText}>
          {isDragging ? "Drop files here" : "Click or drag files here"}
        </span>
      </div>
      {files.length > 0 && (
        <ul className={styles.fileList}>
          {files.map((file, i) => (
            <li
              key={`${file.name}-${file.size}-${file.lastModified}`}
              className={styles.fileItem}>
              <span className={styles.fileName}>{file.name}</span>

              <IconBtn
                type='close'
                size='xs'
                className={styles.removeFile}
                onClick={() => {
                  removeFile(i);
                }}
                aria-label={`Remove ${file.name}`}
              />
            </li>
          ))}
        </ul>
      )}
      {(errorMessage != null || infoMessage != null) && (
        <span
          className={[
            styles.inputInfoMessage,
            errorMessage && styles.errorMessage,
          ]
            .filter(Boolean)
            .join(" ")}>
          {errorMessage ?? infoMessage}
        </span>
      )}
    </div>
  );
};

export { FileUpload };
