package repair.common;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CodeChangeInfo {
    @JsonProperty("before_commit_id")
    private String beforeCommitId;
    @JsonProperty("after_commit_id")
    private String afterCommitId;
    @JsonProperty("file_path")
    private String filePath;
    @JsonProperty("signature_before")
    private String signatureBefore;
    @JsonProperty("signature_after")
    private String signatureAfter;
    @JsonProperty("before_start")
    private String beforeStart;
    @JsonProperty("before_end")
    private String beforeEnd;
    @JsonProperty("after_start")
    private String afterStart;
    @JsonProperty("after_end")
    private String afterEnd;

    public String getBeforeCommitId() {
        return beforeCommitId;
    }

    public String getAfterCommitId() {
        return afterCommitId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getSignatureBefore() {
        return signatureBefore;
    }

    public String getSignatureAfter() {
        return signatureAfter;
    }

    public String getBeforeStart() {
        return beforeStart;
    }

    public String getBeforeEnd() {
        return beforeEnd;
    }

    public String getAfterStart() {
        return afterStart;
    }

    public String getAfterEnd() {
        return afterEnd;
    }
}
