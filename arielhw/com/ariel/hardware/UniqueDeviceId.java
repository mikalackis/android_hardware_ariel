/*
 * Copyright (C) 2016 The CyanogenMod Project
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

package com.ariel.hardware;

import android.util.Log;

import com.ariel.internal.util.FileUtils;

import android.os.Build;

import java.util.UUID;

/**
 * Generate a unique but deterministic ID for this hardware, based on unchangeable
 * hardware serial numbers.
 */
public class UniqueDeviceId {

    private static final String TAG = "UniqueDeviceId";

    private static final int TYPE_MMC0_CID = 0;

    private static String sUniqueId = null;

    private static final String MMC_TYPE = "/sys/block/mmcblk0/device/type";
    private static final String MMC_CID  = "/sys/block/mmcblk0/device/cid";

    /**
     * Whether device supports reporting a unique device id.
     *
     * @return boolean Supported devices must return always true
     */
    public static boolean isSupported() {
        return FileUtils.isFileReadable(MMC_TYPE) &&
               FileUtils.isFileReadable(MMC_CID) &&
               getUniqueDeviceIdInternal() != null;
    }

    /**
     * This method retreives a unique ID for the device.
     *
     * @return String The unique device ID
     */
    public static String getUniqueDeviceId() {
        return getUniqueDeviceIdInternal();
    }

    private static String getUniqueDeviceIdInternal() {
        if (sUniqueId != null) {
            return sUniqueId;
        }

        try {
            String sMmcType = FileUtils.readOneLine(MMC_TYPE);
            String sCid = FileUtils.readOneLine(MMC_CID);
            if ("MMC".equals(sMmcType) && sCid != null) {
                sCid = sCid.trim();
                if (sCid.length() == 32) {
                    sUniqueId = String.format("%03x00000%32s", TYPE_MMC0_CID, sCid);
                    return sUniqueId;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get unique device ID: " + e.getMessage());
            return null;
        }

        /* Any additional types should be added here. */

        return null;
    }

    public static String getUniquePseudoDeviceId() {
        return getUniquePseudoID();
    }

    private static String getUniquePseudoID() {
        // If all else fails, if the user does have lower than API 9 (lower
        // than Gingerbread), has reset their device or 'Secure.ANDROID_ID'
        // returns 'null', then simply the ID returned will be solely based
        // off their Android device information. This is where the collisions
        // can happen.
        // Thanks http://www.pocketmagic.net/?p=1662!
        // Try not to use DISPLAY, HOST or ID - these items could change.
        // If there are collisions, there will be overlapping data
        String m_szDevIDShort = UUID_PREFIX + (Build.BOARD.length() % 10)
                + (Build.BRAND.length() % 10) + (Build.CPU_ABI.length() % 10)
                + (Build.DEVICE.length() % 10)
                + (Build.MANUFACTURER.length() % 10)
                + (Build.MODEL.length() % 10) + (Build.PRODUCT.length() % 10);

        // Thanks to @Roman SL!
        // http://stackoverflow.com/a/4789483/950427
        // Only devices with API >= 9 have android.os.Build.SERIAL
        // http://developer.android.com/reference/android/os/Build.html#SERIAL
        // If a user upgrades software or roots their device, there will be a
        // duplicate entry
        String serial = null;
        try {
            serial = Build.class.getField("SERIAL").get(null)
                    .toString();

            // Go ahead and return the serial for api => 9
            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode())
                    .toString();
        } catch (Exception exception) {
            // String needs to be initialized
            serial = "serial"; // some value
        }

        // Thanks @Joe!
        // http://stackoverflow.com/a/2853253/950427
        // Finally, combine the values we have found by using the UUID class to
        // create a unique identifier
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode())
                .toString();
    }
}
