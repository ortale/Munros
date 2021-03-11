package com.example.myapplication.services;

import android.content.Context;

import com.example.myapplication.exceptions.InvalidOrNotFoundFileException;
import com.example.myapplication.exceptions.InvalidQueryException;
import com.example.myapplication.models.Hill;
import com.example.myapplication.util.ErrorMessages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;

/**
 * The {@code HillService} class is responsible for receiving data from outside of the library.
 * The input data can be filtered and should use Enumerators for fields names and so for some values
 * like hills category and filter sorting.
 *
 */
public class HillService {
    private static HillService instance;

    private Context context;

    /**
     * <b>
     *     {@code FilterCategory}
     * </b>
     *
     * <p>
     *     Enum contains values to Category filter.
     * </p>
     */
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

    /**
     * <b>{@code FilterSort}</b>
     *
     * <p>
     *     Enum contains values to Sort filter.
     * </p>
     */
    public enum FilterSort {
        ASC,
        DESC
    }

    /**
     * <b>{@code Filters}</b>
     * <p>
     *      Enum containing fields names.
     * </p>
     * <p>
     *     <b>CATEGORY</b> field to filter hills category
     *     <b>SORT_HEIGHT</b> field to sort by height and can indicate ascending or descending sort
     *     <b>SORT_NAME</b> filter to sort by name and can indicate ascending or descending sort
     *     <b>LIMIT</b> field to filter by limit of rows
     *     <b>MIN_HEIGHT_M</b> field to filter by minimum height in metres and depends on maximum height
     *     <b>MAX_HEIGHT_M</b> field to filter by maximum height in metres and depends on minimum height
     * </p>
     */
    public enum Filters {
        CATEGORY,
        SORT_HEIGHT,
        SORT_NAME,
        LIMIT,
        MIN_HEIGHT_M,
        MAX_HEIGHT_M
    }

    private HillService(Context context) {
        this.context = context;
    }

    /**
     * Gets {@code HillService} class instance using singleton.
     * Needs {@code Context} from the current application context.
     *
     * @param context
     *        Current application context used to access resources.
     * @return
     *        A instance of {@code HillService} class using singleton.
     */
    public static HillService getInstance(Context context) {
        if (instance == null) {
            instance = new HillService(context);
        }

        return instance;
    }

    /**
     * Gets hills list and searchFilters in order to generate {@code Observable<Hill>} adding filters
     * and sorting fields if they exist.
     *
     * @param hills
     *        Contains list of hills retrieved from CSV file.
     * @param searchFilter
     *        Contains filter fields and their respective values.
     * @return
     *        An {@code Observable<Hill>} containing the necessary filters and sorting fields if they exist.
     * @throws InvalidQueryException
     *        In case of any invalid query, this exception is thrown.
     */
    private Observable<Hill> prepareHillObservable(List<Hill> hills, Map<Filters, Object> searchFilter) throws InvalidQueryException {
        Observable<Hill> observable = null;

        for (Map.Entry<Filters, Object> entry : searchFilter.entrySet()) {
            if (entry.getKey().equals(Filters.SORT_NAME)) {
                String sortName = entry.getValue().toString();

                if (sortName.equalsIgnoreCase(FilterSort.ASC.name())) {
                    hills.sort(((o1, o2) -> o1.getName().compareTo(o2.getName())));
                }

                else {
                    hills.sort(((o1, o2) -> o2.getName().compareTo(o1.getName())));
                }
            }

            if (entry.getKey().equals(Filters.SORT_HEIGHT)) {
                String sortHeight = entry.getValue().toString();

                if (sortHeight.equalsIgnoreCase(FilterSort.ASC.name())) {
                    hills.sort(((o1, o2) -> o1.getHeightInMetres().compareTo(o2.getHeightInMetres())));
                }

                else {
                    hills.sort(((o1, o2) -> o2.getHeightInMetres().compareTo(o1.getHeightInMetres())));
                }
            }
        }

        observable = Observable.fromIterable(hills).subscribeOn(Schedulers.io());

        for (Map.Entry<Filters, Object> entry : searchFilter.entrySet()) {
            if (entry.getKey().equals(Filters.CATEGORY)) {
                observable = observable.filter(hill -> hill.getYearPost1997().equalsIgnoreCase(entry.getValue().toString()));
            }

            if (entry.getKey().equals(Filters.LIMIT)) {
                int limit = (int) entry.getValue();
                observable = observable.take(limit);
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

    /**
     * Method used to get all hills data and can assume the provider filters if they exist.
     *
     * @param observer
     *        Callback for this request.
     * @param searchFilter
     *        Contains all the suitable filters for the search.
     * @throws InvalidQueryException
     */
    public void getHills(@NonNull Observer<Hill> observer, Map<Filters, Object> searchFilter) throws InvalidQueryException {
        try {
            List<Hill> hills = loadFileData();

            prepareHillObservable(hills, searchFilter)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check the last line containing hill information.
     *
     * @param line
     *        Current searching line.
     * @return
     *        true for last line and false for not the last line.
     */
    private boolean isEndOfHills(String[] line) {
        return line[0].isEmpty();
    }

    /**
     * Loads CSV file data into a list.
     *
     * @return
     *         A list of objects Hill.
     * @throws IOException
     *         {@code InvalidOrNotFoundFileException} Exception inherits from {@code IOException} class
     *         and is used to set a custom message in case if CSV file is not found.
     */
    private List<Hill> loadFileData() throws IOException {
        InputStreamReader is = new InputStreamReader(context.getAssets().open("munrotab_v6.2.csv"));

        if (is == null) {
            throw new InvalidOrNotFoundFileException(ErrorMessages.INVALID_CSV_FILE);
        }

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
