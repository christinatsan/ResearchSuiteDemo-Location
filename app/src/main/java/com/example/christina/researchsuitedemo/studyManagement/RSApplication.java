package com.example.christina.researchsuitedemo.studyManagement;

/**
 * Created by Christina on 8/11/17.
 */

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.example.christina.researchsuitedemo.geofenceManagement.RSuiteGeofenceManager;

import net.sqlcipher.database.SQLiteDatabase;

import org.researchstack.backbone.ResourcePathManager;
import org.researchstack.backbone.StorageAccess;
import org.researchstack.backbone.storage.database.AppDatabase;
import org.researchstack.backbone.storage.database.sqlite.SqlCipherDatabaseHelper;
import org.researchstack.backbone.storage.database.sqlite.UpdatablePassphraseProvider;
import org.researchstack.backbone.storage.file.UnencryptedProvider;
import org.researchstack.skin.DataResponse;

import java.io.UnsupportedEncodingException;

import edu.cornell.tech.foundry.ohmageomhbackend.ORBEOhmageResultBackEnd;
import edu.cornell.tech.foundry.ohmageomhsdk.OhmageOMHManager;
import edu.cornell.tech.foundry.researchsuiteresultprocessor.RSRPResultsProcessor;
import edu.cornell.tech.foundry.researchsuitetaskbuilder.RSTBStateHelper;
import rx.Single;
import rx.SingleSubscriber;


public class RSApplication extends Application {

    @Override
    public void onCreate()
    {
        super.onCreate();
        this.initializeSingletons(this);
    }

    public void initializeSingletons(Context context) {

        //TODO: Change to pin encrypted
        UnencryptedProvider encryptionProvider = new UnencryptedProvider();

        AppDatabase dbAccess = createAppDatabaseImplementation(context);
        dbAccess.setEncryptionKey(encryptionProvider.getEncrypter().getDbKey());

        RSFileAccess fileAccess = createFileAccessImplementation(context);
        fileAccess.setEncrypter(encryptionProvider.getEncrypter());

        StorageAccess.getInstance().init(
                null,
                new UnencryptedProvider(),
                fileAccess,
                createAppDatabaseImplementation(context)
        );

        String directory = context.getApplicationInfo().dataDir;


//        OhmageOMHManager.config(
//                context,
//                getString(R.string.omh_base_url),
//                getString(R.string.omh_client_id),
//                getString(R.string.omh_client_secret),
//                fileAccess,
//                getString(R.string.ohmage_queue_directory)
//        );

        RSResourcePathManager resourcePathManager = new RSResourcePathManager();
        ResourcePathManager.init(resourcePathManager);
        //config task builder singleton
        //task builder requires ResourceManager, ImpulsivityAppStateManager
        RSTaskBuilderManager.init(context, resourcePathManager, fileAccess);


        RSResultsProcessorManager.init(ORBEOhmageResultBackEnd.getInstance());
        RSRPResultsProcessor resultsProcessor = new RSRPResultsProcessor(ORBEOhmageResultBackEnd.getInstance());

    }


    public void resetSingletons(Context context) {



        this.initializeSingletons(context);

    }



    protected RSFileAccess createFileAccessImplementation(Context context)
    {
        String pathName = "/rsuite";
        return new RSFileAccess(pathName);
    }

    protected AppDatabase createAppDatabaseImplementation(Context context) {
        SQLiteDatabase.loadLibs(context);

        return new SqlCipherDatabaseHelper(
                context,
                SqlCipherDatabaseHelper.DEFAULT_NAME,
                null,
                SqlCipherDatabaseHelper.DEFAULT_VERSION,
                new UpdatablePassphraseProvider()
        );
    }

    public Single<DataResponse> signOut(Context context) {

        return Single.create(new Single.OnSubscribe<DataResponse>() {
            @Override
            public void call(final SingleSubscriber<? super DataResponse> singleSubscriber) {
                OhmageOMHManager.getInstance().signOut(new OhmageOMHManager.Completion() {
                    @Override
                    public void onCompletion(Exception e) {

                        Log.d("testing order: ","signed out");
                        RSFileAccess.getInstance().clearFileAccess(RSApplication.this);


                        if (e != null) {
                            Log.d("testing order: ","not signed out");
                            singleSubscriber.onError(e);
                        }
                        else {
                            Log.d("testing order: ","signed out");
                            singleSubscriber.onSuccess(new DataResponse(true, "success"));
                        }
                    }
                });
            }
        });

    }

    @Override
    protected void attachBaseContext(Context base)
    {
        // This is needed for android versions < 5.0 or you can extend MultiDexApplication
        super.attachBaseContext(base);
//        MultiDex.install(this);
    }

    public void initializeGeofenceManager(){
        RSTBStateHelper stateHelper = RSTaskBuilderManager.getBuilder().getStepBuilderHelper().getStateHelper();

        double homeLat = 0;
        double homeLng = 0;
        double workLat = 0;
        double workLng = 0;

        byte[] homeLatByte = stateHelper.valueInState(this,"latitude_home");
        byte[] homeLngByte = stateHelper.valueInState(this,"longitude_home");
        byte[] workLatByte = stateHelper.valueInState(this,"latitude_work");
        byte[] workLngByte = stateHelper.valueInState(this,"longitude_work");

        if(homeLatByte != null && homeLngByte != null && workLatByte != null && workLngByte != null){
            try {
                String homeLatString = new String(homeLatByte, "UTF-8");
                String homeLngString = new String(homeLngByte, "UTF-8");
                String workLatString = new String(workLatByte, "UTF-8");
                String workLngString = new String(workLngByte, "UTF-8");

                homeLat = Double.parseDouble(homeLatString);
                homeLng = Double.parseDouble(homeLngString);
                workLat = Double.parseDouble(workLatString);
                workLng = Double.parseDouble(workLngString);

                RSuiteGeofenceManager.init(this,homeLat,homeLng,workLat,workLng);

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else {
            RSuiteGeofenceManager.init(this,homeLat,homeLng,workLat,workLng);
        }

    }

}
