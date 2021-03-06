package org.citra.citra_emu.ui.main;

import android.os.SystemClock;

import org.citra.citra_emu.BuildConfig;
import org.citra.citra_emu.CitraApplication;
import org.citra.citra_emu.R;
import org.citra.citra_emu.features.settings.model.Settings;
import org.citra.citra_emu.features.settings.utils.SettingsFile;
import org.citra.citra_emu.model.GameDatabase;
import org.citra.citra_emu.utils.AddDirectoryHelper;
import org.citra.citra_emu.utils.DirectoryInitialization;

public final class MainPresenter {
    public static final int REQUEST_ADD_DIRECTORY = 1;
    public static final int REQUEST_INSTALL_CIA = 2;

    private final MainView mView;
    private String mDirToAdd;
    private long mLastClickTime = 0;

    public MainPresenter(MainView view) {
        mView = view;
    }

    public void onCreate() {
        String versionName = BuildConfig.VERSION_NAME;
        mView.setVersionString(versionName);
        if (DirectoryInitialization.areCitraDirectoriesReady()) {
            refreshGameList();
        }
    }

    public void launchFileListActivity(int request) {
        if (mView != null) {
            mView.launchFileListActivity(request);
        }
    }

    public boolean handleOptionSelection(int itemId) {
        // Double-click prevention, using threshold of 500 ms
        if (SystemClock.elapsedRealtime() - mLastClickTime < 500) {
            return false;
        }
        mLastClickTime = SystemClock.elapsedRealtime();

        if (itemId == R.id.menu_settings_core) {
            mView.launchSettingsActivity(SettingsFile.FILE_NAME_CONFIG);
            return true;
        } else if (itemId == R.id.button_add_directory) {
            launchFileListActivity(REQUEST_ADD_DIRECTORY);
            return true;
        } else if (itemId == R.id.button_install_cia) {
            launchFileListActivity(REQUEST_INSTALL_CIA);
            return true;
        } else if (itemId == R.id.button_premium) {
            mView.launchSettingsActivity(Settings.SECTION_PREMIUM);
            return true;
        } else {
            return false;
        }
    }

    public void addDirIfNeeded(AddDirectoryHelper helper) {
        if (mDirToAdd != null) {
            helper.addDirectory(mDirToAdd, mView::refresh);

            mDirToAdd = null;
        }
    }

    public void onDirectorySelected(String dir) {
        mDirToAdd = dir;
    }

    public void refreshGameList() {
        GameDatabase databaseHelper = CitraApplication.databaseHelper;
        databaseHelper.scanLibrary(databaseHelper.getWritableDatabase());
        mView.refresh();
    }
}
