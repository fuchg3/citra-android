package org.citra.citra_emu.features.settings.ui.viewholder;

import android.view.View;

import org.citra.citra_emu.features.settings.model.view.SettingsItem;
import org.citra.citra_emu.features.settings.ui.SettingsAdapter;

public final class EmptyViewHolder extends SettingViewHolder {
    public EmptyViewHolder(View itemView, SettingsAdapter adapter) {
        super(itemView, adapter);
    }

    @Override protected void findViews(View root) {
        // Do nothing
    }

    @Override public void bind(SettingsItem item) {
        // Do nothing
    }

    @Override public void onClick(View clicked) {
        // Do nothing
    }
}
