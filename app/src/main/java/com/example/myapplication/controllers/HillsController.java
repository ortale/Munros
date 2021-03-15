package com.example.myapplication.controllers;

import android.content.Context;

import com.example.myapplication.exceptions.InvalidQueryException;
import com.example.myapplication.models.Hill;
import com.example.myapplication.util.ErrorMessages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class HillsController {
    private static HillsController instance;

    private Context context;

    public Map<Filters, Object> searchFilter;

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

    public enum Filters {
        CATEGORY,
        SORT_HEIGHT,
        SORT_NAME,
        LIMIT,
        MIN_HEIGHT_M,
        MAX_HEIGHT_M
    }

    private HillsController(Context context) {
        this.context = context;
    }

    public static HillsController getInstance(Context context) {
        if (instance == null) {
            instance = new HillsController(context);
        }

        return instance;
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

    private Observable<Hill> prepareHillObservable(List<Hill> hills, Map<Filters, Object> searchFilter) throws InvalidQueryException {
        Observable<Hill> observable = Observable.fromIterable(hills).subscribeOn(Schedulers.io());

        for (Map.Entry<Filters, Object> entry : searchFilter.entrySet()) {
            if (entry.getKey().equals(Filters.CATEGORY)) {
                observable = observable.filter(hill -> hill.getYearPost1997().equalsIgnoreCase(entry.getValue().toString()));
            }

            if (entry.getKey().equals(Filters.LIMIT)) {
                int limit = (int) entry.getValue();
                observable = observable.take(limit);
            }

            if (entry.getKey().equals(Filters.SORT_NAME)) {
                String sortName = entry.getValue().toString();

                if (sortName.equalsIgnoreCase(FilterSort.ASC.name())) {
                    hills.sort(((o1, o2) -> o1.getName().compareTo(o2.getName())));
                }

                else {
                    hills.sort(((o1, o2) -> o2.getName().compareTo(o1.getName())));
                }

                observable = Observable.fromIterable(hills).subscribeOn(Schedulers.io());
            }

            if (entry.getKey().equals(Filters.SORT_HEIGHT)) {
                String sortHeight = entry.getValue().toString();

                if (sortHeight.equalsIgnoreCase(FilterSort.ASC.name())) {
                    hills.sort(((o1, o2) -> o1.getHeightInMetres().compareTo(o2.getHeightInMetres())));
                }

                else {
                    hills.sort(((o1, o2) -> o2.getHeightInMetres().compareTo(o1.getHeightInMetres())));
                }

                observable = Observable.fromIterable(hills).subscribeOn(Schedulers.io());
            }

            if ((entry.getKey().equals(Filters.MAX_HEIGHT_M) || entry.getKey().equals(Filters.MIN_HEIGHT_M))
                    && entry.getValue() != null) {
                if (entry.getKey().equals(Filters.MAX_HEIGHT_M) && entry.getValue() != null) {
                    int max = (int) entry.getValue();

                    for (Map.Entry<Filters, Object> minHeightCheck : searchFilter.entrySet()) {
                        if (minHeightCheck.getKey().equals(Filters.MIN_HEIGHT_M) && minHeightCheck.getValue() != null) {
                            int min = (int) minHeightCheck.getValue();

                            if (min > max) {
                                throw new InvalidQueryException(ErrorMessages.INVALID_MAX_HEIGHT);
                            }

                            else {
                                observable = observable.filter(hill -> hill.getHeightInMetres() > min && hill.getHeightInMetres() < max);
                            }
                        }

                        else {
                            throw new InvalidQueryException(ErrorMessages.MISSING_MIN_HEIGHT);
                        }
                    }
                } else if (entry.getKey().equals(Filters.MIN_HEIGHT_M) && entry.getValue() != null) {
                    int max = (int) entry.getValue();

                    for (Map.Entry<Filters, Object> minHeightCheck : searchFilter.entrySet()) {
                        if (minHeightCheck.getKey().equals(Filters.MAX_HEIGHT_M) && minHeightCheck.getValue() != null) {
                            int min = (int) minHeightCheck.getValue();

                            if (min > max) {
                                throw new InvalidQueryException(ErrorMessages.INVALID_MIN_HEIGHT);
                            }

                            else {
                                observable = observable.filter(hill -> hill.getHeightInMetres() > min && hill.getHeightInMetres() < max);
                            }
                        }

                        else {
                            throw new InvalidQueryException(ErrorMessages.MISSING_MAX_HEIGHT);
                        }
                    }
                }
            }
        }

        return observable;
    }

    public void getHillsAll(@NonNull Observer<Hill> observer, Map<Filters, Object> searchFilter) throws InvalidQueryException {
        try {
            List<Hill> hills = loadFileData();

            prepareHillObservable(hills, searchFilter)
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
