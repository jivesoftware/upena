package com.jivesoftware.os.upena.deployable.okta.client.models.factors;


import com.jivesoftware.os.upena.deployable.okta.client.framework.ApiObject;

public class Question extends ApiObject {

    private String question;

    private String questionText;

    /**
     * Gets question
     */
    public String getQuestion() {
        return this.question;
    }

    /**
     * Sets question
     */
    public void setQuestion(String val) {
        this.question = val;
    }

    /**
     * Gets questionText
     */
    public String getQuestionText() {
        return this.questionText;
    }

    /**
     * Sets questionText
     */
    public void setQuestionText(String val) {
        this.questionText = val;
    }
}