package org.citra.citra_emu.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceManager;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import org.citra.citra_emu.CitraApplication;
import org.citra.citra_emu.R;
import org.citra.citra_emu.features.settings.utils.SettingsFile;
import org.citra.citra_emu.ui.main.MainActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton to manage user billing state.
 */
public class BillingManager implements LifecycleObserver, PurchasesUpdatedListener {
    private final String BILLING_SKU_PREMIUM = "citra.citra_emu.product_id.premium";

    private BillingClient mBillingClient;
    private SkuDetails mSkuPremium;
    private boolean mIsPremiumActive = false;
    private boolean mIsServiceConnected = false;
    private Runnable mUpdateBillingCallback;

    private static final SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(CitraApplication.getAppContext());

    private static volatile BillingManager INSTANCE;

    private BillingManager() {
    }

    public static BillingManager getInstance() {
        if (INSTANCE == null) {
            synchronized (BillingManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BillingManager();
                }
            }
        }
        return INSTANCE;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void create() {
        Log.debug("Create a new billing client");
        mBillingClient = BillingClient.newBuilder(CitraApplication.getAppContext())
                .enablePendingPurchases().setListener(this).build();
        if (!mIsServiceConnected) {
            querySkuDetails();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void destroy() {
        Log.debug("Destroy billing client");
        if (mIsServiceConnected) {
            mBillingClient.endConnection();
        }
    }

    static public boolean isPremiumCached() {
        return mPreferences.getBoolean(SettingsFile.KEY_PREMIUM, false);
    }

    /**
     * @return true if Premium subscription is currently active
     */
    public boolean isPremiumActive() {
        return mIsPremiumActive;
    }

    /**
     * Invokes the billing flow for Premium
     *
     * @param callback Optional callback, called once, on completion of billing
     */
    public void invokePremiumBilling(Activity activity, Runnable callback) {
        if (mSkuPremium == null) {
            return;
        }

        // Optional callback to refresh the UI for the caller when billing completes
        mUpdateBillingCallback = callback;

        // Invoke the billing flow
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(mSkuPremium)
                .build();
        mBillingClient.launchBillingFlow(activity, flowParams);
    }

    private void updatePremiumState(boolean isPremiumActive) {
        mIsPremiumActive = isPremiumActive;

        // Cache state for synchronous UI
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(SettingsFile.KEY_PREMIUM, isPremiumActive);
        editor.apply();

        // No need to show button in action bar if Premium is active
        MainActivity.setPremiumButtonVisible(!isPremiumActive);
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchaseList) {
        if (purchaseList == null || purchaseList.isEmpty()) {
            // Premium is not active, or billing is unavailable
            updatePremiumState(false);
            return;
        }

        Purchase premiumPurchase = null;
        for (Purchase purchase : purchaseList) {
            if (purchase.getSkus().contains(BILLING_SKU_PREMIUM)) {
                premiumPurchase = purchase;
            }
        }

        if (premiumPurchase != null && premiumPurchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // Premium has been purchased
            updatePremiumState(true);

            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!premiumPurchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(premiumPurchase.getPurchaseToken())
                                .build();

                AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = billingResult1 ->
                        Toast.makeText(CitraApplication.getAppContext(), R.string.premium_settings_welcome, Toast.LENGTH_SHORT).show();
                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
            }

            if (mUpdateBillingCallback != null) {
                try {
                    mUpdateBillingCallback.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mUpdateBillingCallback = null;
            }
        }
    }

    private void onQuerySkuDetailsFinished(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
        if (skuDetailsList == null) {
            // This can happen when no user is signed in
            return;
        }

        if (skuDetailsList.isEmpty()) {
            return;
        }

        mSkuPremium = skuDetailsList.get(0);

        queryPurchases();
    }

    private void querySkuDetails() {
        executeServiceRequest(() -> {
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            List<String> skuList = new ArrayList<>();

            skuList.add(BILLING_SKU_PREMIUM);
            params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);

            mBillingClient.querySkuDetailsAsync(params.build(), this::onQuerySkuDetailsFinished);
        });
    }

    private void onQueryPurchasesFinished(BillingResult billingResult, List<Purchase> purchaseList) {
        // Have we been disposed of in the meantime? If so, or bad result code, then quit
        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            updatePremiumState(false);
            return;
        }
        // Update the UI and purchases inventory with new list of purchases
        onPurchasesUpdated(billingResult, purchaseList);
    }

    private void queryPurchases() {
        executeServiceRequest(() ->
                mBillingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, this::onQueryPurchasesFinished));
    }

    private void startServiceConnection(final Runnable executeOnFinish) {
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    mIsServiceConnected = true;
                }

                if (executeOnFinish != null) {
                    executeOnFinish.run();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                mIsServiceConnected = false;
            }
        });
    }

    private void executeServiceRequest(Runnable runnable) {
        if (mIsServiceConnected) {
            runnable.run();
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            startServiceConnection(runnable);
        }
    }
}
