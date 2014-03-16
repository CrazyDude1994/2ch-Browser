package com.vortexwolf.dvach.common;

import java.io.File;

import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Application;
import android.content.res.Resources;
import android.httpimage.BitmapMemoryCache;
import android.httpimage.BitmapCache;
import android.httpimage.FileSystemPersistence;
import android.httpimage.HttpImageManager;

import com.vortexwolf.dvach.common.library.ExtendedHttpClient;
import com.vortexwolf.dvach.common.library.ExtendedObjectMapper;
import com.vortexwolf.dvach.db.DvachSqlHelper;
import com.vortexwolf.dvach.db.FavoritesDataSource;
import com.vortexwolf.dvach.db.HiddenThreadsDataSource;
import com.vortexwolf.dvach.db.HistoryDataSource;
import com.vortexwolf.dvach.interfaces.IBitmapManager;
import com.vortexwolf.dvach.interfaces.ICacheDirectoryManager;
import com.vortexwolf.dvach.interfaces.IDraftPostsStorage;
import com.vortexwolf.dvach.interfaces.IJsonApiReader;
import com.vortexwolf.dvach.interfaces.INavigationService;
import com.vortexwolf.dvach.interfaces.IOpenTabsManager;
import com.vortexwolf.dvach.interfaces.IPagesSerializationService;
import com.vortexwolf.dvach.interfaces.IPostSender;
import com.vortexwolf.dvach.services.BitmapManager;
import com.vortexwolf.dvach.services.CacheDirectoryManager;
import com.vortexwolf.dvach.services.NavigationService;
import com.vortexwolf.dvach.services.SerializationService;
import com.vortexwolf.dvach.services.ThreadImagesService;
import com.vortexwolf.dvach.services.MyTracker;
import com.vortexwolf.dvach.services.domain.DownloadFileService;
import com.vortexwolf.dvach.services.domain.JsonApiReader;
import com.vortexwolf.dvach.services.domain.PostSender;
import com.vortexwolf.dvach.services.presentation.DraftPostsStorage;
import com.vortexwolf.dvach.services.presentation.DvachUriBuilder;
import com.vortexwolf.dvach.services.presentation.OpenTabsManager;
import com.vortexwolf.dvach.services.presentation.PagesSerializationService;
import com.vortexwolf.dvach.settings.ApplicationSettings;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        MyTracker tracker = new MyTracker(this.getApplicationContext());
        DefaultHttpClient httpClient = new ExtendedHttpClient();
        ApplicationSettings settings = new ApplicationSettings(this, this.getResources());
        DvachUriBuilder dvachUriBuilder = new DvachUriBuilder(settings);
        JsonApiReader jsonApiReader = new JsonApiReader(httpClient, this.getResources(), new ExtendedObjectMapper(), dvachUriBuilder);
        DvachSqlHelper dbHelper = new DvachSqlHelper(this);
        HistoryDataSource historyDataSource = new HistoryDataSource(dbHelper);
        FavoritesDataSource favoritesDataSource = new FavoritesDataSource(dbHelper);
        HiddenThreadsDataSource hiddenThreadsDataSource = new HiddenThreadsDataSource(dbHelper);
        CacheDirectoryManager cacheManager = new CacheDirectoryManager(super.getCacheDir(), this.getPackageName(), settings, tracker);
        BitmapMemoryCache bitmapMemoryCache = new BitmapMemoryCache();
        HttpImageManager imageManager = new HttpImageManager(bitmapMemoryCache, new FileSystemPersistence(cacheManager), httpClient, this.getResources());
        NavigationService navigationService = new NavigationService();
        DownloadFileService downloadFileService = new DownloadFileService(this.getResources(), httpClient);

        Container container = Factory.getContainer();
        container.register(Resources.class, this.getResources());
        container.register(DvachUriBuilder.class, dvachUriBuilder);
        container.register(ApplicationSettings.class, settings);
        container.register(DefaultHttpClient.class, httpClient);
        container.register(IJsonApiReader.class, jsonApiReader);
        container.register(IPostSender.class, new PostSender(httpClient, this.getResources(), dvachUriBuilder, settings));
        container.register(IDraftPostsStorage.class, new DraftPostsStorage());
        container.register(INavigationService.class, navigationService);
        container.register(IOpenTabsManager.class, new OpenTabsManager(historyDataSource, navigationService));
        container.register(MyTracker.class, tracker);
        container.register(ICacheDirectoryManager.class, cacheManager);
        container.register(IPagesSerializationService.class, new PagesSerializationService(cacheManager, new SerializationService()));
        container.register(BitmapMemoryCache.class, bitmapMemoryCache);
        container.register(HttpImageManager.class, imageManager);
        container.register(IBitmapManager.class, new BitmapManager(imageManager));
        container.register(HistoryDataSource.class, historyDataSource);
        container.register(FavoritesDataSource.class, favoritesDataSource);
        container.register(HiddenThreadsDataSource.class, hiddenThreadsDataSource);
        container.register(DownloadFileService.class, downloadFileService);
        container.register(ThreadImagesService.class, new ThreadImagesService());

        historyDataSource.open();
        favoritesDataSource.open();
        hiddenThreadsDataSource.open();

        tracker.startSession(this);
        cacheManager.trimCacheIfNeeded();
    }

    @Override
    public void onTerminate() {
        Factory.getContainer().resolve(MyTracker.class).stopSession();

        super.onTerminate();
    }

    public static DefaultHttpClient getHttpClient() {
        return Factory.getContainer().resolve(DefaultHttpClient.class);
    }

    public ApplicationSettings getSettings() {
        return Factory.getContainer().resolve(ApplicationSettings.class);
    }

    public IJsonApiReader getJsonApiReader() {
        return Factory.getContainer().resolve(IJsonApiReader.class);
    }

    public IPostSender getPostSender() {
        return Factory.getContainer().resolve(IPostSender.class);
    }

    public IDraftPostsStorage getDraftPostsStorage() {
        return Factory.getContainer().resolve(IDraftPostsStorage.class);
    }

    public MyTracker getTracker() {
        return Factory.getContainer().resolve(MyTracker.class);
    }

    public IBitmapManager getBitmapManager() {
        return Factory.getContainer().resolve(IBitmapManager.class);
    }

    public IOpenTabsManager getOpenTabsManager() {
        return Factory.getContainer().resolve(IOpenTabsManager.class);
    }

    public ICacheDirectoryManager getCacheManager() {
        return Factory.getContainer().resolve(ICacheDirectoryManager.class);
    }

    public IPagesSerializationService getSerializationService() {
        return Factory.getContainer().resolve(IPagesSerializationService.class);
    }

    public HistoryDataSource getHistoryDataSource() {
        return Factory.getContainer().resolve(HistoryDataSource.class);
    }

    @Override
    public File getCacheDir() {
        // NOTE: this method is used in Android 2.2 and higher
        return Factory.getContainer().resolve(ICacheDirectoryManager.class).getCurrentCacheDirectory();
    }
}
