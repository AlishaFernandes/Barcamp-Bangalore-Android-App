/*
 * Copyright (C) 2012 Saurabh Minni <http://100rabh.com>
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

package com.bangalore.barcamp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.net.Uri;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.bangalore.barcamp.activity.AboutActivity;
import com.bangalore.barcamp.activity.BCBActivityBaseClass;
import com.bangalore.barcamp.activity.InternalVenueMapActivity;
import com.bangalore.barcamp.activity.ScheduleActivity;
import com.bangalore.barcamp.activity.SettingsActivity;
import com.bangalore.barcamp.activity.ShareActivity;
import com.bangalore.barcamp.activity.UpdateMessagesActivity;
import com.bangalore.barcamp.activity.WebViewActivity;
import com.bangalore.barcamp.data.BarcampBangalore;
import com.bangalore.barcamp.data.BarcampData;
import com.bangalore.barcamp.data.BarcampUserScheduleData;
import com.bangalore.barcamp.data.Session;
import com.bangalore.barcamp.data.Slot;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
import com.markupartist.android.widget.ActionBar.IntentAction;
import com.slidingmenu.lib.SlidingMenuActivity;

public class BCBUtils {

	private static final int MAX_LOG = 2000;
	private static final String BARCAMP_SCHEDULE_JSON = "http://barcampbangalore.org/schadmin/android.json";
	private static final String BCB_LOCATION_MAPS_URL = "https://www.google.co.in/maps?t=m&cid=0x9ed33f443055ef5f&z=17&iwloc=A";
	protected static final int START_SCHEDULE = 100;
	protected static final int START_ABOUT = 101;
	protected static final int START_SETTINGS = 102;
	protected static final int START_SHARE = 103;
	protected static final int START_BCB12_TWEETS = 104;
	protected static final int START_BCB_UPDATES = 105;
	private static final String BCB_USER_SCHEDULE_URL = "http://barcampbangalore.org/bcb/wp-android_helper.php?action=getuserdata&userid=%s&userkey=%s";
	protected static final int START_INTERNAL_VENUE = 106;

	public static void createActionBarOnActivity(final Activity activity) {
		createActionBarOnActivity(activity, false);
	}

	public static void createActionBarOnActivity(final Activity activity,
			boolean isHome) {
		// ******** Start of Action Bar configuration
		ActionBar actionbar = (ActionBar) activity
				.findViewById(R.id.actionBar1);
		actionbar.setHomeLogo(R.drawable.home);
		actionbar.setHomeAction(new Action() {
			@Override
			public void performAction(View view) {
				((SlidingMenuActivity) activity).toggle();
			}

			@Override
			public int getDrawable() {
				return R.drawable.home;
			}
		});

		actionbar.setTitle(R.string.app_title_text);
		TextView logo = (TextView) activity.findViewById(R.id.actionbar_title);
		Shader textShader = new LinearGradient(0, 0, 0, logo.getHeight(),
				new int[] { Color.WHITE, 0xff999999 }, null, TileMode.CLAMP);
		logo.getPaint().setShader(textShader);
		actionbar.setOnTitleClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

			}
		});
		// ******** End of Action Bar configuration

	}

	public static Action createShareAction(Activity activity) {
		IntentAction shareAction = new IntentAction(activity,
				createShareIntent(activity), R.drawable.share_icon);

		return shareAction;
	}

	public static Intent createShareIntent(Activity activity) {
		Intent intent = new Intent(activity, ShareActivity.class);
		return intent;

	}

	public static PendingIntent createPendingIntentForID(Context context,
			String id, int slot, int session) {
		Intent intent = new Intent(context, SessionAlarmIntentService.class);
		intent.putExtra(SessionAlarmIntentService.SESSION_ID, id);
		intent.putExtra(SessionAlarmIntentService.EXTRA_SLOT_POS, slot);
		intent.putExtra(SessionAlarmIntentService.EXTRA_SESSION_POSITION,
				session);
		int idInt = Integer.parseInt(id);
		PendingIntent pendingIntent = PendingIntent.getService(context, idInt,
				intent, PendingIntent.FLAG_ONE_SHOT);
		return pendingIntent;
	}

	public static Boolean updateContextWithBarcampData(Context context) {
		Boolean retVal = false;
		BufferedReader in = null;
		try {
			HttpClient client = new DefaultHttpClient();
			HttpUriRequest request = new HttpGet(BARCAMP_SCHEDULE_JSON);
			HttpResponse response = client.execute(request);
			in = new BufferedReader(new InputStreamReader(response.getEntity()
					.getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();
			String page = sb.toString();

			if (page.length() > MAX_LOG) {
				int iCount = 0;
				for (; iCount < page.length() - MAX_LOG; iCount += MAX_LOG) {
					Log.e("bcbdata", page.substring(iCount, iCount + MAX_LOG));
				}
				Log.e("bcbdata", page.substring(iCount));
			} else {
				Log.e("bcbdata", page);
			}

			BarcampData data = DataProcessingUtils.parseBCBJSON(page);
			if (data != null) {
				((BarcampBangalore) context).setBarcampData(data);
				retVal = true;
				BCBSharedPrefUtils.setAllBCBUpdates(context, page);
			}

		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (!retVal) {
			updateContextDataFromSharedPreferences(context);
		}
		return retVal;
	}

	public static void updateContextDataFromSharedPreferences(Context context) {
		try {
			String page = BCBSharedPrefUtils.getAllBCBUpdates(context, null);
			if (page != null) {
				BarcampData data;
				data = DataProcessingUtils.parseBCBJSON(page);
				((BarcampBangalore) context).setBarcampData(data);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static void addNavigationActions(
			final BCBActivityBaseClass homeActivity) {
		homeActivity.setBehindContentView(R.layout.navigation_menu);
		int offset = 100;
		DisplayMetrics metrics = new DisplayMetrics();
		homeActivity.getWindow().getWindowManager().getDefaultDisplay()
				.getMetrics(metrics);
		offset = ((metrics.widthPixels * 130)) / 480;

		homeActivity.setBehindOffset(offset);
		homeActivity.setBehindScrollScale(0.5f);

		View view = homeActivity.findViewById(R.id.nav_agenda);
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(homeActivity, ScheduleActivity.class);
				homeActivity.startActivityForResult(intent, START_SCHEDULE);
			}
		});

		view = homeActivity.findViewById(R.id.nav_about);
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(homeActivity, AboutActivity.class);
				homeActivity.startActivityForResult(intent, START_ABOUT);
			}
		});

		view = homeActivity.findViewById(R.id.nav_internal_venue_map);
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(homeActivity,
						InternalVenueMapActivity.class);
				homeActivity.startActivityForResult(intent,
						START_INTERNAL_VENUE);
			}
		});

		view = homeActivity.findViewById(R.id.nav_settings);
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(homeActivity, SettingsActivity.class);
				homeActivity.startActivityForResult(intent, START_SETTINGS);
			}
		});
		view.setVisibility(View.GONE);

		view = homeActivity.findViewById(R.id.nav_share);
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(homeActivity, ShareActivity.class);
				homeActivity.startActivityForResult(intent, START_SHARE);
			}
		});

		view = homeActivity.findViewById(R.id.nav_tweets);
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(homeActivity, WebViewActivity.class);
				intent.putExtra(WebViewActivity.URL,
						"file:///android_asset/bcb11_updates.html");
				homeActivity.startActivityForResult(intent, START_BCB12_TWEETS);
			}
		});

		view = homeActivity.findViewById(R.id.nav_update);
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(homeActivity,
						UpdateMessagesActivity.class);
				homeActivity.startActivityForResult(intent, START_BCB_UPDATES);
			}
		});

		view = homeActivity.findViewById(R.id.nav_venue);
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final PackageManager pm = homeActivity.getPackageManager();

				Intent intent = new Intent(Intent.ACTION_VIEW, Uri
						.parse(BCB_LOCATION_MAPS_URL));
				final List<ResolveInfo> matches = pm.queryIntentActivities(
						intent, 0);
				for (ResolveInfo info : matches) {
					Log.e("MapPackage", info.loadLabel(pm) + " "
							+ info.activityInfo.packageName + " "
							+ info.activityInfo.name);
					if (info.activityInfo.name
							.equals("com.google.android.maps.MapsActivity")) {
						intent.setClassName("com.google.android.apps.maps",
								"com.google.android.maps.MapsActivity");
					}
				}

				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				homeActivity.startActivity(intent);
			}
		});

		view = homeActivity.findViewById(R.id.nav_BCB);
		view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://barcampbangalore.org"));
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				homeActivity.startActivity(intent);
			}
		});

	}

	public static void syncUserScheduleData(Context context) {
		String userID = BCBSharedPrefUtils.getUserID(context);
		String userKey = BCBSharedPrefUtils.getUserKey(context);
		if (TextUtils.isEmpty(userKey) || TextUtils.isEmpty(userID)) {
			return;
		}

		BufferedReader in = null;
		try {
			HttpClient client = new DefaultHttpClient();
			String userScheduleURL = String.format(BCB_USER_SCHEDULE_URL,
					userID, userKey);
			Log.e("UserURL", userScheduleURL);
			HttpUriRequest request = new HttpGet(userScheduleURL);
			HttpResponse response = client.execute(request);
			in = new BufferedReader(new InputStreamReader(response.getEntity()
					.getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();
			String page = sb.toString();
			if (page.length() > MAX_LOG) {
				int iCount = 0;
				for (; iCount < page.length() - MAX_LOG; iCount += MAX_LOG) {
					Log.e("schedule", page.substring(iCount, iCount + MAX_LOG));
				}
				Log.e("schedule", page.substring(iCount));
			} else {
				Log.e("schedule", page);
			}
			List<BarcampUserScheduleData> data = DataProcessingUtils
					.parseBCBScheduleJSON(page);
			((BarcampBangalore) context).setUserSchedule(data);

		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void removeSessionFromSchedule(Context context,
			String sessionid, int slotPos, int sessionPos) {
		BCBSharedPrefUtils.setAlarmSettingsForID(context, sessionid,
				BCBSharedPrefUtils.ALARM_NOT_SET);
		PendingIntent intent = BCBUtils.createPendingIntentForID(context,
				sessionid, slotPos, sessionPos);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(intent);
	}

	public static void setAlarmForSession(Context context, Slot slot,
			Session session, int slotpos, int sessionpos) {
		BCBSharedPrefUtils.setAlarmSettingsForID(context, session.id,
				BCBSharedPrefUtils.ALARM_SET);
		PendingIntent intent = BCBUtils.createPendingIntentForID(context,
				session.id, slotpos, sessionpos);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		int hour = slot.startTime / 100;
		int mins = slot.startTime % 100;
		Log.e("Session", "hour : " + hour + " mins :" + mins);
		GregorianCalendar date = new GregorianCalendar(2013,
				Calendar.SEPTEMBER, 14, hour, mins);
		long timeInMills = date.getTimeInMillis() - 300000;
		alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMills, intent);
	}
}
