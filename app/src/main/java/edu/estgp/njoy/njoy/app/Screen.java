package edu.estgp.njoy.njoy.app;

import java.util.ArrayList;

/**
 * Created by vrealinho on 08/03/18.
 */

public class Screen {
    private String title;
    private ArrayList<Option> options;

    public int getId() {
        return id;
    }

    private int id;

    public Screen(String title, ArrayList options, int id) {
        this.title = title;
        this.options = options;
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ArrayList<Option> getOptions() {
        return options;
    }

    public void setOptions(ArrayList options) {
        this.options = options;
    }
}
