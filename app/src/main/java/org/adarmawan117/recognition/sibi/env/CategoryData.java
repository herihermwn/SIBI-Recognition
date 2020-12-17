package org.adarmawan117.recognition.sibi.env;

public class CategoryData {
    private String name;
    private String url;
    private int total;

    public CategoryData(String name, String url, int total) {
        this.name = name;
        this.url = url;
        this.total = total;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public int getTotal() {
        return total;
    }
}
