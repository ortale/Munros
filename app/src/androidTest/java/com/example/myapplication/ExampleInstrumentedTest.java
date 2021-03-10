package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myapplication.controllers.HillsController;
import com.example.myapplication.models.Hill;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.File;

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
public class ExampleInstrumentedTest {
    private final String TAG = ExampleInstrumentedTest.class.getSimpleName();

    private HillsController hillsController;

    private Context context;

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

        hillsController = HillsController.getInstance(context);
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
        Observer<Hill> observer = new Observer<Hill>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                Log.v(TAG, "onSubscribe");
            }

            @Override
            public void onNext(@NonNull Hill hill) {
                Log.v(TAG, "onNext Hill name: " + hill.getName() + " onNext category: " + hill.getYearPost1997());
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

        hillsController.getHills(observer);

        Observable<Object> obs = mockedObservableNoErrors();
        TestObserver<Object> testObserver = TestObserver.create();
        obs.subscribe(testObserver);
        testObserver.assertError(Throwable.class);

        testObserver.assertSubscribed();
        testObserver.assertNoErrors();
    }
}