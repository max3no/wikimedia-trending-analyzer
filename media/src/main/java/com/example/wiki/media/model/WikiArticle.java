package com.example.wiki.media.model;

public class WikiArticle {
    private String article;
    private long views;
    private int rank;

    public WikiArticle(String article, long views, int rank) {
        this.article = article;
        this.views = views;
        this.rank = rank;
    }

    public String getArticle() { return article; }
    public long getViews() { return views; }
    public int getRank() { return rank; }
}
