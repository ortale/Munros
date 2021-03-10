package com.example.myapplication.controllers;

import android.content.Context;

import com.example.myapplication.exceptions.InvalidQueryException;
import com.example.myapplication.models.Hill;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;

public class HillsController {
    private static HillsController instance;

    private Context context;

    private HillsController(Context context) {
        this.context = context;
    }

    public static HillsController getInstance(Context context) {
        if (instance == null) {
            instance = new HillsController(context);
        }

        return instance;
    }

    public enum FilterCategory {
        MUN("MUN"),
        MUN_TOP("TOP");

        private String name;

        FilterCategory(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    public enum FilterSort {
        ASC,
        DESC
    }

    public void getHillsBetweenHeight(@NonNull Observer<Hill> observer, int min, int max) throws InvalidQueryException {
        if (max < min) {
            throw new InvalidQueryException("Maximum height should be more than minimum");
        }

        try {
            List<Hill> hills = loadFileData();

            Observable.fromIterable(hills).subscribeOn(Schedulers.io())
                    .filter(hill -> hill.getHeightInMetres() > min && hill.getHeightInMetres() < max)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getHillsLimitedList(@NonNull SingleObserver<List<Hill>> observer, int limit) throws InvalidQueryException {
        if (limit <= 0) {
            throw new InvalidQueryException("Limit should be at least 1");
        }

        try {
            List<Hill> hills = loadFileData();

            Observable.fromIterable(hills).subscribeOn(Schedulers.computation())
                    .take(limit)
                    .toSortedList((par1, par2) -> par1.getHeightInMetres().compareTo(par2.getHeightInMetres()))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getHillsSortByHeight(@NonNull SingleObserver<List<Hill>> observer, FilterSort sortHeight) {
        try {
            List<Hill> hills = loadFileData();

            Observable.fromIterable(hills).subscribeOn(Schedulers.computation())
                    .toSortedList((par1, par2) -> {
                        if (sortHeight.equals(FilterSort.ASC)) {
                            return par1.getHeightInMetres().compareTo(par2.getHeightInMetres());
                        }

                        return par2.getHeightFeet().compareTo(par1.getHeightFeet());
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getHillsSortByName(@NonNull SingleObserver<List<Hill>> observer, FilterSort sortName) {
        try {
            List<Hill> hills = loadFileData();

            Observable.fromIterable(hills).subscribeOn(Schedulers.computation())
                    .toSortedList((par1, par2) -> {
                        if (sortName.equals(FilterSort.ASC)) {
                            return par1.getHeightInMetres().compareTo(par2.getHeightInMetres());
                        }

                        return par2.getHeightFeet().compareTo(par1.getHeightFeet());
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getHillsByCat(@NonNull Observer<Hill> observer, FilterCategory category) {
        try {
            List<Hill> hills = loadFileData();

            Observable.fromIterable(hills).subscribeOn(Schedulers.io())
                    .filter(hill -> hill.getYearPost1997().equalsIgnoreCase(category.getName()))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getHills(@NonNull Observer<Hill> observer) {
        try {
            List<Hill> hills = loadFileData();

            Observable.fromIterable(hills).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isEndOfHills(String[] line) {
        return line[0].isEmpty();
    }

    private List<Hill> loadFileData() throws IOException {
        InputStreamReader is = new InputStreamReader(context.getAssets().open("munrotab_v6.2.csv"));

        BufferedReader reader = new BufferedReader(is);
        reader.readLine();

        String line = null;
        boolean isEndOfHills = false;

        List<Hill> hills = new ArrayList<>();

        while (!isEndOfHills && ((line = reader.readLine()) != null)) {
            String limiter = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

            String[] columns = line.split(limiter, -1);
            isEndOfHills = isEndOfHills(columns);

            if (columns[0].isEmpty()) {
                break;
            }

            Hill hill = new Hill();

            hill.setRunningNo(Integer.parseInt(columns[0]));
            hill.setName(columns[5]);
            hill.setHeightInMetres(Double.parseDouble(columns[9]));
            hill.setGridRef(columns[13]);
            hill.setYearPost1997(columns[27]);

            hills.add(hill);
        }

        return hills;
    }
}
