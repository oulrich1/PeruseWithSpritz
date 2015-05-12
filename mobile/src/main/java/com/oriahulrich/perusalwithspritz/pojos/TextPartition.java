package com.oriahulrich.perusalwithspritz.pojos;

public class TextPartition {
    public TextPartition() {
        init();
    }
    public TextPartition(String text) {
        init();
        setText(text);
    }

    private void init() {
        setText("");
    }

    public String getText() {
        return m_text;
    }

    public void setText(String text) {
        this.m_text = text;
    }
    private String m_text;
}
