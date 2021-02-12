package org.citra.citra_emu.features.settings.ui;

import org.citra.citra_emu.features.settings.model.Settings;

/**
 * Abstraction for the Activity that manages SettingsFragments.
 */
public interface SettingsActivityView {
    /**
     * Show a new SettingsFragment.
     *
     * @param menuTag    Identifier for the settings group that should be displayed.
     * @param addToStack Whether or not this fragment should replace a previous one.
     */
    void showSettingsFragment(String menuTag, boolean addToStack, String gameId);

    /**
     * Called by a contained Fragment to get access to the Setting HashMap
     * loaded from disk, so that each Fragment doesn't need to perform its own
     * read operation.
     *
     * @return A possibly null HashMap of Settings.
     */
    Settings getSettings();

    /**
     * Used to provide the Activity with Settings HashMaps if a Fragment already
     * has one; for example, if a rotation occurs, the Fragment will not be killed,
     * but the Activity will, so the Activity needs to have its HashMaps resupplied.
     *
     * @param settings The ArrayList of all the Settings HashMaps.
     */
    void setSettings(Settings settings);

    /**
     * Called when an asynchronous load operation completes.
     *
     * @param settings The (possibly null) result of the ini load operation.
     */
    void onSettingsFileLoaded(Settings settings);

    /**
     * Called when an asynchronous load operation fails.
     */
    void onSettingsFileNotFound();

    /**
     * Display a popup text message on screen.
     *
     * @param message The contents of the onscreen message.
     * @param is_long Whether this should be a long Toast or short one.
     */
    void showToastMessage(String message, boolean is_long);

    /**
     * Show the previous fragment.
     */
    void popBackStack();

    /**
     * End the activity.
     */
    void finish();

    /**
     * Called by a containing Fragment to tell the Activity that a setting was changed;
     * unless this has been called, the Activity will not save to disk.
     */
    void onSettingChanged();

    /**
     * Show loading dialog while loading the settings
     */
    void showLoading();

    /**
     * Hide the loading the dialog
     */
    void hideLoading();

    /**
     * Show a hint to the user that the app needs write to external storage access
     */
    void showPermissionNeededHint();

    /**
     * Show a hint to the user that the app needs the external storage to be mounted
     */
    void showExternalStorageNotMountedHint();

    /**
     * Start the DirectoryInitialization and observe the result.
     */
    void startDirectoryInitialization();

    /**
     * Stop observing the DirectoryInitialization.
     */
    void stopObservingDirectoryInitialization();
}
