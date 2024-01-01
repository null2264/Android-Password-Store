/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.passwordstore.ui.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ShortcutManager
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import app.passwordstore.R
import app.passwordstore.util.auth.BiometricAuthenticator
import app.passwordstore.util.auth.BiometricAuthenticator.Result
import app.passwordstore.util.extensions.sharedPrefs
import app.passwordstore.util.settings.PreferenceKeys
import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.helpers.onClick
import de.Maxr1998.modernpreferences.helpers.pref
import de.Maxr1998.modernpreferences.helpers.singleChoice
import de.Maxr1998.modernpreferences.helpers.switch
import de.Maxr1998.modernpreferences.preferences.choice.SelectionItem

class GeneralSettings(private val activity: FragmentActivity) : SettingsProvider {

  override fun provideSettings(builder: PreferenceScreen.Builder) {
    builder.apply {
      val themeValues = activity.resources.getStringArray(R.array.app_theme_values)
      val themeOptions = activity.resources.getStringArray(R.array.app_theme_options)
      val themeItems =
        themeValues.zip(themeOptions).map { SelectionItem(it.first, it.second, null) }
      singleChoice(PreferenceKeys.APP_THEME, themeItems) {
        initialSelection = activity.resources.getString(R.string.app_theme_def)
        titleRes = R.string.pref_app_theme_title
      }

      val sortValues = activity.resources.getStringArray(R.array.sort_order_values)
      val sortOptions = activity.resources.getStringArray(R.array.sort_order_entries)
      val sortItems = sortValues.zip(sortOptions).map { SelectionItem(it.first, it.second, null) }
      singleChoice(PreferenceKeys.SORT_ORDER, sortItems) {
        initialSelection = sortValues[0]
        titleRes = R.string.pref_sort_order_title
      }

      switch(PreferenceKeys.DISABLE_SYNC_ACTION) {
        titleRes = R.string.pref_disable_sync_on_pull_title
        summaryRes = R.string.pref_disable_sync_on_pull_summary
        defaultValue = false
      }

      switch(PreferenceKeys.FILTER_RECURSIVELY) {
        titleRes = R.string.pref_recursive_filter_title
        summaryRes = R.string.pref_recursive_filter_summary
        defaultValue = true
      }

      switch(PreferenceKeys.SEARCH_ON_START) {
        titleRes = R.string.pref_search_on_start_title
        summaryRes = R.string.pref_search_on_start_summary
        defaultValue = false
      }

      switch(PreferenceKeys.SHOW_HIDDEN_CONTENTS) {
        titleRes = R.string.pref_show_hidden_title
        summaryRes = R.string.pref_show_hidden_summary
        defaultValue = false
      }

      // val canAuthenticate = BiometricAuthenticator.canAuthenticate(activity)
      switch(PreferenceKeys.BIOMETRIC_AUTH_2) {
        titleRes = R.string.pref_biometric_auth_title
        defaultValue = false
        enabled = false
        // summaryRes =
        //   if (canAuthenticate) R.string.pref_biometric_auth_summary
        //   else R.string.pref_biometric_auth_summary_error
        summary = "Temporarily disabled due to a bug, see issue 2802"
        onClick {
          enabled = false
          val isChecked = checked
          activity.sharedPrefs.edit {
            BiometricAuthenticator.authenticate(activity) { result ->
              when (result) {
                is Result.Success -> {
                  // Apply the changes
                  putBoolean(PreferenceKeys.BIOMETRIC_AUTH_2, checked)
                  enabled = true
                }
                is Result.Retry -> {}
                else -> {
                  // If any error occurs, revert back to the previous
                  // state. This
                  // catch-all clause includes the cancellation case.
                  putBoolean(PreferenceKeys.BIOMETRIC_AUTH_2, !checked)
                  checked = !isChecked
                  enabled = true
                }
              }
            }
          }
          activity.getSystemService<ShortcutManager>()?.apply {
            removeDynamicShortcuts(dynamicShortcuts.map { it.id }.toMutableList())
          }
          false
        }
      }

      pref(PreferenceKeys.DISABLE_BATTERY_OPTIMIZATION) {
        titleRes = R.string.pref_disable_battery_optimization_title
        onClick {
          val powerManager: PowerManager? = ContextCompat.getSystemService(activity.applicationContext, PowerManager::class.java)
          val packageName: String = activity.applicationContext.packageName
          if (powerManager != null) {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
              try {
                @SuppressLint("BatteryLife")
                val intent = Intent().apply {
                  action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                  data = "package:$packageName".toUri()
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(intent)
              } catch (e: ActivityNotFoundException) {
                Toast.makeText(activity, "Battery optimization settings not found", Toast.LENGTH_SHORT).show()
              }
            } else {
              Toast.makeText(activity, "Battery optimization already disabled", Toast.LENGTH_SHORT).show()
            }
          }
          true
        }
      }
    }
  }
}