package org.citra.citra_emu.features.settings.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;

import org.citra.citra_emu.NativeLibrary;
import org.citra.citra_emu.R;
import org.citra.citra_emu.utils.DirectoryInitialization;
import org.citra.citra_emu.utils.EmulationMenuSettings;

import java.util.Objects;

public final class SettingsActivity extends AppCompatActivity implements SettingsActivityView {
    private static final String ARG_MENU_TAG = "menu_tag";
    private static final String ARG_GAME_ID = "game_id";
    private static final String FRAGMENT_TAG = "settings";
    private final SettingsActivityPresenter mPresenter = new SettingsActivityPresenter(this);

    private Observer<DirectoryInitialization.DirectoryInitializationState> mDirectoryStateObserver;

    private ProgressBar mProgressBar;

    public static void launch(Context context, String menuTag, String gameId) {
        Intent settings = new Intent(context, SettingsActivity.class);
        settings.putExtra(ARG_MENU_TAG, menuTag);
        settings.putExtra(ARG_GAME_ID, gameId);
        context.startActivity(settings);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        mProgressBar = findViewById(R.id.progress_bar);

        Intent launcher = getIntent();
        String gameID = launcher.getStringExtra(ARG_GAME_ID);
        String menuTag = launcher.getStringExtra(ARG_MENU_TAG);

        mPresenter.onCreate(savedInstanceState, menuTag, gameID);

        // Show "Back" button in the action bar for navigation
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);

        return true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // Critical: If super method is not called, rotations will be busted.
        super.onSaveInstanceState(outState);
        mPresenter.saveState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPresenter.onStart();
    }

    /**
     * If this is called, the user has left the settings screen (potentially through the
     * home button) and will expect their changes to be persisted. So we kick off an
     * IntentService which will do so on a background thread.
     */
    @Override
    protected void onStop() {
        super.onStop();

        mPresenter.onStop(isFinishing());

        // Update framebuffer layout when closing the settings
        NativeLibrary.NotifyOrientationChange(EmulationMenuSettings.getLandscapeScreenLayout(),
                getWindowManager().getDefaultDisplay().getRotation());
    }

    @Override
    public void onBackPressed() {
        mPresenter.onBackPressed();
    }


    @Override
    public void showSettingsFragment(String menuTag, boolean addToStack, String gameID) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (addToStack) {
            if (areSystemAnimationsEnabled()) {
                transaction.setCustomAnimations(
                        R.animator.settings_enter,
                        R.animator.settings_exit,
                        R.animator.settings_pop_enter,
                        R.animator.setttings_pop_exit);
            }

            transaction.addToBackStack(null);
            mPresenter.addToStack();
        }
        transaction.replace(R.id.frame_content, SettingsFragment.newInstance(menuTag, gameID), FRAGMENT_TAG);

        transaction.commit();
    }

    private boolean areSystemAnimationsEnabled() {
        float duration = Settings.Global.getFloat(
                getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1);
        float transition = Settings.Global.getFloat(
                getContentResolver(),
                Settings.Global.TRANSITION_ANIMATION_SCALE, 1);
        return duration != 0 && transition != 0;
    }

    @Override
    public void startDirectoryInitialization() {
        DirectoryInitialization.start(this);
        mDirectoryStateObserver = directoryInitializationState -> {
            if (directoryInitializationState == DirectoryInitialization.DirectoryInitializationState.CITRA_DIRECTORIES_INITIALIZED) {
                hideLoading();
                mPresenter.loadSettingsUI();
            } else if (directoryInitializationState == DirectoryInitialization.DirectoryInitializationState.EXTERNAL_STORAGE_PERMISSION_NEEDED) {
                showPermissionNeededHint();
                hideLoading();
            } else if (directoryInitializationState == DirectoryInitialization.DirectoryInitializationState.CANT_FIND_EXTERNAL_STORAGE) {
                showExternalStorageNotMountedHint();
                hideLoading();
            }
        };
        DirectoryInitialization.directoryState.observe(this, mDirectoryStateObserver);
    }

    @Override
    public void stopObservingDirectoryInitialization() {
        if (mDirectoryStateObserver != null) {
            DirectoryInitialization.directoryState.removeObserver(mDirectoryStateObserver);
            mDirectoryStateObserver = null;
        }
    }

    @Override
    public void showLoading() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showPermissionNeededHint() {
        Toast.makeText(this, R.string.write_permission_needed, Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void showExternalStorageNotMountedHint() {
        Toast.makeText(this, R.string.external_storage_not_mounted, Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public org.citra.citra_emu.features.settings.model.Settings getSettings() {
        return mPresenter.getSettings();
    }

    @Override
    public void setSettings(org.citra.citra_emu.features.settings.model.Settings settings) {
        mPresenter.setSettings(settings);
    }

    @Override
    public void onSettingsFileLoaded(org.citra.citra_emu.features.settings.model.Settings settings) {
        SettingsFragmentView fragment = getFragment();

        if (fragment != null) {
            fragment.onSettingsFileLoaded(settings);
        }
    }

    @Override
    public void onSettingsFileNotFound() {
        SettingsFragmentView fragment = getFragment();

        if (fragment != null) {
            fragment.loadDefaultSettings();
        }
    }

    @Override
    public void showToastMessage(String message, boolean is_long) {
        Toast.makeText(this, message, is_long ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }

    @Override
    public void popBackStack() {
        getSupportFragmentManager().popBackStackImmediate();
    }

    @Override
    public void onSettingChanged() {
        mPresenter.onSettingChanged();
    }

    private SettingsFragment getFragment() {
        return (SettingsFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
    }
}
