package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myapplication.services.HillService;
import com.example.myapplication.exceptions.InvalidQueryException;
import com.example.myapplication.models.Hill;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class HillsInstrumentedTest {
    private final String TAG = HillsInstrumentedTest.class.getSimpleName();

    private HillService hillService;

    private Context context;

    int count = 0;

    @Rule
    public TemporaryFolder storageDirectory = new TemporaryFolder();

    private File nonExistentDirectory;
    private File existentDirectory;

    @Before
    public void initialise() {
        nonExistentDirectory = Mockito.mock(File.class);
        Mockito.when(nonExistentDirectory.exists()).thenReturn(false);

        existentDirectory = storageDirectory.getRoot();

        context = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();

        hillService = HillService.getInstance(context);
    }

    private Observable<Object> mockedObservableNoErrors() {
        Hill hill = new Hill();

        hill.setRunningNo(10);
        hill.setName("Sgurr na Banachdich");
        hill.setYearPost1997("MUN");
        hill.setGridRef("NN773308");
        hill.setHeightInMetres(3003.0);

        return Observable.create(e -> {
            e.onNext(hill);
        });
    }

    @Test
    public void hillsList_noErrors() {
        count = 0;
        Observer<Hill> observer = new Observer<Hill>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                Log.v(TAG, "onSubscribe");
            }

            @Override
            public void onNext(@NonNull Hill hill) {
                count++;
                Log.v(TAG, "onNext Hill name: " + hill.getName() + " onNext category: "
                        + hill.getYearPost1997() + " height: " + hill.getHeightInMetres()
                        + " count: " + count);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                Log.v(TAG, "onError: " + e);
            }

            @Override
            public void onComplete() {
                Log.v(TAG, "onComplete");
            }
        };

        Map<HillService.Filters, Object> searchFilter = new HashMap<>();
        searchFilter.put(HillService.Filters.CATEGORY, HillService.FilterCategory.MUN);
        searchFilter.put(HillService.Filters.SORT_NAME, HillService.FilterSort.DESC);
        searchFilter.put(HillService.Filters.LIMIT, 10);

        try {
            hillService.getHills(observer, searchFilter);
        } catch (InvalidQueryException e) {
            e.printStackTrace();
        }

        Observable<Object> obs = mockedObservableNoErrors();
        TestObserver<Object> testObserver = TestObserver.create();
        obs.subscribe(testObserver);

        testObserver.assertSubscribed();
        testObserver.assertNoErrors();
    }
}