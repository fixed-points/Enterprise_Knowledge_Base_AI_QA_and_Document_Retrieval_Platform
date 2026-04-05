package com.wqh.knowledgebase.dto;

public class DashboardOverview {
    private long documentCount;
    private long indexedCount;
    private long qaCount;
    private long feedbackCount;

    public long getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(long documentCount) {
        this.documentCount = documentCount;
    }

    public long getIndexedCount() {
        return indexedCount;
    }

    public void setIndexedCount(long indexedCount) {
        this.indexedCount = indexedCount;
    }

    public long getQaCount() {
        return qaCount;
    }

    public void setQaCount(long qaCount) {
        this.qaCount = qaCount;
    }

    public long getFeedbackCount() {
        return feedbackCount;
    }

    public void setFeedbackCount(long feedbackCount) {
        this.feedbackCount = feedbackCount;
    }
}
