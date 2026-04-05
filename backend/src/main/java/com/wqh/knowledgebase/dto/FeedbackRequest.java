package com.wqh.knowledgebase.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class FeedbackRequest {

    @NotNull(message = "不能为空")
    private Long qaRecordId;

    @NotNull(message = "不能为空")
    @Min(value = 1, message = "不能小于 1")
    @Max(value = 5, message = "不能大于 5")
    private Integer rating;

    private String comment;

    public Long getQaRecordId() {
        return qaRecordId;
    }

    public void setQaRecordId(Long qaRecordId) {
        this.qaRecordId = qaRecordId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
