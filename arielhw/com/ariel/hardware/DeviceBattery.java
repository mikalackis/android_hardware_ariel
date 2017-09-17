/*
 * Copyright (C) 2015 The CyanogenMod Project
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

import android.os.FileUtils;
import android.os.RemoteException;
import android.util.Slog;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;

import com.android.server.am.BatteryStatsService;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatteryStatsImpl;

import android.os.BatteryStats;
import android.util.Log;

import android.os.MemoryFile;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;

import android.os.SystemClock;

/**
 * Support for storage of key-value pairs which persists across device wipes /
 * factory resets. This is used for various security related features. A
 * reasonable implementation of this needs to store items on a partition which
 * is unaffected by factory reset. The actual format of the storage is left up
 * to the implementation. The implementation in this file is suitable for
 * devices which have a /persist partition (recent QCOM devices fit this
 * criteria).
 */
public class DeviceBattery {

    private static final String TAG = "DeviceBattery";

    private static final boolean DEBUG = false;

    private static IBatteryStats mBatteryStats;

    static {
        mBatteryStats = BatteryStatsService.getService();
    }

    public static long getRemainingBatteryTime() {
        try {
            return mBatteryStats.computeBatteryTimeRemaining();
        } catch (RemoteException e) {
            return -101;
        }
    }

    public static long getChargeRemainingTime() {
        try {
            return mBatteryStats.computeChargeTimeRemaining();
        } catch (RemoteException e) {
            return -101;
        }
    }

    public static boolean isCharging() {
        try {
            return mBatteryStats.isCharging();
        } catch (RemoteException e) {
            return false;
        }
    }

    public static long getScreenOnTime() {
        BatteryStats batteryStats = getStats(mBatteryStats);
        return batteryStats.getScreenOnTime(SystemClock.elapsedRealtime() * 1000,
                BatteryStats.STATS_SINCE_CHARGED) / 1000;
    }

    private static BatteryStatsImpl getStats(IBatteryStats service) {
        try {
            ParcelFileDescriptor pfd = service.getStatisticsStream();
            if (pfd != null) {
                try (FileInputStream fis = new ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
                    byte[] data = readFully(fis, MemoryFile.getSize(pfd.getFileDescriptor()));
                    Parcel parcel = Parcel.obtain();
                    parcel.unmarshall(data, 0, data.length);
                    parcel.setDataPosition(0);
                    BatteryStatsImpl stats = com.android.internal.os.BatteryStatsImpl.CREATOR
                            .createFromParcel(parcel);
                    return stats;
                } catch (IOException e) {
                    Log.w(TAG, "Unable to read statistics stream", e);
                }
            }
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException:", e);
        }
        return new BatteryStatsImpl();
    }

    public static byte[] readFully(FileInputStream stream) throws java.io.IOException {
        return readFully(stream, stream.available());
    }

    public static byte[] readFully(FileInputStream stream, int avail) throws java.io.IOException {
        int pos = 0;
        byte[] data = new byte[avail];
        while (true) {
            int amt = stream.read(data, pos, data.length - pos);
            //Log.i("foo", "Read " + amt + " bytes at " + pos
            //        + " of avail " + data.length);
            if (amt <= 0) {
                //Log.i("foo", "**** FINISHED READING: pos=" + pos
                //        + " len=" + data.length);
                return data;
            }
            pos += amt;
            avail = stream.available();
            if (avail > data.length - pos) {
                byte[] newData = new byte[pos + avail];
                System.arraycopy(data, 0, newData, 0, pos);
                data = newData;
            }
        }
    }


}
