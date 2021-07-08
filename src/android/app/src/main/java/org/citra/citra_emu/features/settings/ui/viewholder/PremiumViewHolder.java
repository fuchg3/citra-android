package org.citra.citra_emu.features.settings.ui.viewholder;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import org.citra.citra_emu.R;
import org.citra.citra_emu.features.settings.model.view.SettingsItem;
import org.citra.citra_emu.features.settings.ui.SettingsAdapter;
import org.citra.citra_emu.utils.BillingManager;
import org.citra.citra_emu.utils.Log;

public final class PremiumViewHolder extends SettingViewHolder {
    private final Activity mActivity;
    private final BillingManager mBillingManager;
    private TextView mHeaderName;
    private TextView mTextDescription;

    public PremiumViewHolder(View itemView, SettingsAdapter adapter, Activity activity) {
        super(itemView, adapter);
        mActivity = activity;
        mBillingManager = BillingManager.getInstance();
        itemView.setOnClickListener(this);
    }

    @Override
    protected void findViews(View root) {
        mHeaderName = root.findViewById(R.id.text_setting_name);
        mTextDescription = root.findViewById(R.id.text_setting_description);
    }

    @Override
    public void bind(SettingsItem item) {
        updateText();
    }

    @Override
    public void onClick(View clicked) {
        if (mBillingManager.isPremiumActive()) {
            return;
        }

        // Invoke billing flow if Premium is not already active, then refresh the UI to indicate
        // the purchase has completed.
        mBillingManager.invokePremiumBilling(mActivity, this::updateText);
    }

    /**
     * Update the text shown to the user, based on whether Premium is active
     */
    private void updateText() {
        if (mBillingManager.isPremiumActive()) {
            mHeaderName.setText(R.string.premium_settings_welcome);
            mTextDescription.setText(R.string.premium_settings_welcome_description);
        } else {
            mHeaderName.setText(R.string.premium_settings_upsell);
            mTextDescription.setText(R.string.premium_settings_upsell_description);
        }
    }
}
