/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tv.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.preference.PreferenceActivity;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;

import com.android.internal.util.XmlUtils;
import com.android.tv.settings.accessories.AccessoryUtils;
import com.android.tv.settings.accessories.BluetoothAccessoryActivity;
import com.android.tv.settings.accessories.BluetoothConnectionsManager;
import com.android.tv.settings.accounts.AccountSettingsActivity;
import com.android.tv.settings.accounts.AddAccountWithTypeActivity;
import com.android.tv.settings.accounts.AuthenticatorHelper;
import com.android.tv.settings.connectivity.ConnectivityStatusIconUriGetter;
import com.android.tv.settings.connectivity.ConnectivityStatusTextGetter;
import com.android.tv.settings.connectivity.WifiNetworksActivity;
import com.android.tv.settings.device.sound.SoundActivity;
import com.android.tv.settings.users.RestrictedProfileDialogFragment;
import com.android.tv.settings.util.UriUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

/**
 * Gets the list of browse headers and browse items.
 */
public class BrowseInfo extends BrowseInfoBase {

    private static final String TAG = "TvSettings.BrowseInfo";
    private static final boolean DEBUG = false;

    private static final String ACCOUNT_TYPE_GOOGLE = "com.google";

    private static final String ETHERNET_PREFERENCE_KEY = "ethernet";

    interface XmlReaderListener {
        void handleRequestedNode(Context context, XmlResourceParser parser, AttributeSet attrs)
                throws org.xmlpull.v1.XmlPullParserException, IOException;
    }

    static class SoundActivityImageUriGetter implements MenuItem.UriGetter {

        private final Context mContext;

        SoundActivityImageUriGetter(Context context) {
            mContext = context;
        }

        @Override
        public String getUri() {
            return UriUtils.getAndroidResourceUri(mContext.getResources(),
                    SoundActivity.getIconResource(mContext.getContentResolver()));
        }
    }

    static class XmlReader {

        private final Context mContext;
        private final int mXmlResource;
        private final String mRootNodeName;
        private final String mNodeNameRequested;
        private final XmlReaderListener mListener;

        XmlReader(Context context, int xmlResource, String rootNodeName, String nodeNameRequested,
                XmlReaderListener listener) {
            mContext = context;
            mXmlResource = xmlResource;
            mRootNodeName = rootNodeName;
            mNodeNameRequested = nodeNameRequested;
            mListener = listener;
        }

        void read() {
            XmlResourceParser parser = null;
            try {
                parser = mContext.getResources().getXml(mXmlResource);
                AttributeSet attrs = Xml.asAttributeSet(parser);

                int type;
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && type != XmlPullParser.START_TAG) {
                    // Parse next until start tag is found
                }

                String nodeName = parser.getName();
                if (!mRootNodeName.equals(nodeName)) {
                    throw new RuntimeException("XML document must start with <" + mRootNodeName
                            + "> tag; found" + nodeName + " at " + parser.getPositionDescription());
                }

                final int outerDepth = parser.getDepth();
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                    if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                        continue;
                    }

