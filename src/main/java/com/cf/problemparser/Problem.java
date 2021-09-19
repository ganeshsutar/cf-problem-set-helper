package com.cf.problemparser;

import java.util.Arrays;

public class Problem {
    private String problemNo;
    private String problemTitle;
    private int noOfTests;
    private String[] inputs;
    private String[] outputs;

    public String getProblemNo() {
        return problemNo;
    }

    public void setProblemNo(String problemNo) {
        this.problemNo = problemNo;
    }

    public String getProblemTitle() {
        return problemTitle;
    }

    public void setProblemTitle(String problemTitle) {
        this.problemTitle = problemTitle;
    }

    public int getNoOfTests() {
        return noOfTests;
    }

    public void setNoOfTests(int noOfTests) {
        this.noOfTests = noOfTests;
    }

    public String[] getInputs() {
        return inputs;
    }

    public void setInputs(String[] inputs) {
        this.inputs = inputs;
    }

    public String[] getOutputs() {
        return outputs;
    }

    public void setOutputs(String[] outputs) {
        this.outputs = outputs;
    }

    public String getModuleName() {
        return problemNo + problemTitle;
    }

    @Override
    public String toString() {
        return "Problem{" +
                "problemNo='" + problemNo + '\'' +
                ", problemTitle='" + problemTitle + '\'' +
                ", noOfTests=" + noOfTests +
                ", inputs=" + Arrays.toString(inputs) +
                ", outputs=" + Arrays.toString(outputs) +
                '}';
    }
}