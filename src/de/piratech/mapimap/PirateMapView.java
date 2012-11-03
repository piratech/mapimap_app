package de.piratech.mapimap;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import de.piratech.mapimap.R;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class PirateMapView extends MapActivity {

	private MapView mMapView;

	public class HelloItemizedOverlay extends ItemizedOverlay<OverlayItem> {
		private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
		private Context mContext;

		public HelloItemizedOverlay(Drawable defaultMarker, Context context) {
			super(boundCenterBottom(defaultMarker));
			mContext = context;
		}

		public void addOverlay(OverlayItem overlay) {
			mOverlays.add(overlay);
			populate();
		}

		@Override
		protected OverlayItem createItem(int i) {
			return mOverlays.get(i);
		}

		@Override
		public int size() {
			return mOverlays.size();
		}

		@Override
		protected boolean onTap(int index) {
			final OverlayItem item = mOverlays.get(index);

			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			// builder.setTitle("Pick a color");

			Log.v("Trace", "Search Items");
			ArrayList<OverlayItem> list = new ArrayList<OverlayItem>();
			for (int i = 0; i < mOverlays.size(); i++) {
				if ((mOverlays.get(i).getPoint().getLatitudeE6() == item
						.getPoint().getLatitudeE6())
						&& (mOverlays.get(i).getPoint().getLongitudeE6() == item
								.getPoint().getLongitudeE6())) {
					list.add(mOverlays.get(i));
				}

			}

			CharSequence[] items = new CharSequence[list.size()];
			final String[] urls  = new String[list.size()];

			Log.v("Trace", "Create Item List");
			for (int i = 0; i < list.size(); i++) {
				items[i] = list.get(i).getTitle();
				urls[i]  = list.get(i).getSnippet();
			}
			
			
			Log.v("Trace", "Build List");
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogInterface, int cnt) {
					Log.v("Trace", "Open URL");
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urls[cnt]));
					startActivity(browserIntent);
					return;
				}
			});
			builder.create().show();
			
			Log.v("Trace", "Finish");
			return true;
		}
	}

	private static JSONArray getJSONfromURL(String url) {

		// initialize
		InputStream is = null;
		StringBuilder sb = new StringBuilder();
		JSONArray jArray = null;

		// http post
		try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httppost = new HttpGet(url);
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			is = entity.getContent();

		} catch (Exception e) {
			Log.e("getJSONfromURL", "Error in http connection " + e.toString());
		}

		// convert response to string
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "UTF-8"), 8);

			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			is.close();
		} catch (Exception e) {
			Log.e("getJSONfromURL", "Error converting result " + e.toString());
		}

		// try parse the string to a JSON object
		try {
			jArray = new JSONArray(sb.toString());
		} catch (JSONException e) {
			Log.e("getJSONfromURL", "Error parsing data " + e.toString());
		}

		return jArray;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Location location = ((LocationManager) this.getSystemService(Context.LOCATION_SERVICE)).getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		Log.v("Trace", "Create Map");
		mMapView = new MapView(this, "06cfB-6Xolo4eJAwPrcvcwPBZJr3kyDCUJmOCqw");
		mMapView.getController().setCenter( new GeoPoint((int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6)));
		mMapView.getController().setZoom(12);
		mMapView.setClickable(true);
		mMapView.setEnabled(true);
		setContentView(mMapView);

		Log.v("Trace", "Load Data");
		JSONArray liste = getJSONfromURL("http://mapimap.piratech.de/android.php?lat="+String.valueOf(location.getLatitude())+"&lon="+String.valueOf(location.getLongitude()));

		Log.v("Trace", "Process Data");
		HelloItemizedOverlay ItemizedOverlay = new HelloItemizedOverlay(this.getResources().getDrawable(R.drawable.marker_gold), this);
		if (liste == null) {
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle("Error");
			dialog.setMessage("Fehler beim Laden der Standorte");
			dialog.show();
		} else {
			for (int i = 0; i < liste.length(); i++) {
				try {
					OverlayItem item = new OverlayItem(new GeoPoint(
							(int) (liste.getJSONObject(i).getDouble("lat") * 1E6),
							(int) (liste.getJSONObject(i).getDouble("lon") * 1E6)),
							liste.getJSONObject(i).getString("type") + ": " + liste.getJSONObject(i).getString("name"),
							liste.getJSONObject(i).getString("url"));
					ItemizedOverlay.addOverlay(item);

				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
		Log.v("Trace", "Overlay Map");
		mMapView.getOverlays().add(ItemizedOverlay);
		Log.v("Trace", "Created");

	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 1, 0, "Mehr Infos");
		menu.add(0, 2, 0, "Exit");
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Uri uri = null;
		Intent intent = null;
		switch (item.getItemId()) {
		case 1:
			uri = Uri.parse("http://wiki.piratenpartei.de/BE:Squads/Piratech/Projekte/Mapimap");
			intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
			return true;
		case 2:
			this.finish();
			return true;
		}
		return false;
	}


	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}