                    nodeName = parser.getName();
                    if (mNodeNameRequested.equals(nodeName)) {
                        mListener.handleRequestedNode(mContext, parser, attrs);
                    } else {
                        XmlUtils.skipCurrentTag(parser);
                    }
                }

            } catch (XmlPullParserException|IOException e) {
                throw new RuntimeException("Error parsing headers", e);
            } finally {
                if (parser != null)
                    parser.close();
            }
        }
    }

    private static final String PREF_KEY_ADD_ACCOUNT = "add_account";
    private static final String PREF_KEY_WIFI = "network";
    private static final String PREF_KEY_DEVELOPER = "developer";
    private static final String PREF_KEY_INPUTS = "inputs";
    private static final String PREF_KEY_HOME = "home";
    private static final String PREF_KEY_CAST = "cast";
    private static final String PREF_KEY_GOOGLESETTINGS = "googleSettings";
    private static final String PREF_KEY_USAGE_AND_DIAG = "usageAndDiag";

    private final Context mContext;
    private final AuthenticatorHelper mAuthenticatorHelper;
    private int mNextItemId;
    private int mAccountHeaderId;
    private final BluetoothAdapter mBtAdapter;
    private final Object mGuard = new Object();
    private MenuItem mWifiItem = null;
    private MenuItem mSoundsItem = null;
    private MenuItem mDeveloperOptionItem = null;
    private ArrayObjectAdapter mWifiRow = null;
    private ArrayObjectAdapter mSoundsRow = null;
    private ArrayObjectAdapter mDeveloperRow = null;

    private final PreferenceUtils mPreferenceUtils;
    private boolean mDeveloperEnabled;
    private final boolean mInputSettingNeeded;

    BrowseInfo(Context context) {
        mContext = context;
        mAuthenticatorHelper = new AuthenticatorHelper();
        mAuthenticatorHelper.updateAuthDescriptions(context);
        mAuthenticatorHelper.onAccountsUpdated(context, null);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mNextItemId = 0;
        mPreferenceUtils = new PreferenceUtils(context);
        mDeveloperEnabled = mPreferenceUtils.isDeveloperEnabled();
        mInputSettingNeeded = isInputSettingNeeded();
    }

    void init() {
        synchronized (mGuard) {
            mHeaderItems.clear();
            mRows.clear();
            int settingsXml = isRestricted() ? R.xml.restricted_main : R.xml.main;
            new XmlReader(mContext, settingsXml, "preference-headers", "header",
                    new HeaderXmlReaderListener()).read();
            updateAccessories(R.id.accessories);
        }
    }

    void checkForDeveloperOptionUpdate() {
        final boolean developerEnabled = mPreferenceUtils.isDeveloperEnabled();
        if (developerEnabled != mDeveloperEnabled) {
            mDeveloperEnabled = developerEnabled;
            if (mDeveloperOptionItem != null) {
                mDeveloperRow.add (mDeveloperOptionItem);
                mDeveloperOptionItem = null;
            }
        }
    }

    private class HeaderXmlReaderListener implements XmlReaderListener {
        @Override
        public void handleRequestedNode(Context context, XmlResourceParser parser,
                AttributeSet attrs)
                throws XmlPullParserException, IOException {
            TypedArray sa = mContext.getResources().obtainAttributes(attrs,
                    com.android.internal.R.styleable.PreferenceHeader);
            final int headerId = sa.getResourceId(
                    com.android.internal.R.styleable.PreferenceHeader_id,
                    (int) PreferenceActivity.HEADER_ID_UNDEFINED);
            String title = getStringFromTypedArray(sa,
                    com.android.internal.R.styleable.PreferenceHeader_title);
            sa.recycle();
            sa = context.getResources().obtainAttributes(attrs, R.styleable.CanvasSettings);
            int preferenceRes = sa.getResourceId(R.styleable.CanvasSettings_preference, 0);
            sa.recycle();
            mHeaderItems.add(new HeaderItem(headerId, title));
            final ArrayObjectAdapter currentRow = new ArrayObjectAdapter();
            mRows.put(headerId, currentRow);
            if (headerId != R.id.accessories) {
                new XmlReader(context, preferenceRes, "PreferenceScreen", "Preference",
                        new PreferenceXmlReaderListener(headerId, currentRow)).read();
            }
        }
    }

    private boolean isRestricted() {
        return RestrictedProfileDialogFragment.isRestrictedProfileInEffect(mContext);
    }

    private class PreferenceXmlReaderListener implements XmlReaderListener {

        private final int mHeaderId;
        private final ArrayObjectAdapter mRow;

        PreferenceXmlReaderListener(int headerId, ArrayObjectAdapter row) {
            mHeaderId = headerId;
            mRow = row;
        }

        @Override
        public void handleRequestedNode(Context context, XmlResourceParser parser,
                AttributeSet attrs) throws XmlPullParserException, IOException {
            TypedArray sa = context.getResources().obtainAttributes(attrs,
                    com.android.internal.R.styleable.Preference);

            String key = getStringFromTypedArray(sa,
                    com.android.internal.R.styleable.Preference_key);
            String title = getStringFromTypedArray(sa,
                    com.android.internal.R.styleable.Preference_title);
            int iconRes = sa.getResourceId(com.android.internal.R.styleable.Preference_icon,
                    R.drawable.settings_default_icon);
            sa.recycle();

            if (PREF_KEY_ADD_ACCOUNT.equals(key)) {
                mAccountHeaderId = mHeaderId;
                addAccounts(mRow);
            } else if (PREF_KEY_HOME.equals(key)) {
                // Only show home screen setting if there's a system app to handle the intent.
                Intent recIntent = getIntent(parser, attrs);
                if (systemIntentIsHandled(recIntent) != null) {
                    mRow.add(new MenuItem.Builder()
                            .id(mNextItemId++)
                            .title(title)
                            .imageResourceId(mContext, iconRes)
                            .intent(recIntent)
                            .build());
                }
            } else if (PREF_KEY_CAST.equals(key)) {
                Intent i = getIntent(parser, attrs);
                if (systemIntentIsHandled(i) != null) {
                    mRow.add(new MenuItem.Builder()
                            .id(mNextItemId++)
                            .title(title)
                            .imageResourceId(mContext, iconRes)
                            .intent(i)
                            .build());
                }
            } else if (PREF_KEY_GOOGLESETTINGS.equals(key)) {
                Intent i = getIntent(parser, attrs);
                final ResolveInfo info = systemIntentIsHandled(i);
                if (info != null) {
                    try {
                        final PackageManager packageManager = context.getPackageManager();
                        final String packageName = info.resolvePackageName != null ?
                                info.resolvePackageName : info.activityInfo.packageName;
                        final Resources targetResources =
                                packageManager.getResourcesForApplication(packageName);
                        final String targetTitle = info.loadLabel(packageManager).toString();
                        mRow.add(new MenuItem.Builder()
                                .id(mNextItemId++)
                                .title(targetTitle)
                                .imageResourceId(targetResources, info.iconResourceId)
                                .intent(i)
                                .build());
                    } catch (Exception e) {
                        Log.e(TAG, "Error adding google settings", e);
                    }
                }
            } else if (PREF_KEY_USAGE_AND_DIAG.equals(key)) {
                Intent i = getIntent(parser, attrs);
                if (systemIntentIsHandled(i) != null) {
                    mRow.add(new MenuItem.Builder()
                            .id(mNextItemId++)
                            .title(title)
                            .imageResourceId(mContext, iconRes)
                            .intent(i)
                            .build());
                }
            } else if (!PREF_KEY_INPUTS.equals(key) || mInputSettingNeeded) {
                MenuItem.TextGetter descriptionGetter = getDescriptionTextGetterFromKey(key);
                MenuItem.UriGetter uriGetter = getIconUriGetterFromKey(key);
                MenuItem.Builder builder = new MenuItem.Builder().id(mNextItemId++).title(title)
                        .descriptionGetter(descriptionGetter)
                        .intent(getIntent(parser, attrs));
                if(uriGetter == null) {
                    builder.imageResourceId(mContext, iconRes);
                } else {
                    builder.imageUriGetter(uriGetter);
                }
                final MenuItem item = builder.build();
                if (key.equals(PREF_KEY_WIFI)) {
                    mWifiRow = mRow;
                    mWifiItem = item;
                    mRow.add(mWifiItem);
                } else if (key.equals(SoundActivity.getPreferenceKey())) {
                    mSoundsRow = mRow;
                    mSoundsItem = item;
                    mRow.add(mSoundsItem);
                } else if (key.equals(PREF_KEY_DEVELOPER) && !mDeveloperEnabled) {
                    mDeveloperRow = mRow;
                    mDeveloperOptionItem = item;
                } else {
                    mRow.add(item);
                }
            }
        }
    }

    void rebuildInfo() {
        init();
    }

    void updateAccounts() {
        synchronized (mGuard) {
            if (isRestricted()) {
                // We don't display the accounts in restricted mode
                return;
            }
            ArrayObjectAdapter row = mRows.get(mAccountHeaderId);
            // Clear any account row cards that are not "Location" or "Security".
            String dontDelete[] = new String[3];
            dontDelete[0] = mContext.getString(R.string.system_location);
            dontDelete[1] = mContext.getString(R.string.system_security);
            dontDelete[2] = mContext.getString(R.string.system_diagnostic);
            int i = 0;
            while (i < row.size ()) {
                MenuItem menuItem = (MenuItem) row.get(i);
                String title = menuItem.getTitle ();
                boolean deleteItem = true;
                for (int j = 0; j < dontDelete.length; ++j) {
                    if (title.equals(dontDelete[j])) {
                        deleteItem = false;
                        break;
                    }
                }
                if (deleteItem) {
                    row.removeItems(i, 1);
                } else {
                    ++i;
                }
            }
            // Add accounts to end of row.
            addAccounts(row);
        }
    }

    void updateAccessories() {
        synchronized (mGuard) {
            updateAccessories(R.id.accessories);
        }
    }

    public void updateWifi(final boolean isEthernetAvailable) {
        if (mWifiItem != null) {
            int index = mWifiRow.indexOf(mWifiItem);
            if (index >= 0) {
                mWifiItem = new MenuItem.Builder().from(mWifiItem)
                        .title(mContext.getString(isEthernetAvailable
                                    ? R.string.connectivity_network : R.string.connectivity_wifi))
                        .build();
                mWifiRow.replace(index, mWifiItem);
            }
        }
    }

    public void updateSounds() {
        if (mSoundsItem != null) {
            int index = mSoundsRow.indexOf(mSoundsItem);
            if (index >= 0) {
                mSoundsRow.notifyArrayItemRangeChanged(index, 1);
            }
        }
    }

    private boolean isInputSettingNeeded() {
        TvInputManager manager = (TvInputManager) mContext.getSystemService(
                Context.TV_INPUT_SERVICE);
        if (manager != null) {
            //for (TvInputInfo input : manager.getTvInputList()) {
              //  if (input.isPassthroughInput()) {
                    return false;
                //}
            //}
        }
        return false;
    }

    private void updateAccessories(int headerId) {
        ArrayObjectAdapter row = mRows.get(headerId);
        row.clear();

        addAccessories(row);

        // Add new accessory activity icon
        ComponentName componentName = new ComponentName("com.android.tv.settings",
                "com.android.tv.settings.accessories.AddAccessoryActivity");
        Intent i = new Intent().setComponent(componentName);
        row.add(new MenuItem.Builder().id(mNextItemId++)
                .title(mContext.getString(R.string.accessories_add))
                .imageResourceId(mContext, R.drawable.ic_settings_bluetooth)
                .intent(i).build());
    }

    private Intent getIntent(XmlResourceParser parser, AttributeSet attrs)
            throws org.xmlpull.v1.XmlPullParserException, IOException {
        Intent intent = null;
        if (parser.next() == XmlPullParser.START_TAG && "intent".equals(parser.getName())) {
            TypedArray sa = mContext.getResources()
                    .obtainAttributes(attrs, com.android.internal.R.styleable.Intent);
            String targetClass = getStringFromTypedArray(
                    sa, com.android.internal.R.styleable.Intent_targetClass);
            String targetPackage = getStringFromTypedArray(
                    sa, com.android.internal.R.styleable.Intent_targetPackage);
            String action = getStringFromTypedArray(
                    sa, com.android.internal.R.styleable.Intent_action);
            if (targetClass != null && targetPackage != null) {
                ComponentName componentName = new ComponentName(targetPackage, targetClass);
                intent = new Intent();
                intent.setComponent(componentName);
            } else if (action != null) {
                intent = new Intent(action);
                if (targetPackage != null) {
                    intent.setPackage(targetPackage);
                }
            }

            XmlUtils.skipCurrentTag(parser);
        }
        return intent;
    }

    private String getStringFromTypedArray(TypedArray sa, int resourceId) {
        String value = null;
        TypedValue tv = sa.peekValue(resourceId);
        if (tv != null && tv.type == TypedValue.TYPE_STRING) {
            if (tv.resourceId != 0) {
                value = mContext.getString(tv.resourceId);
            } else {
                value = tv.string.toString();
            }
        }
        return value;
    }

    private MenuItem.TextGetter getDescriptionTextGetterFromKey(String key) {
        if (WifiNetworksActivity.PREFERENCE_KEY.equals(key)) {
            return ConnectivityStatusTextGetter.createWifiStatusTextGetter(mContext);
        }

        if (ETHERNET_PREFERENCE_KEY.equals(key)) {
            return ConnectivityStatusTextGetter.createEthernetStatusTextGetter(mContext);
        }

        return null;
    }

    private MenuItem.UriGetter getIconUriGetterFromKey(String key) {
        if (SoundActivity.getPreferenceKey().equals(key)) {
            return new SoundActivityImageUriGetter(mContext);
        }

        if (WifiNetworksActivity.PREFERENCE_KEY.equals(key)) {
            return ConnectivityStatusIconUriGetter.createWifiStatusIconUriGetter(mContext);
        }

        return null;
    }

    private void addAccounts(ArrayObjectAdapter row) {
        AccountManager am = AccountManager.get(mContext);
        AuthenticatorDescription[] authTypes = am.getAuthenticatorTypes();
        ArrayList<String> allowableAccountTypes = new ArrayList<>(authTypes.length);
        PackageManager pm = mContext.getPackageManager();

        int googleAccountCount = 0;

        for (AuthenticatorDescription authDesc : authTypes) {
            final Resources resources;
            try {
                resources = pm.getResourcesForApplication(authDesc.packageName);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Authenticator description with bad package name", e);
                continue;
            }

            // Main title text comes from the authenticator description (e.g. "Google").
            String authTitle = null;
            try {
                authTitle = resources.getString(authDesc.labelId);
                if (TextUtils.isEmpty(authTitle)) {
                    authTitle = null;  // Handled later when we add the row.
                }
            } catch (NotFoundException e) {
                if (DEBUG) Log.e(TAG, "Authenticator description with bad label id", e);
            }

            // There exist some authenticators which aren't intended to be user-facing.
            // If the authenticator doesn't have a title or an icon, don't present it to
            // the user as an option.
            if (authTitle != null || authDesc.iconId != 0) {
                allowableAccountTypes.add(authDesc.type);
            }

            Account[] accounts = am.getAccountsByType(authDesc.type);
            if (accounts == null || accounts.length == 0) {
                continue;  // No point in continuing; there aren't any accounts to show.
            }

            // Icon URI to be displayed for each account is based on the type of authenticator.
            String imageUri = null;
            if (ACCOUNT_TYPE_GOOGLE.equals(authDesc.type)) {
                googleAccountCount = accounts.length;
                imageUri = googleAccountIconUri(mContext);
            } else {
                try {
                    imageUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                            authDesc.packageName + '/' +
                            resources.getResourceTypeName(authDesc.iconId) + '/' +
                            resources.getResourceEntryName(authDesc.iconId))
                            .toString();
                } catch (NotFoundException e) {
                    if (DEBUG) Log.e(TAG, "Authenticator has bad resource ids", e);
                }
            }

            // Display an entry for each installed account we have.
            for (final Account account : accounts) {
                Intent i = new Intent(mContext, AccountSettingsActivity.class)
                        .putExtra(AccountSettingsActivity.EXTRA_ACCOUNT, account.name);
                row.add(new MenuItem.Builder().id(mNextItemId++)
                        .title(authTitle != null ? authTitle : account.name)
                        .imageUri(imageUri)
                        .description(authTitle != null ? account.name : null)
                        .intent(i)
                        .build());
            }
        }

        // Never allow restricted profile to add accounts.
        if (!isRestricted()) {

            // If there's already a Google account installed, disallow installing a second one.
            if (googleAccountCount > 0) {
                allowableAccountTypes.remove(ACCOUNT_TYPE_GOOGLE);
            }

            // If there are available account types, add the "add account" button.
            if (!allowableAccountTypes.isEmpty()) {
                Intent i = new Intent().setComponent(new ComponentName("com.android.tv.settings",
                        "com.android.tv.settings.accounts.AddAccountWithTypeActivity"));
                i.putExtra(AddAccountWithTypeActivity.EXTRA_ALLOWABLE_ACCOUNT_TYPES_STRING_ARRAY,
                        allowableAccountTypes.toArray(new String[allowableAccountTypes.size()]));

                row.add(new MenuItem.Builder().id(mNextItemId++)
                        .title(mContext.getString(R.string.add_account))
                        .imageResourceId(mContext, R.drawable.ic_settings_add)
                        .intent(i).build());
            }
        }
    }

    private void addAccessories(ArrayObjectAdapter row) {
        if (mBtAdapter != null) {
            Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();
            if (DEBUG) {
                Log.d(TAG, "List of Bonded BT Devices:");
            }

            Set<String> connectedBluetoothAddresses =
                    BluetoothConnectionsManager.getConnectedSet(mContext);

            for (BluetoothDevice device : bondedDevices) {
                if (DEBUG) {
                    Log.d(TAG, "   Device name: " + device.getName() + " , Class: " +
                            device.getBluetoothClass().getDeviceClass());
                }

                int resourceId = AccessoryUtils.getImageIdForDevice(device);
                Intent i = BluetoothAccessoryActivity.getIntent(mContext, device.getAddress(),
                        device.getName(), resourceId);

                String desc = connectedBluetoothAddresses.contains(device.getAddress())
                        ? mContext.getString(R.string.accessory_connected)
                        : null;

                row.add(new MenuItem.Builder().id(mNextItemId++).title(device.getName())
                        .description(desc).imageResourceId(mContext, resourceId)
                        .intent(i).build());
            }
        }
    }

    private static String googleAccountIconUri(Context context) {
        ShortcutIconResource iconResource = new ShortcutIconResource();
        iconResource.packageName = context.getPackageName();
        iconResource.resourceName = context.getResources().getResourceName(
                R.drawable.ic_settings_google_account);
        return UriUtils.getShortcutIconResourceUri(iconResource).toString();
    }

    private ResolveInfo systemIntentIsHandled(Intent intent) {
        if (mContext == null || intent == null) {
            return null;
        }

        PackageManager pm = mContext.getPackageManager();
        if (pm == null) {
            return null;
        }

        for (ResolveInfo info : pm.queryIntentActivities(intent, 0)) {
            if (info.activityInfo != null && info.activityInfo.enabled &&
                (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) ==
                        ApplicationInfo.FLAG_SYSTEM) {
                return info;
            }
        }
        return null;
    }
}
