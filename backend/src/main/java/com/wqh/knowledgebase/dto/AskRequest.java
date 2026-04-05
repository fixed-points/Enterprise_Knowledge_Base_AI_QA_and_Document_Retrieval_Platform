package com.wqh.knowledgebase.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

public class AskRequest {

    @NotBlank(message = "不能为空")
    private String question;

    @Min(value = 1, message = "不能小于 1")
    @Max(value = 10, message = "不能大于 10")
    private Integer topK = 5;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }
}
