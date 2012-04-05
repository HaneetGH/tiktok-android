//-----------------------------------------------------------------------------
// MerchantActivity
//-----------------------------------------------------------------------------

package com.tiktok.consumerapp;

//-----------------------------------------------------------------------------
// imports
//-----------------------------------------------------------------------------

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;

import com.tiktok.consumerapp.drawable.BitmapDrawable;
import com.tiktok.consumerapp.utilities.ColorUtilities;
import com.tiktok.consumerapp.utilities.UIUtilities;

//-----------------------------------------------------------------------------
// class implementation
//-----------------------------------------------------------------------------

public class MerchantActivity extends Activity
{
    //-------------------------------------------------------------------------
    // statics
    //-------------------------------------------------------------------------

    private static final String kLogTag = "MerchantActivity";

    //-------------------------------------------------------------------------
    // activity
    //-------------------------------------------------------------------------

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.merchant);

        // grab merchant id from intent
        Long id = (savedInstanceState == null) ? null :
            (Long)savedInstanceState.getSerializable(MerchantTable.sKeyId);
        if (id == null) {
            Bundle extras = getIntent().getExtras();
            id = extras != null ? extras.getLong(MerchantTable.sKeyId) : null;
        }

        // can't be here without a valid coupon id
        if (id == null) {
            finish();
        }

        // retrieve the coupon from the database
        TikTokDatabaseAdapter adapter = new TikTokDatabaseAdapter(this);
        adapter.open();

        // grab coupon using id
        mMerchant = adapter.fetchMerchant(id);
        setupMerchantDetails(mMerchant);

        // close
        adapter.close();
    }

    //-------------------------------------------------------------------------

    /**
     * The activity is about to become visible.
     */
    @Override
    protected void onStart()
    {
        super.onStart();
    }

    //-------------------------------------------------------------------------

    /**
     * The activity has become visible (it is now "resumed").
     */
    @Override
    protected void onResume()
    {
        super.onResume();
    }

    //-------------------------------------------------------------------------

    /**
     * Another activity is taking focus (this activity is about to be "paused")
     */
    @Override
    protected void onPause()
    {
        super.onPause();
    }

    //-------------------------------------------------------------------------

    /**
     * The activity is no longer visible (it is now "stopped")
     */
    @Override
    protected void onStop()
    {
        super.onStop();
    }

    //-------------------------------------------------------------------------

    /**
     * The activity is about to be destroyed.
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (mIconManager != null) mIconManager.clearAllRequests();
    }

    //-------------------------------------------------------------------------
    // helper functions
    //-------------------------------------------------------------------------

    public void setupMerchantDetails(final Merchant merchant)
    {
        // name
        TextView name = (TextView)findViewById(R.id.name);
        name.setText(merchant.name().toUpperCase());

        // address
        setupAddress(merchant);

        // phone
        TextView phone = (TextView)findViewById(R.id.phone);
        phone.setText(merchant.phone());

        // url
        TextView url = (TextView)findViewById(R.id.url);
        url.setText(merchant.websiteUrl().replace("http://", ""));

        // details
        TextView details = (TextView)findViewById(R.id.details);
        details.setText(merchant.details());

        // gradient
        LinearLayout linearLayout   = (LinearLayout)findViewById(R.id.gradient);
        GradientDrawable background = (GradientDrawable)linearLayout.getBackground();
        background.setColor(ColorUtilities.kTik);

        // icon
        setupIcon(merchant);
    }

    //-------------------------------------------------------------------------

    private void setupAddress(final Merchant merchant)
    {
        TextView address    = (TextView)findViewById(R.id.address);
        String splitAddress = TextUtils.join("\n", merchant.address().split(", ", 2));
        address.setText(splitAddress);

        // [moiz] fucking canadain addresses aren't recognized?!
        if (address.getUrls().length == 0) {

            // reset the link mask so we can use custom links
            address.setAutoLinkMask(0);

            // transform the address to be url compliant
            TransformFilter filter = new TransformFilter() {
                public String transformUrl(Matcher match, String url) {
                    String formatted = url.replace("\n", ",+")
                                          .replace(", ", ",+")
                                          .replace( " ",  "+");
                    return formatted;
                }
            };

            // linkify the address
            String scheme   = "geo:0,0?q=";
            Pattern pattern = Pattern.compile("(.+)", Pattern.DOTALL);
            Linkify.addLinks(address, pattern, scheme, null, filter);
        }
    }

    //-------------------------------------------------------------------------

    private void setupIcon(final Merchant merchant)
    {
        final ImageView iconView            = (ImageView)findViewById(R.id.icon);
        final IconManager.IconData iconData = merchant.iconData();

        // setup the icon manager
        if (mIconManager == null) {
            mIconManager = new IconManager(this);
        }

        // use cached icon if available
        BitmapDrawable icon = mIconManager.getImage(iconData);
        if (icon != null) {
            iconView.setImageBitmap(icon.getBitmap());

        // use activity indicator and load image from server
        } else {

            // set activity indicator
            iconView.setImageResource(R.drawable.activity_indicator);
            iconView.startAnimation(UIUtilities.getActivityIndicatorAnimation());

            // download icon from server
            mIconManager.requestImage(iconData, new IconManager.CompletionHandler() {

                public void onSuccess(final BitmapDrawable drawable) {
                    Log.i(kLogTag, String.format("Downloaded icon: %s", iconData.url));
                    iconView.post(new Runnable() {
                        public void run() {
                            iconView.setImageBitmap(drawable.getBitmap());
                            iconView.clearAnimation();
                        }
                    });
                }

                public void onFailure() {
                    Log.e(kLogTag, String.format("Failed to download icon: %s", iconData.url));
                }

            });
        }
    }

    //-------------------------------------------------------------------------
    // fields
    //-------------------------------------------------------------------------

    private Merchant    mMerchant;
    private IconManager mIconManager;
}
